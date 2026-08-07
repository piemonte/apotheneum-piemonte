/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * LockedIn — sets of thick bars lock into the center of the canvas, one color
 * at a time. A set of bars at varied lengths beams in from the bottom with
 * the UFOAbduction feel — fast entry, decelerating as they near the middle —
 * then the whole set bops up and down for a moment before freezing in place.
 * The next set enters from the top at a new horizontal offset and color,
 * overlaps, bops, freezes. Five colors stack this way — alternating bottom
 * and top entries — then the whole lock releases, fades, and the cycle
 * begins again. Beats punch the bop; Wow adds bounce energy and jitter.
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
public class LockedIn extends StrandPattern {

  private static final int MAX_GENS = 5;   // five colors stack per cycle
  private static final int MAX_BARS = 16;
  // UFOAbduction-style approach: quick entry, deceleration into the center
  private static final double V_ENTER = 0.0016;  // heights/ms far from rest
  private static final double V_NEAR = 0.00030;  // crawl arriving at center
  private static final double BOP_MS = 1500;     // bop phase duration
  private static final double BOP_PERIOD = 380;  // bounce period
  private static final double HOLD_MS = 2000;    // full stack hold before release
  private static final double FADE_MS = 900;
  private static final double[] HUE_STEPS = { 0, 52, 125, 205, 288 };

  public final DiscreteParameter bars =
    new DiscreteParameter("Bars", 9, 4, MAX_BARS)
    .setDescription("Bars per color set");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("How hard beats punch the bop");

  // generation layout (normalized; shared across surfaces)
  private final double[][] genX = new double[MAX_GENS][MAX_BARS];
  private final double[][] genLen = new double[MAX_GENS][MAX_BARS];
  private final double[][] genYc = new double[MAX_GENS][MAX_BARS];
  private final double[][] lenVar = new double[MAX_GENS][MAX_BARS];
  private final boolean[] fromTop = new boolean[MAX_GENS];

  private int gen = 0;          // active generation
  private int phase = 0;        // 0 = enter, 1 = bop, 2 = hold-all, 3 = fade-all
  private double pos = 1.2;     // active set's travel position (normalized offset)
  private double phaseT = 0;
  private double bopKick = 0;
  private boolean laidOut = false;

