/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Motherboard — the strand curtains light up like a live circuit board: bright
 * vertical rails pulse on the beat while data packets race along Manhattan
 * paths — long vertical runs broken by sharp right-angle jogs to a neighboring
 * rail, leaving glowing traces. The whole board breathes between emerald and
 * electric blue, crossing through cyan, and every kick pulses the field.
 * Modeled on the holotrigger green/blue circuit program.
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
public class Motherboard extends StrandPattern {

  private static final int MAX_PACKETS = 24;
  private static final int N_BARS = 10;
  private static final double HUE_A = 125; // emerald
  private static final double HUE_B = 220; // electric blue
  private static final double JOG_INTERVAL_MS = 450;

  public final DiscreteParameter traces =
    new DiscreteParameter("Traces", 12, 4, MAX_PACKETS)
    .setDescription("Data packets racing the board");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of the pulse");

  private final class Surface {
    boolean inited;
    // packets
    final double[] px = new double[MAX_PACKETS];
    final double[] py = new double[MAX_PACKETS];
    final double[] pv = new double[MAX_PACKETS]; // signed: up or down
    final double[] jogT = new double[MAX_PACKETS];
    // last jog: horizontal bridge glow
    final int[] jx0 = new int[MAX_PACKETS];
    final int[] jx1 = new int[MAX_PACKETS];
    final int[] jy = new int[MAX_PACKETS];
    final double[] jenv = new double[MAX_PACKETS];
    // rails
    final int[] barX = new int[N_BARS];
    void reset(int w, int h) {
      for (int i = 0; i < MAX_PACKETS; ++i) {
        this.px[i] = Math.random() * w;
        this.py[i] = Math.random() * h;
        this.pv[i] = (0.010 + Math.random() * 0.012) * (Math.random() < 0.5 ? 1 : -1);
        this.jogT[i] = Math.random() * JOG_INTERVAL_MS;
        this.jenv[i] = 0;
      }
      rollBars(this, w);
      this.inited = true;
    }
  }

  private static void rollBars(Surface s, int w) {
    for (int i = 0; i < N_BARS; ++i) {
      s.barX[i] = (int) (Math.random() * w);
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();
  private double pulse = 0;
  private double blueFlash = 0; // brief blue excursions from the green-locked board
  private int beatCount = 0;

  public Motherboard(LX lx) {
    // Base registers color (hue shift), speed (packet rate), size (trace glow).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("traces", this.traces);
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
    if (this.beat) {
      this.pulse = 1;
      if (this.beatLevel > 0.55) {
        this.blueFlash = 1; // hard hits flash the board blue for a beat
      }
      if ((++this.beatCount & 3) == 0) {
        // every 4th beat the rails re-route
        if (this.cube.inited) rollBars(this.cube, Apotheneum.cube.exterior.width());
        if (this.cylinder.inited) rollBars(this.cylinder, Apotheneum.cylinder.exterior.width());
      }
    }
    this.pulse *= Math.exp(-deltaMs / 190.0);
    this.blueFlash *= Math.exp(-deltaMs / 260.0);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    final int w = o.width();
    final int h = o.height();
    if (!s.inited) s.reset(w, h);

    final double wow = getWow();
    final double speed = Math.max(0.05, getSpeed());
    final double hueShift = LXColor.h(getColor());
    // green-locked board with brief blue excursions: a short blue phase every
    // ~8s, plus beat-flash blues — asymmetric, never a smooth 50/50 oscillation
    final double phase = (this.timeMs % 8000.0) / 8000.0;
    double mix = (phase > 0.80) ? Math.sin((phase - 0.80) / 0.20 * Math.PI) : 0;
    mix = Math.max(mix, this.blueFlash);
    final double hue = LXUtils.lerp(HUE_A, HUE_B, mix) + hueShift;
    final int col = LXColor.hsb((float) (((hue % 360) + 360) % 360), 92, 100);
    final int hot = LXColor.lerp(col, LXColor.WHITE, 0.5f);
    final double gain = (1.0 + this.pulse * (0.4 + wow * 0.4)); // kick pulse
    final int tq = (int) (this.timeMs / 90);

    // vertical rails
    for (int i = 0; i < N_BARS; ++i) {
      final int bx = s.barX[i];
      final double bb = (0.22 + 0.30 * this.levelEnv + this.pulse * 0.35) * gain;
      for (int y = 0; y < h; ++y) {
        double b = bb * drip(y, h) * (0.90 + 0.10 * hashd(bx * 131 + y * 7 + tq));
        b += bb * pool(y, h);
        addPix(o, bx, y, col, b);
      }
    }

    // data packets on Manhattan paths
    final int count = Math.min(MAX_PACKETS, this.traces.getValuei() + (int) Math.round(wow * 6));
    final double vScale = speed * (1 + wow * 0.8);
    final double glowR = 1 + getSize(); // trace thickness
    for (int i = 0; i < count; ++i) {
      s.py[i] += s.pv[i] * vScale * deltaMs;
      if (s.py[i] < -2) { s.py[i] = h + 1; }
      else if (s.py[i] > h + 2) { s.py[i] = -1; }

      s.jogT[i] -= deltaMs;
      if (s.jogT[i] <= 0) {
        s.jogT[i] = JOG_INTERVAL_MS * (0.5 + Math.random()) / (1 + wow);
        if (Math.random() < 0.55) {
          // right-angle jog to a neighboring rail
          int dx = (2 + (int) (Math.random() * 5)) * (Math.random() < 0.5 ? 1 : -1);
          s.jx0[i] = (int) s.px[i];
          s.jx1[i] = (int) s.px[i] + dx;
          s.jy[i] = (int) s.py[i];
          s.jenv[i] = 1;
          s.px[i] = ((s.px[i] + dx) % w + w) % w;
        }
      }

      // packet head + short vertical trace
      final int dir = (s.pv[i] > 0) ? 1 : -1;
      for (int k = 0; k <= 5; ++k) {
        double b = gain * Math.exp(-0.55 * k);
        for (int r = 0; r < (int) glowR; ++r) {
          addPix(o, (int) s.px[i] + r, (int) s.py[i] - k * dir, (k == 0) ? hot : col, b / (1 + r));
        }
      }
      // glowing horizontal bridge from the last jog
      if (s.jenv[i] > 0.02) {
        int lo = Math.min(s.jx0[i], s.jx1[i]);
        int hi2 = Math.max(s.jx0[i], s.jx1[i]);
        for (int x = lo; x <= hi2; ++x) {
          addPix(o, x, s.jy[i], col, s.jenv[i] * gain * 0.8);
        }
        s.jenv[i] *= Math.exp(-deltaMs / 240.0);
      }
    }
  }
}
