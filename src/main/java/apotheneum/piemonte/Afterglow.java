/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Afterglow — joints of light scattered across the surfaces, each constantly
 * pulsing white dots outward along its own rays — a handful of directions at
 * arbitrary angles, like edges meeting at a vertex. The joints themselves stay
 * dark; all you see is the endless soft exhale of dots leaving them and dying
 * away, every ray on its own phase. Kicks flare the field, drops send one
 * synchronized wave down every ray at once, and Wow grows extra rays.
 * After the LXStudio-TE Afterglow.
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
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class Afterglow extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final int MAX_STARS = 48;
  private static final double CYCLE_MS = 3000; // one pulse cycle at speed 1

  public final DiscreteParameter stars =
    new DiscreteParameter("Stars", 20, 6, MAX_STARS)
    .setDescription("How many stars pulse on each surface");

  public final CompoundParameter reach =
    new CompoundParameter("Reach", 0.55, 0.2, 1)
    .setDescription("How far the pulses travel before dying");

  public final DiscreteParameter pulses =
    new DiscreteParameter("Pulses", 3, 1, 6)
    .setDescription("Concurrent pulses per ray");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("How strongly the stars follow the music");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  private double phase = 0;

  // audio: kicks flare the cores, drops send one wave from every star at once
  private double bassAvg, prevBass, sinceBeatMs = 1e9;
  private double levelEnv = 0;
  private double beatPulse = 0;
  private double unison = -1; // 0..1 while a unison wave is traveling; <0 idle

  public Afterglow(LX lx) {
    // Base registers color (star tint), speed (pulse rate), size (dot size).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("stars", this.stars);
    addParameter("reach", this.reach);
    addParameter("pulses", this.pulses);
    addParameter("sensitivity", this.sensitivity);
    addParameter("target", this.target);
  }

  /** Returns 0 = nothing, 1 = beat, 2 = drop. */
  private int detect(double deltaMs) {
    heronarts.lx.audio.GraphicMeter m = this.lx.engine.audio.meter;
    int nb = Math.max(1, m.numBands);
    double bass = m.getAveragef(0, Math.max(1, nb / 4));
    this.bassAvg += (bass - this.bassAvg) * (1 - Math.exp(-deltaMs / 400.0));
    this.sinceBeatMs += deltaMs;
    double sens = this.sensitivity.getValue();
    double threshold = 1.05 + (1 - sens) * 0.7;
    boolean beat = (bass > this.bassAvg * threshold)
      && (bass > this.prevBass)
      && (this.sinceBeatMs >= 140)
      && (bass > 0.01);
    double ratio = LXUtils.clamp((bass / Math.max(1e-4, this.bassAvg) - 1.0) / 1.5, 0, 1);
    this.prevBass = bass;
    if (beat) {
      this.sinceBeatMs = 0;
    }
    double level = m.getAveragef(0, nb);
    double alpha = (level > this.levelEnv)
      ? 1 - Math.exp(-deltaMs / 25.0)
      : 1 - Math.exp(-deltaMs / 220.0);
    this.levelEnv += (level - this.levelEnv) * alpha;
    if (!beat) return 0;
    return (ratio > 0.60 - sens * 0.15) ? 2 : 1;
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    final double speed = Math.max(0.02, getSpeed());
    final int hit = detect(deltaMs);
    if (hit == 1) {
      this.beatPulse = 1; // cores flare on the kick
    } else if (hit == 2 && this.unison < 0) {
      this.unison = 0; // the drop: every star fires one wave together
      this.beatPulse = 1;
    }
    this.beatPulse *= Math.exp(-deltaMs / 240.0);
    if (this.unison >= 0) {
      this.unison += deltaMs / 700.0;
      if (this.unison >= 1) this.unison = -1;
    }
    // louder music quickens the breathing slightly
    this.phase += deltaMs * speed * (1.0 + this.levelEnv * 0.4) / CYCLE_MS;

    final Target t = this.target.getEnum();
    if (t != Target.CYLINDER) {
      step(Apotheneum.cube.exterior, 1);
    }
    if (t != Target.CUBE) {
      step(Apotheneum.cylinder.exterior, 2);
    }
    copyExterior();
  }

  private void step(Apotheneum.Orientation o, int seed) {
    final int w = o.width();
    final int h = o.height();
    final int base = getColor();
    final int dot = LXColor.lerp(base, LXColor.WHITE, 0.75f); // near-white pulses
    final double wow = getWow();
    final int nStars = this.stars.getValuei();
    final int nPulses = this.pulses.getValuei();
    final double maxTravel = (h * 0.5) * this.reach.getValue();
    final double dotR = 1.0 + getSize() * 1.6; // >=1 so sub-pixel motion stays smooth
    final boolean diagonals = wow > 0.15;
    final double gain = (1.0 + wow * 0.5) * (1.0 + this.levelEnv * 0.5 + this.beatPulse * 0.35);

    for (int i = 0; i < nStars; ++i) {
      final int sx = (int) (hashd(i * 31 + seed * 7919) * w);
      final int sy = 2 + (int) (hashd(i * 53 + seed * 104729) * (h - 4));
      final double offset = hashd(i * 97 + seed * 1299709);

      // each joint has its own set of rays at arbitrary angles, like edges
      // meeting at a vertex — no lit core, just dots constantly leaving it
      final int nRays = 3 + (int) (hashd(i * 131 + seed * 15485863) * 3) + (diagonals ? 2 : 0);
      for (int r = 0; r < nRays; ++r) {
        final double ang = hashd(i * 197 + r * 613 + seed * 27644437) * 2 * Math.PI;
        final double ux = Math.cos(ang), uy = Math.sin(ang);
        final double rayLen = maxTravel * (0.6 + 0.4 * hashd(i * 331 + r * 89 + seed));
        final double rayOff = hashd(i * 419 + r * 157 + seed * 3) * 0.5; // desync directions

        // dots constantly streaming out along this ray
        for (int p = 0; p < nPulses; ++p) {
          final double adj = (this.phase + offset + rayOff + (double) p / nPulses) % 1.0;
          final double travel = adj * rayLen;
          final double in = LXUtils.clamp(adj / 0.08, 0, 1);
          final double fade = Math.pow(1.0 - adj, 1.4) * gain * (in * in * (3 - 2 * in));
          if (fade <= 0.01) continue;
          splat(o, w, h, sx + ux * travel, sy + uy * travel, dotR, fade, dot);
        }

        // the drop: one synchronized white wave down every ray of every joint
        if (this.unison >= 0) {
          final double uTravel = this.unison * rayLen;
          final double uFade = Math.pow(1.0 - this.unison, 1.2) * (1.2 + wow * 0.5);
          if (uFade > 0.01 && uTravel >= 0.5) {
            splat(o, w, h, sx + ux * uTravel, sy + uy * uTravel, dotR, uFade, LXColor.WHITE);
          }
        }
      }
    }
  }

  /** Soft round dab; x wraps, y clips. */
  private void splat(Apotheneum.Orientation o, int w, int h, double x, double y,
      double r, double bright, int color) {
    if (bright <= 0.004) return;
    final int y0 = (int) Math.max(0, Math.floor(y - r));
    final int y1 = (int) Math.min(h - 1, Math.ceil(y + r));
    final int x0 = (int) Math.floor(x - r);
    final int x1 = (int) Math.ceil(x + r);
    for (int yy = y0; yy <= y1; ++yy) {
      final double dy = yy - y;
      for (int xx = x0; xx <= x1; ++xx) {
        final double dx = xx - x;
        final double dd = Math.sqrt(dx * dx + dy * dy);
        if (dd > r) continue;
        final double u = 1.0 - dd / r;
        final double f = u * u * bright;
        if (f <= 0.004) continue;
        final int xi = ((xx % w) + w) % w;
        final int idx = o.point(xi, yy).index;
        this.colors[idx] = LXColor.lightest(this.colors[idx],
          LXColor.scaleBrightness(color, (float) LXUtils.clamp(f, 0, 1)));
      }
    }
  }
}
