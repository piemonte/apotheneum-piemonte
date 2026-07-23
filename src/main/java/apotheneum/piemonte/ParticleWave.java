/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * ParticleWave — a vast rolling swell of shimmering grains sweeps around the
 * structure as a thick undulating band: a sea of glitter surging through a dark
 * starfield. The band's edges dissolve into loose grains, a foamy white crest
 * sparkles along the boundary, and sparse cool stars twinkle in the void.
 * Turning up Wow whips the sea into a storm: taller swells, faster roll, hotter
 * foam, and bursts of blue sparkle clusters in the dark.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class ParticleWave extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final int STAR_COLOR = LXColor.hsb(225, 35, 100);
  private static final int CLUSTER_COLOR = LXColor.lerp(STAR_COLOR, LXColor.WHITE, 0.45f);
  private static final double STAR_DENSITY = 0.016;
  private static final int MAX_CLUSTERS = 16;
  private static final int CLUSTER_GLINTS = 16;
  private static final double TAU = Math.PI * 2;

  public final CompoundParameter density =
    new CompoundParameter("Density", 0.65, 0.2, 1)
    .setDescription("Grain density of the particle sea");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  private final class Surface {
    // sparkle-cluster pool (storm surge bursts in the void)
    final double[] cx = new double[MAX_CLUSTERS];
    final double[] cy = new double[MAX_CLUSTERS];
    final double[] clife = new double[MAX_CLUSTERS];
    final double[] cmax = new double[MAX_CLUSTERS];
    final int[] cseed = new int[MAX_CLUSTERS];
    final boolean[] calive = new boolean[MAX_CLUSTERS];

    void spawnCluster(int w, int h) {
      for (int i = 0; i < MAX_CLUSTERS; ++i) {
        if (!this.calive[i]) {
          this.cx[i] = Math.random() * w;
          this.cy[i] = Math.random() * h;
          this.cmax[i] = 700 + Math.random() * 700;
          this.clife[i] = this.cmax[i];
          this.cseed[i] = (int) (Math.random() * 100000);
          this.calive[i] = true;
          return;
        }
      }
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();
  private double timeMs = 0;
  private double phase = 0;

  public ParticleWave(LX lx) {
    // Base registers color (grain tint), speed (roll rate), size (band thickness).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("density", this.density);
    addParameter("target", this.target);
  }

  private static double hash(int x, int y, int t) {
    int h = x * 374761393 + y * 668265263 + t * 1274126177;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    this.timeMs += deltaMs;

    final double speed = Math.max(0.02, getSpeed());
    final double wow = getWow();
    // the swell rolls around the structure; storms roll faster
    this.phase += deltaMs * 0.00006 * speed * (1.0 + wow * 0.9);

    final Target t = this.target.getEnum();
    if (t != Target.CYLINDER) {
      step(this.cube, Apotheneum.cube.exterior, deltaMs, speed, wow);
    }
    if (t != Target.CUBE) {
      step(this.cylinder, Apotheneum.cylinder.exterior, deltaMs, speed, wow);
    }
    copyExterior();
  }

  private void step(Surface s, Apotheneum.Orientation o, double deltaMs, double speed, double wow) {
    final int w = o.width();
    final int h = o.height();

    final int base = getColor();
    final int glint = LXColor.lerp(base, LXColor.WHITE, 0.65f);
    final int foam = LXColor.lerp(base, LXColor.WHITE, 0.85f);
    final double dens = this.density.getValue();

    // storm surge: taller swell, thicker foam
    final double amp = h * 0.18 * (1.0 + wow * 1.2);
    final double thick = h * (0.12 + getSize() * 0.28);
    final double foamW = 1.6 + wow * 1.8;
    final double drift = 0.16 * Math.sin(this.timeMs * 2.0e-5 * (0.5 + speed));
    final double ph = this.phase;

    // shimmer quanta: grains flicker fast, stars twinkle slow
    final int tq = (int) (this.timeMs / 50);
    final int tqs = (int) (this.timeMs / 400);

    for (int x = 0; x < w; ++x) {
      final double u = (double) x / w;
      // three traveling sine octaves shape the swell
      final double yc = h * (0.5 + drift) + amp * (
          0.60 * Math.sin(TAU * (u * 2 - ph))
        + 0.25 * Math.sin(TAU * (u * 5 + ph * 1.7))
        + 0.15 * Math.sin(TAU * (u * 9 - ph * 2.3)));
      final double th = thick * (0.75 + 0.25 * Math.sin(TAU * (u * 3 + ph * 0.8)));

      for (int y = 0; y < h; ++y) {
        final double dRaw = Math.abs(y - yc) - th; // signed distance outside the band
        // granular dissolving edge: jitter the boundary per-pixel
        final double d = dRaw + (hash(x, y, tq >> 2) - 0.5) * 3.0;

        int c;
        double b;
        if (d < 0) {
          // inside the sea: hashed glitter shimmer
          final double g = hash(x, y, tq);
          if (g > dens) continue; // unlit grain this instant
          final double v = g / dens;
          b = 0.22 + 0.78 * v * v * v;
          c = (v > 0.965) ? LXColor.WHITE : ((v > 0.8) ? glint : base);
          // feather deep pixels slightly less than the fringe
          final double edge = LXUtils.clamp(-d / 3.0, 0, 1);
          b *= 0.55 + 0.45 * edge;
        } else {
          // the void: sparse, spatially-stable stars with a slow twinkle
          final double rs = hash(x, y, 0);
          if (rs >= STAR_DENSITY) continue;
          final double tw = 0.35 + 0.65 * hash(x, y, tqs + 7919);
          b = tw * (0.30 + 0.70 * (rs / STAR_DENSITY));
          c = STAR_COLOR;
        }

        // foam crest: white-hot sparkle hugging the boundary
        if (Math.abs(dRaw) < foamW) {
          final double ff = 1.0 - Math.abs(dRaw) / foamW;
          final double fg = hash(x, y, tq + 4241);
          if (fg < 0.5 + wow * 0.3) {
            b = Math.max(b, ff * (0.7 + wow * 0.8) * (0.5 + 0.5 * fg / 0.8));
            c = foam;
          }
        }

        if (b <= 0) continue;
        final int idx = o.point(x, y).index;
        this.colors[idx] = LXColor.scaleBrightness(c, (float) LXUtils.clamp(b, 0, 1));
      }
    }

    // --- storm sparkle clusters bursting in the void ---
    if (Math.random() < deltaMs * (0.0002 + wow * 0.005)) {
      s.spawnCluster(w, h);
    }
    for (int i = 0; i < MAX_CLUSTERS; ++i) {
      if (!s.calive[i]) continue;
      s.clife[i] -= deltaMs;
      if (s.clife[i] <= 0) { s.calive[i] = false; continue; }
      final double fade = s.clife[i] / s.cmax[i];
      final double spread = 2.0 + (1.0 - fade) * 5.0; // glints scatter outward as it dies
      for (int k = 0; k < CLUSTER_GLINTS; ++k) {
        final double gx = s.cx[i] + (hash(s.cseed[i], k, 11) - 0.5) * 2 * spread;
        final double gy = s.cy[i] + (hash(s.cseed[i], k, 37) - 0.5) * 2 * spread;
        final int yy = (int) Math.round(gy);
        if (yy < 0 || yy >= h) continue;
        final int xi = (((int) Math.round(gx)) % w + w) % w;
        final double twk = 0.4 + 0.6 * hash(s.cseed[i] + k, tq, 53);
        final double gb = fade * fade * twk;
        if (gb <= 0.02) continue;
        final int idx = o.point(xi, yy).index;
        this.colors[idx] = LXColor.lightest(this.colors[idx],
          LXColor.scaleBrightness(CLUSTER_COLOR, (float) LXUtils.clamp(gb, 0, 1)));
      }
    }
  }
}
