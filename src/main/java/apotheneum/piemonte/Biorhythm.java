/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Biorhythm — the strand curtains become a living readout: a breathing emerald
 * curtain fills the columns while a bright red spectrum ribbon runs around the
 * ring like an EKG, each column's spike tracing a frequency band of whatever's
 * playing. Layered, never blended — red data on green field. Drops flood the
 * room green; quiet moments leave the heartbeat line running alone. Modeled on
 * the holotrigger green/red waveform program.
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
public class Biorhythm extends StrandPattern {

  private static final int MAX_BINS = 40;

  public final DiscreteParameter bins =
    new DiscreteParameter("Bins", 24, 8, MAX_BINS)
    .setDescription("Frequency bins around the ring");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of floods and spikes");

  private final double[] mag = new double[MAX_BINS]; // smoothed bin magnitudes
  private double flashEnv = 0;
  private double sparkleEnv = 0;

  public Biorhythm(LX lx) {
    // Base registers color (hue shift), speed (idle wave tempo), size (ribbon thickness).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("bins", this.bins);
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
    final int n = this.bins.getValuei();
    final double a = 1 - Math.exp(-deltaMs / 90.0); // temporal smoothing
    for (int i = 0; i < n; ++i) {
      // mirror bins around the ring so the spectrum wraps without a seam
      int half = Math.max(1, n / 2);
      int bi = (i < half) ? i : (n - 1 - i);
      double target = band(bi, half);
      this.mag[i] += (target - this.mag[i]) * a;
    }
    if (this.beat && (this.beatLevel > 0.5 || this.levelEnv > 0.55)) {
      this.flashEnv = 1;
    }
    if (this.beat && this.beatLevel > 0.75) {
      this.sparkleEnv = 1; // the rainbow-speckle moment
    }
    this.flashEnv *= Math.exp(-deltaMs / 380.0);
    this.sparkleEnv *= Math.exp(-deltaMs / 600.0);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final int w = o.width();
    final int h = o.height();
    final int n = this.bins.getValuei();
    final double wow = getWow();
    final double hueShift = LXColor.h(getColor());
    final int green = LXColor.hsb((float) (((125 + hueShift) % 360)), 95, 100);
    final int red = LXColor.hsb((float) (((0 + hueShift) % 360) + 360) % 360, 100, 100);
    final int tq = (int) (this.timeMs / 80);

    // idle wave keeps the EKG alive with no audio
    final double speed = Math.max(0.05, getSpeed());
    final boolean quiet = this.levelEnv < 0.04;
    final double idleT = this.timeMs * 0.0018 * speed;

    // green curtain base, breathing with the level
    final double baseB = 0.20 + 0.45 * LXUtils.clamp(this.levelEnv * 1.5, 0, 1)
      + this.flashEnv * this.flashEnv * (0.5 + wow * 0.3);
    for (int x = 0; x < w; ++x) {
      final double shim = 0.90 + 0.10 * hashd(x * 131 + tq * 7);
      for (int y = 0; y < h; ++y) {
        double b = baseB * drip(y, h) * shim + baseB * pool(y, h);
        addPix(o, x, y, green, b);
      }
    }

    // spectrum ribbon with per-bin spikes: baseline at mid-height, peaks reaching
    // toward the top rim; ribbon color cycles red->green (~2.4s round trip)
    final int y0 = (int) (h * 0.52);
    final double cyc = 0.5 + 0.5 * Math.sin(2 * Math.PI * this.timeMs / 2400.0);
    final int ribbon = LXColor.lerp(red,
      LXColor.hsb((float) (((125 + LXColor.h(getColor())) % 360)), 95, 100), (float) cyc);
    final int ribbonHot = LXColor.lerp(ribbon, LXColor.WHITE, 0.45f);
    final double amp = h * (0.42 + wow * 0.25) * (1 + this.beatLevel * 0.6); // peaks near full height
    final double thick = 1.0 + getSize() * 1.5;
    for (int x = 0; x < w; ++x) {
      final int bi = Math.min(n - 1, x * n / w);
      double m = quiet
        ? 0.25 + 0.25 * Math.sin(idleT + (double) x / w * Math.PI * 4)
        : LXUtils.clamp(this.mag[bi] * (1.2 + this.sensitivity.getValue()), 0, 1);
      final double spike = Math.min(y0 - 1, m * amp);
      // ribbon body
      for (int y = (int) Math.floor(y0 - spike); y <= y0 + (int) thick; ++y) {
        double b;
        if (y < y0) {
          double u = 1.0 - (y0 - y) / Math.max(1.0, spike); // fade toward spike tip
          b = 0.45 + 0.55 * u;
        } else {
          b = 1.0 - (y - y0) / (thick + 1);
        }
        if (b <= 0.02) continue;
        final int c = (y == (int) Math.floor(y0 - spike) && spike > 3) ? ribbonHot : ribbon;
        addPix(o, x, y, c, b);
      }
    }

    // rainbow speckle burst
    if (this.sparkleEnv > 0.05) {
      final int glints = (int) (w * h * 0.015 * this.sparkleEnv);
      for (int k = 0; k < glints; ++k) {
        int gx = (int) (Math.random() * w);
        int gy = (int) (Math.random() * h);
        int c = LXColor.hsb((float) (Math.random() * 360), 80, 100);
        addPix(o, gx, gy, c, 0.4 + Math.random() * 0.6);
      }
    }
  }
}
