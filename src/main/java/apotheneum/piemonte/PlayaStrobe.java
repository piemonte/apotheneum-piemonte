/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * PlayaStrobe — a bright glint rakes around the structure like light sweeping
 * across churning storm water, its edges torn by turbulent noise, over a faint
 * storm-shimmer floor. The operator holds the trigger: hitting the Strobe
 * button fires a hard white pulsating burst that flashes the entire structure
 * and decays away; holding it sustains the storm. Wow deepens the turbulence,
 * widens the glint, and drives the strobe harder.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class PlayaStrobe extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final double DECAY_MS = 1500; // strobe burst fade-out
  private static final double STROBE_DUTY = 0.45;

  public final BooleanParameter strobe =
    new BooleanParameter("Strobe", false)
    .setMode(BooleanParameter.Mode.MOMENTARY)
    .setDescription("Fire a pulsating white strobe burst; hold to sustain");

  public final CompoundParameter rate =
    new CompoundParameter("Rate", 9, 2, 16)
    .setDescription("Strobe pulse rate in flashes per second");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  private double timeMs = 0;
  private double phase = 0;
  private double strobeEnv = 0;

  public PlayaStrobe(LX lx) {
    // Base registers color (glint tint), speed (sweep rate), size (glint width).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("strobe", this.strobe);
    addParameter("rate", this.rate);
    addParameter("target", this.target);
  }

  private static double frac(double x) {
    return x - Math.floor(x);
  }

  private static double hash(int x, int y, int t) {
    int h = x * 374761393 + y * 668265263 + t * 1274126177;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  /** Smooth value noise in [0,1]. */
  private static double vnoise(double x, double y) {
    int xi = (int) Math.floor(x), yi = (int) Math.floor(y);
    double fx = x - xi, fy = y - yi;
    double sx = fx * fx * (3 - 2 * fx), sy = fy * fy * (3 - 2 * fy);
    double n0 = LXUtils.lerp(hash(xi, yi, 0), hash(xi + 1, yi, 0), sx);
    double n1 = LXUtils.lerp(hash(xi, yi + 1, 0), hash(xi + 1, yi + 1, 0), sx);
    return LXUtils.lerp(n0, n1, sy);
  }

  /** Two-octave ridged turbulence in [0,1] — bright storm veins. */
  private static double turb(double x, double y) {
    double n1 = Math.abs(vnoise(x, y) * 2 - 1);
    double n2 = Math.abs(vnoise(x * 2.1 + 13.7, y * 2.1 + 5.3) * 2 - 1);
    return 1.0 - (n1 * 0.65 + n2 * 0.35);
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    this.timeMs += deltaMs;

    final double speed = Math.max(0.02, getSpeed());
    final double wow = getWow();
    this.phase += deltaMs * 0.00009 * speed;

    // operator trigger: full while held, then a decaying burst
    if (this.strobe.isOn()) {
      this.strobeEnv = 1;
    } else if (this.strobeEnv > 0) {
      this.strobeEnv = Math.max(0, this.strobeEnv - deltaMs / DECAY_MS);
    }

    final Target t = this.target.getEnum();
    if (t != Target.CYLINDER) {
      sweep(Apotheneum.cube.exterior, wow);
    }
    if (t != Target.CUBE) {
      sweep(Apotheneum.cylinder.exterior, wow);
    }
    copyExterior();

    // pulsating white-hot strobe over everything, interior included
    if (this.strobeEnv > 0) {
      final double hz = this.rate.getValue() * (1.0 + wow * 0.7);
      if (frac(this.timeMs * 0.001 * hz) < STROBE_DUTY) {
        final double env = this.strobeEnv;
        final int flash = LXColor.scaleBrightness(LXColor.WHITE, (float) (env * env));
        for (int i = 0; i < this.colors.length; ++i) {
          this.colors[i] = LXColor.lightest(this.colors[i], flash);
        }
      }
    }
  }

  private void sweep(Apotheneum.Orientation o, double wow) {
    final int w = o.width();
    final int h = o.height();
    final int base = getColor();
    final int hot = LXColor.lerp(base, LXColor.WHITE, 0.75f);

    final double bandW = w * (0.05 + getSize() * 0.13) * (1.0 + wow * 0.5);
    final double xc = frac(this.phase) * w;
    final double nt = this.timeMs * 0.0004;
    final double floorAmt = 0.05 + wow * 0.09;
    final double gain = 1.0 + wow * 0.5;

    for (int x = 0; x < w; ++x) {
      double dx = x - xc;
      dx -= w * Math.rint(dx / w); // wrapped distance to the glint center
      final double adx = Math.abs(dx);

      for (int y = 0; y < h; ++y) {
        // turbulence in glint-relative coords keeps the noise seam hidden behind the sweep
        final double tb = turb(dx * 0.30 + nt * 0.6, y * 0.30 - nt);

        double b = 0;
        int c = base;
        // glint band with a storm-torn edge
        final double dEff = adx + (tb - 0.5) * bandW * 1.1;
        double u = 1.0 - dEff / bandW;
        if (u > 0) {
          u = LXUtils.clamp(u, 0, 1);
          final double sm = u * u * (3 - 2 * u);
          b = Math.pow(sm, 1.6) * (0.45 + 0.75 * tb) * gain;
          c = (sm > 0.92) ? LXColor.WHITE : ((sm > 0.7) ? hot : base);
        }
        // faint storm-shimmer floor everywhere else
        final double fl = floorAmt * tb * tb;
        if (fl > b) {
          b = fl;
          c = base;
        }
        if (b <= 0.004) continue;
        final int idx = o.point(x, y).index;
        this.colors[idx] = LXColor.scaleBrightness(c, (float) LXUtils.clamp(b, 0, 1));
      }
    }
  }
}
