/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Pressure — a two-channel color organ on the strand curtains: the columns
 * gate hard between near-black and saturated washes, each column committed to
 * either red or green, identities re-rolled with every major pulse. Fast
 * attack, slow release — the whole rig breathes dark-bright-dark like a
 * warehouse PA. Where red columns overdrive they bleed amber, and everything
 * pools on the floor line. Modeled on the holotrigger red/green pulse program.
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
public class Pressure extends StrandPattern {

  private static final int MAX_GROUPS = 32;

  public final DiscreteParameter columns =
    new DiscreteParameter("Columns", 24, 8, MAX_GROUPS)
    .setDescription("Strand column groups around the surface");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of the gate");

  // per-group state, shared across surfaces so cube and cylinder pulse together
  private final double[] energy = new double[MAX_GROUPS];
  private final boolean[] isRed = new boolean[MAX_GROUPS];
  private final boolean[] firing = new boolean[MAX_GROUPS];
  private int beatCount = 0;
  private int lastIdlePulse = -1;
  private boolean inited = false;

  public Pressure(LX lx) {
    // Base registers color (hue shift), speed (idle breathe tempo), size (unused softness).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("columns", this.columns);
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
    if (!this.inited) {
      for (int i = 0; i < MAX_GROUPS; ++i) {
        this.isRed[i] = hashd(i * 17 + 3) < 0.4;
        this.firing[i] = true;
        this.energy[i] = 0.4;
      }
      this.inited = true;
    }

    final double wow = getWow();
    if (this.beat) {
      ++this.beatCount;
      // the whole wall swaps identity together each pulse, with a few dissenters
      final boolean globalRed = (this.beatCount & 1) == 1;
      for (int i = 0; i < MAX_GROUPS; ++i) {
        this.firing[i] = hashd(this.beatCount * 131 + i * 7) < (0.70 + this.beatLevel * 0.25 + wow * 0.1);
        this.isRed[i] = globalRed ^ (hashd(this.beatCount * 977 + i * 31) < 0.15);
      }
    }

    // idle breathe when the room is quiet: the ~1.1s gate cadence of the reference
    final double speed = Math.max(0.05, getSpeed());
    final double breathe = 0.5 + 0.5 * Math.sin(this.timeMs * 0.0057 * speed * 2);
    final boolean quiet = this.levelEnv < 0.05;
    // keep the red/green wall-swap running in silence too
    final int idlePulse = (int) (this.timeMs * speed * 2 / 1100.0);
    if (quiet && idlePulse != this.lastIdlePulse) {
      this.lastIdlePulse = idlePulse;
      final boolean globalRed = (idlePulse & 1) == 1;
      for (int i = 0; i < MAX_GROUPS; ++i) {
        this.firing[i] = hashd(idlePulse * 131 + i * 7) < 0.75;
        this.isRed[i] = globalRed ^ (hashd(idlePulse * 977 + i * 31) < 0.15);
      }
    }

    // fast attack / fast-ish release per group (measured ~250-300ms)
    final double aAtk = 1 - Math.exp(-deltaMs / 30.0);
    final double aRel = 1 - Math.exp(-deltaMs / (270.0 - wow * 90));
    for (int i = 0; i < MAX_GROUPS; ++i) {
      double target;
      if (quiet) {
        target = (0.15 + 0.75 * breathe) * (this.firing[i] ? 1 : 0.35);
      } else {
        target = this.firing[i]
          ? LXUtils.clamp(0.35 + this.levelEnv * 0.9 + this.beatLevel * 0.4 + wow * 0.2, 0, 1.3)
          : 0.06;
      }
      double a = (target > this.energy[i]) ? aAtk : aRel;
      this.energy[i] += (target - this.energy[i]) * a;
    }
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final int w = o.width();
    final int h = o.height();
    final int n = this.columns.getValuei();
    final double hueShift = LXColor.h(getColor());
    final int green = LXColor.hsb((float) (((120 + hueShift) % 360)), 98, 100);
    final int red = LXColor.hsb((float) (((2 + hueShift) % 360)), 100, 100);
    final int amber = LXColor.hsb((float) (((30 + hueShift) % 360)), 92, 100);
    final int tq = (int) (this.timeMs / 80);

    for (int x = 0; x < w; ++x) {
      final int g = Math.min(n - 1, x * n / w);
      final double e = this.energy[g];
      if (e <= 0.01) continue;
      // soft edges between neighbor groups
      final double gf = ((double) x * n / w) - g;
      final double edge = Math.min(1.0, Math.min(gf, 1 - gf) * 6 + 0.35);
      final double shim = 0.92 + 0.08 * hashd(x * 131 + tq * 7);
      int c = this.isRed[g] ? red : green;
      // overdriven red bleeds amber
      if (this.isRed[g] && e > 0.85) {
        c = LXColor.lerp(red, amber, (float) LXUtils.clamp((e - 0.85) * 4, 0, 1));
      }
      final int hot = LXColor.lerp(c, LXColor.WHITE, 0.35f);

      for (int y = 0; y < h; ++y) {
        double b = e * drip(y, h) * edge * shim;
        b += e * pool(y, h);
        if (b <= 0.01) continue;
        addPix(o, x, y, (e > 1.0 && y < 3) ? hot : c, b);
      }
    }
  }
}
