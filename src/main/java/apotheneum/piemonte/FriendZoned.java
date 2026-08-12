/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * FriendZoned — concentric circles are born at the center of each facade and
 * slowly expand outward. Close to the center each ring is a continuous
 * circle; as it expands, small splits crack open along its edge, the splits
 * widen into gaps, and by the time a ring nears the canvas edge it has
 * crumbled into drifting lines and dots. Every ring spins on its own —
 * randomly clockwise or counter-clockwise — and the radial gap between
 * successive rings is randomized. On the cylinder the rings rise instead:
 * each circle wraps the full circumference and expands from the base to the
 * top. Splits bounds how many cracks a ring can develop; Spin scales the
 * rotation rate; Size runs the stroke from hairline to fat; beats flare
 * the rings; a Pulse tap sweeps
 * every ring to white and melts back to the set color; Wow deepens the
 * fragment flicker and spreads the ring hues.
 *
 * WARNING: Flashing imagery, best viewed in deep playa
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
public class FriendZoned extends StrandPattern {

  private static final int MAX_RINGS = 14;
  private static final double V_EXPAND = 0.00013; // normalized radius per ms at speed 0.5
  private static final double DONE = 1.08;        // past the canvas edge, ring retires

  /** Upper bound on the per-ring random split count (each ring rolls 3..Splits). */
  public final DiscreteParameter splits =
    new DiscreteParameter("Splits", 12, 4, 25)
    .setDescription("Maximum splits a ring can crack into");

  /** Rotation-rate multiplier on every ring's random spin. */
  public final CompoundParameter spin =
    new CompoundParameter("Spin", 1, 0, 4)
    .setDescription("Rotation speed of the rings");

  // ring slots (normalized radius space; shared across surfaces)
  private final boolean[] alive = new boolean[MAX_RINGS];
  private final double[] rad = new double[MAX_RINGS];    // 0 = center, 1 = canvas edge
  private final double[] rot = new double[MAX_RINGS];    // accumulated spin (radians)
  private final double[] rotV = new double[MAX_RINGS];   // signed spin rate, rad/ms
  private final int[] segs = new int[MAX_RINGS];         // primary split count
  private final double[] segPh = new double[MAX_RINGS];  // split-mask phase
  private final double[] gap = new double[MAX_RINGS];    // radial gap before the next ring
  private final double[] hueOff = new double[MAX_RINGS];

  private int newest = -1;
  private double flare = 0;
  // Pulse-tap whiteout: rings ease to white, then melt back to color
  private double whiteEnv = 0;
  private boolean whiteRising = false;

  public FriendZoned(LX lx) {
    // Base registers hue (ring color), speed (expansion rate), size (ring thickness).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("splits", this.splits);
    addParameter("spin", this.spin);
    addTargetParameter();
  }

