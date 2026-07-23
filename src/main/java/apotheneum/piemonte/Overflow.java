/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Overflow — the strand curtains run like a vessel of glowing green fluid:
 * energy fills the columns from the floor up, its surface undulating around the
 * ring, with a hot red band riding the waterline and red drips tearing off to
 * fall and pool below. The fill breathes with the music — rising on the swell,
 * draining in the quiet — and a drop flashes the whole room green. Modeled on
 * the holotrigger green/red cascade program.
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
public class Overflow extends StrandPattern {

  private static final int MAX_DRIPS = 80;
  private static final double GREEN_HUE = 120;
  private static final double RED_HUE = 8;
  private static final double GRAV = 0.00012;

  public final DiscreteParameter drips =
    new DiscreteParameter("Drips", 24, 0, MAX_DRIPS)
    .setDescription("How many red drips can fall at once");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of the fill and flash");

  private final class Surface {
    boolean inited;
    final double[] dx = new double[MAX_DRIPS];
    final double[] dy = new double[MAX_DRIPS];
    final double[] dv = new double[MAX_DRIPS];
    final boolean[] alive = new boolean[MAX_DRIPS];
    void reset() {
      for (int i = 0; i < MAX_DRIPS; ++i) this.alive[i] = false;
      this.inited = true;
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();
  private double flashEnv = 0;
  private double lfo = 0;

  public Overflow(LX lx) {
    // Base registers color (hue shift), speed (breathe tempo), size (red band width).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("drips", this.drips);
    addParameter("sensitivity", this.sensitivity);
    addTargetParameter();
  }

  @Override
  protected double beatThreshold() {
    return 1.05 + (1 - this.sensitivity.getValue()) * 0.7;
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  /** Smooth wrapped 1D noise around the ring: sum of two sines with irrational ratio. */
  private double surfNoise(double x01, double t) {
    return 0.5
      + 0.30 * Math.sin(2 * Math.PI * (x01 * 2 + t * 0.11))
      + 0.20 * Math.sin(2 * Math.PI * (x01 * 5 - t * 0.07));
  }

  @Override
  protected void advance(double deltaMs) {
    final double speed = Math.max(0.02, getSpeed());
    this.lfo += deltaMs * 0.0002 * speed;
    if (this.beat && (this.beatLevel > 0.5 || this.levelEnv > 0.55)) {
      this.flashEnv = 1; // the drop: whole room goes green
    }
    this.flashEnv *= Math.exp(-deltaMs / 380.0);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    if (!s.inited) s.reset();

    final int w = o.width();
    final int h = o.height();
    final double wow = getWow();
    final double hueShift = LXColor.h(getColor());
    final int green = LXColor.hsb((float) (((GREEN_HUE + hueShift) % 360)), 95, 100);
    final int red = LXColor.hsb((float) (((RED_HUE + hueShift) % 360)), 100, 100);
    final int greenHot = LXColor.lerp(green, LXColor.WHITE, 0.4f);

    // fill level: audio envelope with a slow breathing fallback, Wow surges it
    final double env = LXUtils.clamp(
      Math.max(this.levelEnv * (1.2 + wow * 0.5), 0.30 + 0.25 * Math.sin(2 * Math.PI * this.lfo))
      + this.beatLevel * 0.15, 0, 1);
    // the red band rides the waterline, but surges to flood whole columns on hits
    final double bandW = (1.5 + getSize() * 3.0) + this.beatLevel * h * 0.35;
    final int tq = (int) (this.timeMs / 90); // shimmer quantum

    for (int x = 0; x < w; ++x) {
      final double x01 = (double) x / w;
      final double surfY = (h - 1) * (1.0 - env * surfNoise(x01, this.timeMs * 0.001));
      final double shim = 0.90 + 0.10 * hashd(x * 131 + tq * 7);

      for (int y = 0; y < h; ++y) {
        double b;
        int c;
        if (y >= surfY) {
          // green fluid below the waterline
          b = (0.55 + 0.45 * drip(y - (int) surfY, h)) * shim;
          b += pool(y, h);
          c = (y - surfY < 1.2) ? greenHot : green;
        } else if (surfY - y <= bandW) {
          // hot red band riding the surface
          double u = 1.0 - (surfY - y) / bandW;
          b = (0.5 + 0.5 * u * u) * shim;
          c = red;
        } else {
          continue;
        }
        addPix(o, x, y, c, b);
      }
    }

    // red drips tearing off the surface
    final int maxDrips = this.drips.getValuei();
    int alive = 0;
    for (int i = 0; i < MAX_DRIPS; ++i) if (s.alive[i]) ++alive;
    double spawnP = deltaMs * (0.004 + this.beatLevel * 0.03 + wow * 0.01);
    for (int i = 0; i < MAX_DRIPS && alive < maxDrips; ++i) {
      if (!s.alive[i] && Math.random() < spawnP) {
        s.dx[i] = Math.random() * w;
        double x01 = s.dx[i] / w;
        s.dy[i] = (h - 1) * (1.0 - env * surfNoise(x01, this.timeMs * 0.001));
        s.dv[i] = 0.008 + Math.random() * 0.012;
        s.alive[i] = true;
        ++alive;
      }
    }
    for (int i = 0; i < MAX_DRIPS; ++i) {
      if (!s.alive[i]) continue;
      s.dv[i] += GRAV * deltaMs;
      s.dy[i] += s.dv[i] * deltaMs;
      if (s.dy[i] >= h - 1) { s.alive[i] = false; continue; }
      comet(o, s.dx[i], s.dy[i], 4, red, 0.9, true);
    }

    // drifting floor-pool tendrils, red and green curls glowing on the floor line
    for (int i = 0; i < 4; ++i) {
      final double dir = (i % 2 == 0) ? 1 : -1;
      final double px = (hashd(i * 97 + 5) * w + dir * this.timeMs * 0.0025) % w;
      final int pc = (i % 2 == 0) ? red : green;
      for (int dx = -4; dx <= 4; ++dx) {
        final double g = Math.exp(-dx * dx / 6.0) * 0.45;
        addPix(o, (int) px + dx, h - 1, pc, g);
        addPix(o, (int) px + dx, h - 2, pc, g * 0.5);
      }
    }

    // the drop: full green flash
    if (this.flashEnv > 0.01) {
      final int fc = LXColor.scaleBrightness(green,
        (float) (this.flashEnv * this.flashEnv * (0.5 + wow * 0.4)));
      for (int x = 0; x < w; ++x) {
        for (int y = 0; y < h; ++y) {
          final int idx = o.point(x, y).index;
          this.colors[idx] = LXColor.lightest(this.colors[idx], fc);
        }
      }
    }
  }
}
