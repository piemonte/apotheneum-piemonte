/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Zoetrope — red vertical bars orbiting the fixture. A handful of dotted
 * partial-height bars, each with a hot amber core fading to deep red at the
 * ends, spin smoothly around the cylinder and cube like slits in a zoetrope
 * drum. Brightness pulses with the beat; Speed is bipolar, so the drum spins
 * either direction. Turn up Wow and the spin goes stepped and glitchy — the
 * bars jump azimuth on hits and flicker, the way the reference actually moves.
 * Split out of the Mainframe reference (holotrigger studio session).
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
public class Zoetrope extends StrandPattern {

  private static final int MAX_BARS = 12;
  private static final double REV_PER_SEC = 0.35; // at |Speed| = 1 (measured ~0.3-0.5)

  public final DiscreteParameter bars =
    new DiscreteParameter("Bars", 4, 1, MAX_BARS)
    .setDescription("Red bars orbiting the fixture");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("How strongly beats pulse and step the bars");

  private double phase = 0;      // 0..1 revolution
  private double stepKick = 0;   // Wow: azimuth jump impulse on hits
  private double pulse = 0;

  public Zoetrope(LX lx) {
    // Base registers color (bar tint), bipolar speed (spin rate + direction),
    // size (bar height), wow (stepped glitch spin).
    super(lx, 0.35, -1, 1, 0.5, 0, 1);
    addParameter("bars", this.bars);
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

  @Override
  protected void advance(double deltaMs) {
    final double speed = getSpeed(); // bipolar: sign = direction
    final double wow = getWow();
    // smooth orbit; Wow trades smooth spin for stepped jumps on the beat
    this.phase += deltaMs * 0.001 * REV_PER_SEC * speed * (1.0 - wow * 0.75);
    if (this.beat) {
      this.pulse = 1;
      if (wow > 0.1) {
        // stepped rotation: jump a sixteenth-turn on hits, like the footage
        this.stepKick += (1.0 / 16.0) * (speed >= 0 ? 1 : -1) * (0.5 + wow);
      }
    }
    // the kick eases in quickly so steps read as jumps, not slides
    final double ease = 1 - Math.exp(-deltaMs / 60.0);
    this.phase += this.stepKick * ease;
    this.stepKick *= (1 - ease);
    this.phase -= Math.floor(this.phase);
    this.pulse *= Math.exp(-deltaMs / 200.0);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final int w = o.width();
    final int h = o.height();
    final double wow = getWow();
    final double hueShift = LXColor.h(getColor());
    final int red = LXColor.hsb((float) (((4 + hueShift) % 360) + 360) % 360, 100, 100);
    final int amber = LXColor.hsb((float) (((24 + hueShift) % 360)), 92, 100);
    final int n = this.bars.getValuei();
    final int tq = (int) (this.timeMs / 80);
    final double bright = (0.75 + 0.25 * this.levelEnv) * (1.0 + this.pulse * (0.3 + wow * 0.4));

    for (int i = 0; i < n; ++i) {
      // evenly spaced azimuths with per-bar jitter; all share the orbit phase
      final double az = (double) i / n + (hashd(i * 31 + 7) - 0.5) * 0.06 + this.phase;
      final double xc = (az - Math.floor(az)) * w;
      // partial height: middle-third bars with per-bar variance
      final double yc = h * (0.38 + 0.24 * hashd(i * 53 + 11));
      final double halfH = h * (0.10 + getSize() * 0.14) * (0.8 + 0.4 * hashd(i * 97 + 3));
      // Wow glitch: per-bar flicker dropout
      if (wow > 0.3 && hashd(i * 131 + tq * 7) < wow * 0.18) continue;

      final int x0 = (int) Math.floor(xc);
      final double xf = xc - x0;
      for (int dy = (int) -halfH; dy <= halfH; ++dy) {
        final int y = (int) yc + dy;
        if (y < 0 || y >= h) continue;
        // hot core at the middle fading to the ends, dotted per-LED texture
        final double core = 1.0 - Math.abs(dy) / (halfH + 1);
        final double dot = 0.6 + 0.4 * hashd(i * 977 + y * 131 + tq * 3);
        final double b = bright * (0.25 + 0.75 * core * core) * dot;
        final int c = (core > 0.72) ? amber : red;
        // sub-pixel across two columns so the spin glides
        addPix(o, x0, y, c, b * (1 - xf));
        addPix(o, x0 + 1, y, c, b * xf);
        // faint side bleed
        addPix(o, x0 - 1, y, red, b * 0.25 * (1 - xf));
        addPix(o, x0 + 2, y, red, b * 0.25 * xf);
      }
    }
  }
}