  private static double frac(double v) {
    return v - Math.floor(v);
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  private void spawnRing() {
    for (int i = 0; i < MAX_RINGS; ++i) {
      if (this.alive[i]) continue;
      this.alive[i] = true;
      this.rad[i] = 0;
      this.rot[i] = Math.random() * 2 * Math.PI;
      // random spin, randomly clockwise or counter-clockwise
      this.rotV[i] = (Math.random() < 0.5 ? -1 : 1)
        * (0.00008 + Math.random() * 0.00030);
      this.segs[i] = 3 + (int) (Math.random() * (this.splits.getValuei() - 2));
      this.segPh[i] = Math.random();
      this.gap[i] = 0.05 + Math.random() * 0.12;
      this.hueOff[i] = (Math.random() - 0.5) * 36;
      this.newest = i;
      return;
    }
  }

  @Override
  protected void advance(double deltaMs) {
    final double dt = deltaMs * Math.max(0.05, getSpeed()) * 2;

    if (this.beat) {
      this.flare = 1;
    }
    this.flare *= Math.exp(-deltaMs / 300.0);

    // a Pulse tap sweeps every ring to white and back: quick ease up
    // (~70ms), slow melt back down (~550ms) — no strobing
    if (pulseHit()) {
      this.whiteRising = true;
    }
    if (this.whiteRising) {
      this.whiteEnv += (1 - this.whiteEnv) * (1 - Math.exp(-deltaMs / 70.0));
      if (this.whiteEnv > 0.96) {
        this.whiteRising = false;
      }
    } else {
      this.whiteEnv *= Math.exp(-deltaMs / 550.0);
    }

    for (int i = 0; i < MAX_RINGS; ++i) {
      if (!this.alive[i]) continue;
      this.rad[i] += V_EXPAND * dt; // uniform expansion keeps the gaps intact
      this.rot[i] += this.rotV[i] * this.spin.getValue() * dt;
      if (this.rad[i] > DONE) {
        this.alive[i] = false;
        if (this.newest == i) this.newest = -1;
      }
    }

    // continuous stream: birth the next ring once the youngest has cleared
    // its own randomized gap
    if (this.newest < 0 || this.rad[this.newest] >= this.gap[this.newest]) {
      spawnRing();
    }
  }

  /**
   * Angular coverage of ring i at angle theta, given how far out it is (u).
   * Splits crack open partway out, widen, then the surviving arcs crumble
   * into dots toward the canvas edge.
   */
  private double arcMask(int i, double theta, double u, int tq) {
    final double a = theta + this.rot[i];
    double m = 1;

    final double crack = LXUtils.clamp((u - 0.18) / 0.55, 0, 1);
    if (crack > 0) {
      final double f = frac(a / (2 * Math.PI) * this.segs[i] + this.segPh[i]);
      final double dGap = Math.min(f, 1 - f);      // 0 at split center
      final double gapW = 0.005 + 0.42 * crack;    // hairline crack → wide gap
      m *= LXUtils.clamp((dGap - gapW * 0.5) / 0.10, 0, 1);
    }

    final double dotty = LXUtils.clamp((u - 0.62) / 0.34, 0, 1);
    if (dotty > 0 && m > 0) {
      final double f2 = frac(a / (2 * Math.PI) * this.segs[i] * 3 + this.segPh[i] * 2.7);
      final double d2 = Math.min(f2, 1 - f2);      // 0 mid-dot
      final double duty = 1.0 - 0.62 * dotty;      // arcs shrink to dots
      final double dm = LXUtils.clamp((duty * 0.5 - d2) / 0.10, 0, 1);
      m *= LXUtils.lerp(1, dm, dotty);
      // crumbled fragments shimmer; Wow deepens the flicker
      final double depth = 0.12 + getWow() * 0.5;
      m *= 1 - depth * dotty * hashd(i * 977 + (int) (f2 * 37) + tq);
    }
    return m;
  }

  private int ringColor(int i, float baseHue, double u) {
    final double spread = 1 + getWow() * 1.5;
    final float hu = (float) ((((baseHue + this.hueOff[i] * spread) % 360) + 360) % 360);
    final int c = LXColor.hsb(hu, 92, 100);
    // rings are born white-hot at the center, cooling into color as they grow
    final float hotf = (float) (LXUtils.clamp((0.15 - u) / 0.15, 0, 1) * 0.7);
    return (hotf > 0) ? LXColor.lerp(c, LXColor.WHITE, hotf) : c;
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final int w = o.width();
    final int h = o.height();
    final int tq = (int) (this.timeMs / 90);
    final float baseHue = LXColor.h(getColor());
    final double gain = 1 + this.flare * 0.5;

    // Pulse whiteout blend for this frame
    final float wht = (float) (this.whiteEnv * 0.9);

    if (isCube) {
      // circles expand from the center of each face
      final int faceW = w / 4;
      final double cyf = (h - 1) / 2.0;
      final double cxf = (faceW - 1) / 2.0;
      final double rMax = Math.sqrt(cxf * cxf + cyf * cyf);
      final double halfW = 0.2 + getSize() * 3.0; // Size: hairline → fat stroke

      for (int face = 0; face < 4; ++face) {
        final int x0 = face * faceW;
        for (int lx = 0; lx < faceW; ++lx) {
          final double dx = lx - cxf;
          for (int y = 0; y < h; ++y) {
            final double dy = y - cyf;
            final double rPix = Math.sqrt(dx * dx + dy * dy);
            double theta = 0;
            boolean haveTheta = false;
            for (int i = 0; i < MAX_RINGS; ++i) {
              if (!this.alive[i]) continue;
              final double dR = Math.abs(rPix - this.rad[i] * rMax);
              if (dR > halfW * 3 + 1) continue;
              if (!haveTheta) {
                theta = Math.atan2(dy, dx);
                haveTheta = true;
              }
              // gaussian cross-section: bright core melting into a soft glow
              final double dn = dR / (halfW + 0.35);
              final double cov = Math.exp(-1.6 * dn * dn);
              final double m = arcMask(i, theta, this.rad[i], tq);
              if (m <= 0.02) continue;
              int c = ringColor(i, baseHue, this.rad[i]);
              if (wht > 0.01) {
                c = LXColor.lerp(c, LXColor.WHITE, wht);
              }
              addPix(o, x0 + lx, y, c, Math.min(1, cov * m * gain));
            }
          }
        }
      }
    } else {
      // on the cylinder the circles wrap the circumference and rise from the base
      final double halfW = 0.15 + getSize() * 2.2; // Size: hairline → fat stroke
      for (int x = 0; x < w; ++x) {
        final double theta = (double) x / w * 2 * Math.PI;
        for (int i = 0; i < MAX_RINGS; ++i) {
          if (!this.alive[i]) continue;
          final double ringY = (h - 1) - this.rad[i] * (h - 1);
          final int yLo = (int) Math.floor(ringY - halfW * 3 - 1);
          final int yHi = (int) Math.ceil(ringY + halfW * 3 + 1);
          final double m = arcMask(i, theta, this.rad[i], tq);
          if (m <= 0.02) continue;
          int c = ringColor(i, baseHue, this.rad[i]);
          if (wht > 0.01) {
            c = LXColor.lerp(c, LXColor.WHITE, wht);
          }
          for (int y = Math.max(0, yLo); y <= Math.min(h - 1, yHi); ++y) {
            // gaussian cross-section, matching the cube's soft-glow ring profile
            final double dn = Math.abs(y - ringY) / (halfW + 0.35);
            final double cov = Math.exp(-1.6 * dn * dn);
            if (cov <= 0.02) continue;
            addPix(o, x, y, c, Math.min(1, cov * m * gain));
          }
        }
      }
    }
  }
}