  public LockedIn(LX lx) {
    // Base registers color (wheel anchor), speed (pace), size (bar thickness).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("bars", this.bars);
    addParameter("sensitivity", this.sensitivity);
    addTargetParameter();
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  private void layout() {
    for (int g = 0; g < MAX_GENS; ++g) {
      this.fromTop[g] = (g & 1) == 1; // alternate bottom / top entries
      final double off = Math.random(); // per-set horizontal offset
      for (int b = 0; b < MAX_BARS; ++b) {
        this.genX[g][b] = (off + (double) b / MAX_BARS
          + (Math.random() - 0.5) * 0.02) % 1.0;
        this.genLen[g][b] = 0.14 + Math.random() * 0.30; // varied lengths
        this.genYc[g][b] = 0.42 + Math.random() * 0.16;  // rest around center
        this.lenVar[g][b] = 0.8 + Math.random() * 0.4;
      }
    }
    this.gen = 0;
    this.phase = 0;
    this.pos = 1.2;
    this.phaseT = 0;
    this.laidOut = true;
  }

  @Override
  protected void advance(double deltaMs) {
    if (!this.laidOut) {
      layout();
    }
    final double speed = Math.max(0.05, getSpeed());
    final double wow = getWow();
    final double dt = deltaMs * speed * 2;

    if (this.beat) {
      this.bopKick = 1;
    }
    this.bopKick *= Math.exp(-deltaMs / 260.0);

    switch (this.phase) {
      case 0: { // entering: travel toward the center, decelerating on approach
        final double target = 0;
        final double dist = Math.abs(this.pos - target);
        final double v = LXUtils.lerp(V_NEAR, V_ENTER,
          LXUtils.clamp(dist / 0.45, 0, 1));
        this.pos -= Math.signum(this.pos) * v * dt;
        if (dist < 0.015) {
          this.pos = 0;
          this.phase = 1;
          this.phaseT = 0;
        }
        break;
      }
      case 1: { // bopping
        this.phaseT += dt;
        if (this.phaseT >= BOP_MS) {
          // freeze this set; launch the next, or hold the finished stack
          if (this.gen + 1 < MAX_GENS) {
            ++this.gen;
            this.phase = 0;
            this.pos = this.fromTop[this.gen] ? -1.2 : 1.2;
          } else {
            this.phase = 2;
            this.phaseT = 0;
          }
        }
        break;
      }
      case 2: { // full five-color lock: hold
        this.phaseT += dt;
        if (this.phaseT >= HOLD_MS) {
          this.phase = 3;
          this.phaseT = 0;
        }
        break;
      }
      case 3: { // release: fade, then relayout and go again
        this.phaseT += dt;
        if (this.phaseT >= FADE_MS) {
          layout();
        }
        break;
      }
    }
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final int w = o.width();
    final int h = o.height();
    final double wow = getWow();
    final double hueShift = LXColor.h(getColor());
    final int nBars = this.bars.getValuei();
    final double thickC = Math.max(2, (2.5 + getSize() * 5) * (isCube ? 1.6 : 1.0));
    final double fadeAll = (this.phase == 3)
      ? 1.0 - LXUtils.clamp(this.phaseT / FADE_MS, 0, 1) : 1.0;
    final int tq = (int) (this.timeMs / 80);

    // active set's bop offset: bounce that beats can punch harder
    final double bopA = (this.phase == 1)
      ? (0.05 + wow * 0.045 + this.bopKick * 0.04 * this.sensitivity.getValue())
        * Math.sin(2 * Math.PI * this.phaseT / BOP_PERIOD)
        * (1.0 - 0.35 * LXUtils.clamp(this.phaseT / BOP_MS, 0, 1))
      : 0;

    for (int g = 0; g <= this.gen && g < MAX_GENS; ++g) {
      final boolean active = (g == this.gen) && (this.phase <= 1);
      final int col = LXColor.hsb(
        (float) (((hueShift + HUE_STEPS[g]) % 360) + 360) % 360, 92, 100);
      final int hot = LXColor.lerp(col, LXColor.WHITE, 0.55f);
      final double genB = (active ? 1.0 : 0.82) * fadeAll;

      for (int b = 0; b < nBars; ++b) {
        final double xN = this.genX[g][b];
        final double len = this.genLen[g][b] * this.lenVar[g][b];
        double yc = this.genYc[g][b];
        if (active) {
          if (this.phase == 0) {
            yc += this.pos; // still traveling in
          } else {
            yc += bopA * (0.8 + 0.4 * hashd(g * 97 + b * 31)); // bopping
          }
        }
        final double topF = (yc - len / 2) * (h - 1);
        final double botF = (yc + len / 2) * (h - 1);
        if (botF < -1 || topF > h) continue;

        final int x0 = (int) Math.round(xN * w);
        final int bw = (int) thickC;
        // white-hot leading edge while traveling
        final boolean entering = active && this.phase == 0;
        final double leadY = (this.fromTop[g] ? botF : topF);

        for (int dx = 0; dx < bw; ++dx) {
          final int x = x0 + dx;
          for (int yy = (int) Math.floor(topF) - 1; yy <= (int) Math.ceil(botF) + 1; ++yy) {
            if (yy < 0 || yy >= h) continue;
            final double dOut = (yy < topF) ? (topF - yy) : ((yy > botF) ? (yy - botF) : 0);
            final double cov = LXUtils.clamp(1.0 - dOut, 0, 1);
            if (cov <= 0.02) continue;
            int c = col;
            double bb = genB * cov * (0.80 + 0.20 * hashd(x * 131 + yy * 7 + (active ? tq : 0)));
            if (entering && Math.abs(yy - leadY) < 1.5) {
              c = hot;
              bb = Math.min(1, bb * 1.4);
            }
            addPix(o, x, yy, c, bb);
          }

          // gradient tail filling all the way back to the bar's entry edge:
          // bottom-entering bars trail to the floor, top-entering to the sky
          if (this.fromTop[g]) {
            final int tailStart = (int) Math.floor(topF) - 1;
            final double span = Math.max(1, topF);
            for (int yy = tailStart; yy >= 0; --yy) {
              final double t2 = (topF - yy) / span;
              final double tb = genB * (0.10 + 0.45 * Math.pow(1.0 - t2, 1.4));
              addPix(o, x, yy, col, tb);
            }
          } else {
            final int tailStart = (int) Math.ceil(botF) + 1;
            final double span = Math.max(1, (h - 1) - botF);
            for (int yy = tailStart; yy < h; ++yy) {
              final double t2 = (yy - botF) / span;
              final double tb = genB * (0.10 + 0.45 * Math.pow(1.0 - t2, 1.4));
              addPix(o, x, yy, col, tb);
            }
          }
        }
      }
    }
  }
}
