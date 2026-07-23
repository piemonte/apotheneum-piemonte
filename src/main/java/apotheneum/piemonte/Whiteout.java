/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Whiteout — a silver monochrome cascade: dense white rain streams down the
 * strand curtains like backlit falling water, every droplet scintillating as it
 * falls. The curtains hang in discrete panels with dark gaps between them — a
 * barcode of waterfalls — with one lone warm amber block glowing off to the
 * side. Music gently swells the density; Wow whips it into a blizzard.
 * Modeled on the holotrigger white digital-rain program.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class Whiteout extends StrandPattern {

  private static final int MAX_DROPS = 600;
  private static final int SILVER = LXColor.hsb(210, 8, 100); // barely-cool white
  private static final int AMBER = LXColor.hsb(45, 85, 100);
  private static final double GAP_FRAC = 0.42; // wide dark gaps between curtain panels

  public final CompoundParameter density =
    new CompoundParameter("Density", 0.6, 0.1, 1)
    .setDescription("How heavy the rain falls");

  public final DiscreteParameter panels =
    new DiscreteParameter("Panels", 8, 1, 12)
    .setDescription("Curtain panels around the surface (1 = continuous)");

  private final class Surface {
    boolean inited;
    final double[] px = new double[MAX_DROPS];
    final double[] py = new double[MAX_DROPS];
    final double[] pv = new double[MAX_DROPS];
    final double[] pb = new double[MAX_DROPS];
    final boolean[] alive = new boolean[MAX_DROPS];
    void reset() {
      for (int i = 0; i < MAX_DROPS; ++i) this.alive[i] = false;
      this.inited = true;
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();

  public Whiteout(LX lx) {
    // Base registers color (unused tint stays silver), speed (fall rate), size (tail length).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("density", this.density);
    addParameter("panels", this.panels);
    addTargetParameter();
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  /** True if column x lies in a dark gap between curtain panels. */
  private boolean inGap(int x, int w, int nPanels) {
    if (nPanels <= 1) return false;
    double pf = (double) x / w * nPanels;
    return (pf - Math.floor(pf)) > (1.0 - GAP_FRAC);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    if (!s.inited) s.reset();

    final int w = o.width();
    final int h = o.height();
    final double wow = getWow();
    final double speed = Math.max(0.05, getSpeed());
    final int nPanels = this.panels.getValuei();
    final double tail = (3 + getSize() * 3) * (1 + wow * 0.5);
    // density swells gently with the music; blizzard at high Wow
    final double dens = this.density.getValue()
      * (0.55 + 0.45 * LXUtils.clamp(this.levelEnv * 1.5, 0, 1) + wow)
      * (nPanels <= 1 ? 1.0 : 1.0 / (1.0 - GAP_FRAC));

    // base curtain: static full-height strands, all the motion is in-place
    // scintillation (the reference's threads shimmer without falling)
    final int tqc = (int) (this.timeMs / 60);
    for (int x = 0; x < w; ++x) {
      if (inGap(x, w, nPanels)) continue;
      for (int y = 0; y < h; ++y) {
        final double g = hashd(x * 262147 + y * 8191 + tqc * 131);
        if (g > 0.42) continue; // ~40% of strand pixels lit each instant
        final double b = (0.06 + 0.30 * (g / 0.42)) * (0.6 + 0.4 * this.levelEnv + wow * 0.3);
        addPix(o, x, y, SILVER, b * (0.8 + 0.2 * drip(y, h)));
      }
    }

    // spawn accent streaks (sparse, slower than the shimmer suggests motion)
    final double spawnRate = dens * w * 0.0005 * deltaMs;
    int toSpawn = (int) spawnRate + ((Math.random() < spawnRate % 1) ? 1 : 0);
    for (int i = 0; i < MAX_DROPS && toSpawn > 0; ++i) {
      if (s.alive[i]) continue;
      int x = (int) (Math.random() * w);
      if (inGap(x, w, nPanels)) continue;
      s.px[i] = x;
      s.py[i] = -Math.random() * 4;
      s.pv[i] = (0.017 + Math.random() * 0.010) * speed * (1 + wow * 0.6);
      s.pb[i] = 0.35 + 0.65 * Math.random();
      s.alive[i] = true;
      --toSpawn;
    }

    // fall + draw
    final int tq = (int) (this.timeMs / 50);
    for (int i = 0; i < MAX_DROPS; ++i) {
      if (!s.alive[i]) continue;
      s.py[i] += s.pv[i] * deltaMs;
      if (s.py[i] >= h + tail) { s.alive[i] = false; continue; }
      // scintillation: each droplet flickers frame to frame
      double tw = 0.55 + 0.45 * hashd(i * 131 + tq * 7);
      comet(o, s.px[i], s.py[i], tail, SILVER, s.pb[i] * tw, s.pb[i] > 0.8);
    }

    // sparkle mask: brief white glints across the lit panel area
    final double sparkleP = 0.010 + wow * 0.025;
    final int glints = (int) (w * h * sparkleP * (deltaMs / 50.0));
    for (int k = 0; k < glints; ++k) {
      int gx = (int) (Math.random() * w);
      if (inGap(gx, w, nPanels)) continue;
      int gy = (int) (Math.random() * h);
      addPix(o, gx, gy, LXColor.WHITE, 0.25 + Math.random() * 0.5);
    }

    // the lone warm accent block, parked on one panel edge
    final int ax = (int) (w * 0.83);
    for (int dx = 0; dx < 3; ++dx) {
      for (int dy = 0; dy < 6; ++dy) {
        addPix(o, ax + dx, h - 10 + dy, AMBER, 0.16 + 0.05 * hashd(dx * 7 + dy * 13 + tq));
      }
    }
  }
}
