/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * HolyWater — thin waterfalls of light pour from the top rim like strands in a
 * cathedral dome, each a coherent laser-thin stream with a long tail. When a
 * stream reaches the floor it splashes, splaying light outward through the
 * bottom rows in a parabolic pool. The whole scene journeys slowly through
 * four hues — emerald, red, indigo, violet — and a faint warm ring mid-height
 * echoes the balconies of the hall. Beats throw extra splash. Modeled on the
 * holotrigger rotunda installation.
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
public class HolyWater extends StrandPattern {

  private static final int MAX_STREAMS = 24;
  private static final double[] JOURNEY = { 130, 2, 225, 280 }; // emerald, red, indigo, violet
  private static final double HOLD_MS = 11000;
  private static final double XFADE_MS = 3000;
  private static final double SPLASH_MS = 1600; // splash curls linger on the floor

  public final DiscreteParameter streams =
    new DiscreteParameter("Streams", 12, 4, MAX_STREAMS)
    .setDescription("Waterfall streams per surface");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of splash flourishes");

  private final class Surface {
    boolean inited;
    final double[] sx = new double[MAX_STREAMS];
    final double[] sy = new double[MAX_STREAMS];
    final double[] sv = new double[MAX_STREAMS];
    final double[] splash = new double[MAX_STREAMS]; // >0 while splashing (ms left)
    final double[] splashX = new double[MAX_STREAMS];
    void reset(int w) {
      for (int i = 0; i < MAX_STREAMS; ++i) {
        respawn(this, i, w, true);
        this.splash[i] = 0;
      }
      this.inited = true;
    }
  }

  private static void respawn(Surface s, int i, int w, boolean scatter) {
    s.sx[i] = Math.random() * w;
    s.sy[i] = scatter ? Math.random() * 30 : -(Math.random() * 8);
    s.sv[i] = 0.14 + Math.random() * 0.10; // beams drop near-instantly (~0.4-0.6s)
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();
  private double pulse = 0;

  public HolyWater(LX lx) {
    // Base registers color (hue shift), speed (pour rate), size (tail length).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("streams", this.streams);
    addParameter("sensitivity", this.sensitivity);
    addTargetParameter();
  }

  @Override
  protected double beatThreshold() {
    return 1.05 + (1 - this.sensitivity.getValue()) * 0.7;
  }

  @Override
  protected void advance(double deltaMs) {
    if (this.beat) {
      this.pulse = 1;
    }
    this.pulse *= Math.exp(-deltaMs / 220.0);
  }

  /** Journey hue with eased crossfades between held states. */
  private double journeyHue() {
    final double cycle = HOLD_MS + XFADE_MS;
    final double t = this.timeMs / cycle;
    final int leg = (int) Math.floor(t);
    final double into = (t - leg) * cycle;
    final double f = LXUtils.clamp((into - HOLD_MS) / XFADE_MS, 0, 1);
    final double e = f * f * (3 - 2 * f);
    final double a = JOURNEY[((leg % JOURNEY.length) + JOURNEY.length) % JOURNEY.length];
    final double b = JOURNEY[(((leg + 1) % JOURNEY.length) + JOURNEY.length) % JOURNEY.length];
    double d = ((b - a) % 360 + 360) % 360;
    if (d > 180) d -= 360;
    return a + d * e;
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    final int w = o.width();
    final int h = o.height();
    if (!s.inited) s.reset(w);

    final double wow = getWow();
    final double speed = Math.max(0.05, getSpeed());
    final double hueShift = LXColor.h(getColor());
    final double hue = journeyHue() + hueShift;
    final int col = LXColor.hsb((float) (((hue % 360) + 360) % 360), 90, 100);
    // second simultaneous beam color (reference: blue sides, green center)
    final int col2 = LXColor.hsb((float) ((((hue - 100) % 360) + 360) % 360), 90, 100);
    final int scrim = LXColor.hsb((float) (((230 + hueShift) % 360)), 80, 100);

    final int count = Math.min(MAX_STREAMS, this.streams.getValuei() + (int) Math.round(wow * 8));
    final double tail = (16 + getSize() * 20) * (1 + wow * 0.3); // near-full-height columns
    final int floorY = h - 7;

    for (int i = 0; i < count; ++i) {
      final int streamCol = (i % 3 == 2) ? col2 : col; // dual-color beam field
      if (s.splash[i] > 0) {
        // splash: long-lived curling pool splaying wide through the bottom rows
        s.splash[i] -= deltaMs;
        final double f = LXUtils.clamp(s.splash[i] / SPLASH_MS, 0, 1);
        final double grow = 1.0 - f * f; // fast splay, slow drift
        final double spread = grow * (11 + wow * 5 + this.pulse * 4);
        final double wiggle = Math.sin(this.timeMs * 0.004 + i * 2.1) * 1.5; // drifting curl
        for (int dx = (int) -spread; dx <= (int) spread; ++dx) {
          final double u = (spread <= 0) ? 0 : Math.abs(dx) / spread;
          final int yy = floorY + (int) Math.round((1.0 - u * u) * 4 + wiggle * u);
          final double b = (0.25 + 0.75 * f) * (1.0 - u * 0.7) * (1.1 + this.pulse * 0.6);
          addPix(o, (int) s.splashX[i] + dx, Math.min(h - 1, Math.max(floorY, yy)), streamCol, b);
          addPix(o, (int) s.splashX[i] + dx, h - 1, streamCol, b * 0.5); // pool on the floor line
        }
        if (s.splash[i] <= 0) {
          respawn(s, i, w, false);
        }
        continue;
      }

      s.sy[i] += s.sv[i] * speed * 60 * deltaMs * 0.016;
      if (s.sy[i] >= floorY) {
        s.splash[i] = SPLASH_MS;
        s.splashX[i] = s.sx[i];
        continue;
      }
      // laser-thin coherent stream with a long tail
      comet(o, s.sx[i], s.sy[i], tail, streamCol, 0.95 + this.pulse * 0.3, true);
    }

    // persistent horizontal waterline scrim (~48% height, always blue-family)
    final int scrimY = (int) (h * 0.48);
    for (int x = 0; x < w; ++x) {
      addPix(o, x, scrimY, scrim, 0.15);
      addPix(o, x, scrimY + 1, scrim, 0.07);
    }
  }
}
