/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Slipstream — cylinder only. You stand where the performer sat: dotted rays of
 * light pour down the walls all around you, chains of dashes streaming along
 * each ray like particle beams raking outward across a floor. The rays breathe
 * with the music — sparse embers in the quiet, dense comet streams as the level
 * rises — and on a big hit the whole room floods with light while a halo ring
 * blooms at the top rim. The palette drifts slowly through scenes: rose, then
 * emerald, teal flood, blue-white, and finally mixed multicolor embers.
 * Modeled on the ksawerykomputery studio floor piece.
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
public class Slipstream extends ParameterPattern {

  private static final int MAX_RAYS = 90;
  private static final double SCENE_MS = 20000;  // per palette scene
  private static final double SCENE_XFADE = 3000;
  private static final double DUO = -1000;       // red/blue split ring, slowly rotating
  // scene hues; NaN = multicolor embers (per-ray hue)
  private static final double[] SCENES = { 350, 130, 175, 225, DUO, Double.NaN };

  public final DiscreteParameter rays =
    new DiscreteParameter("Rays", 48, 12, MAX_RAYS)
    .setDescription("How many rays stream down the cylinder");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("How easily the music floods the room");

  // per-ray state
  private final double[] rx = new double[MAX_RAYS];      // column position
  private final double[] rphase = new double[MAX_RAYS];  // dash phase offset
  private final double[] rspeed = new double[MAX_RAYS];  // per-ray flow factor
  private final double[] rlen = new double[MAX_RAYS];    // length factor
  private final double[] rswirl = new double[MAX_RAYS];  // x drift per row
  private final double[] rhue = new double[MAX_RAYS];    // multicolor scene hue
  private boolean inited = false;

  // audio (Cope-style)
  private double bassAvg, prevBass, sinceBeatMs = 1e9;
  private double levelEnv = 0;
  private double floodEnv = 0;
  private double floodHold = 0; // flood plateaus bright, then cuts hard
  private double haloEnv = 0;
  private double pulse = 0;
  private double timeMs = 0;
  private double flow = 0;

  public Slipstream(LX lx) {
    // Base registers color (global hue shift), speed (stream rate), size (dash scale).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("rays", this.rays);
    addParameter("sensitivity", this.sensitivity);
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  private void initRays(int w) {
    for (int i = 0; i < MAX_RAYS; ++i) {
      this.rx[i] = hashd(i * 7 + 1) * w;
      this.rphase[i] = hashd(i * 13 + 3);
      this.rspeed[i] = 0.7 + 0.6 * hashd(i * 29 + 5);
      this.rlen[i] = 0.55 + 0.45 * hashd(i * 31 + 11);
      this.rswirl[i] = (hashd(i * 37 + 17) - 0.5) * 0.16;
      this.rhue[i] = hashd(i * 41 + 23) * 360;
    }
    this.inited = true;
  }

  private boolean detect(double deltaMs) {
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
    this.prevBass = bass;
    if (beat) {
      this.sinceBeatMs = 0;
    }
    double level = m.getAveragef(0, nb);
    double alpha = (level > this.levelEnv)
      ? 1 - Math.exp(-deltaMs / 25.0)
      : 1 - Math.exp(-deltaMs / 220.0);
    this.levelEnv += (level - this.levelEnv) * alpha;
    return beat;
  }

  /** Scene hue at the current time, with crossfade; NaN result = multicolor. */
  private double sceneHue(int which) {
    return SCENES[((which % SCENES.length) + SCENES.length) % SCENES.length];
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    this.timeMs += deltaMs;

    final Apotheneum.Orientation o = Apotheneum.cylinder.exterior;
    final int w = o.width();
    final int h = o.height();
    if (!this.inited) {
      initRays(w);
    }

    final double speed = Math.max(0.02, getSpeed());
    final double wow = getWow();
    final boolean beat = detect(deltaMs);
    this.flow += deltaMs * 0.012 * speed;

    // climaxes: strong onsets flood the room + bloom the halo; Wow makes them easier/hotter
    final double bassRatio = LXUtils.clamp((this.prevBass / Math.max(1e-4, this.bassAvg) - 1.0) / 1.5, 0, 1);
    if (beat) {
      this.pulse = 1;
      if (bassRatio > 0.55 - wow * 0.25 || this.levelEnv > 0.5 - wow * 0.2) {
        this.floodEnv = 1;
        this.floodHold = 500; // reference: flood plateaus ~500ms, then a hard cut
        this.haloEnv = 1;
      }
    }
    this.pulse *= Math.exp(-deltaMs / 160.0);
    if (this.floodHold > 0) {
      this.floodHold -= deltaMs;
    } else {
      this.floodEnv *= Math.exp(-deltaMs / 130.0); // sharp extinguish
    }
    this.haloEnv *= Math.exp(-deltaMs / 1200.0); // magenta haze lingers

    // palette scene morph (Color knob rotates the whole journey)
    final double hueShift = LXColor.h(getColor());
    final double sceneT = this.timeMs / SCENE_MS;
    final int scene = (int) Math.floor(sceneT);
    final double intoMs = (sceneT - scene) * SCENE_MS;
    final double xfade = LXUtils.clamp(intoMs / SCENE_XFADE, 0, 1); // 0=prev scene, 1=current
    final double huePrev = sceneHue(scene - 1);
    final double hueCur = sceneHue(scene);

    final double dashPeriod = 2.5 + getSize() * 4.0;
    final double duty = 0.42;
    final int count = Math.min(MAX_RAYS, this.rays.getValuei() + (int) Math.round(wow * 20));
    // rays reach further down the walls as the music swells
    final double reach = h * (0.35 + 0.65 * LXUtils.clamp(this.levelEnv * 1.6 + this.pulse * 0.3, 0, 1));

    for (int i = 0; i < count; ++i) {
      // choose this ray's hue: crossfade scenes; NaN = per-ray multicolor;
      // DUO = red/blue opposition split by a slowly rotating boundary
      final double duoHue = ((this.rx[i] / w + this.timeMs * 0.00008) % 1.0 < 0.5) ? 2 : 220;
      double hue;
      double hp = (huePrev == DUO) ? duoHue : (Double.isNaN(huePrev) ? this.rhue[i] : huePrev);
      double hc = (hueCur == DUO) ? duoHue : (Double.isNaN(hueCur) ? this.rhue[i] : hueCur);
      hue = LXUtils.lerp(hp, hp + wrapDeg(hc - hp), xfade) + hueShift;
      final int col = LXColor.hsb((float) (((hue % 360) + 360) % 360), 82, 100);
      final int hot = LXColor.lerp(col, LXColor.WHITE, 0.6f);

      final double rayLen = reach * this.rlen[i];
      final double ph = this.rphase[i] + this.flow * this.rspeed[i];
      final double bright = (0.55 + 0.45 * this.levelEnv) * (1.0 + this.pulse * 0.5);

      for (int y = 0; y < h; ++y) {
        // dashes travel down the ray; fade out past the ray's reach
        double tailFade = 1.0 - LXUtils.clamp((y - rayLen) / 6.0, 0, 1);
        if (tailFade <= 0) break;
        double v = y / dashPeriod - ph;
        double f = v - Math.floor(v);
        if (f >= duty) continue;
        // dash profile: bright leading edge, feathered back
        double dp = 1.0 - Math.abs(f / duty * 2 - 1);
        double b = bright * tailFade * (0.35 + 0.65 * dp * dp);
        int xx = (int) Math.round(this.rx[i] + this.rswirl[i] * y);
        int c = (dp > 0.8) ? hot : col;
        addPix(o, w, h, xx, y, c, b);
      }
    }

    // flood flash: the whole room saturates on a climax
    if (this.floodEnv > 0.01) {
      double hueF = (Double.isNaN(hueCur) || hueCur == DUO) ? 175 : hueCur;
      final int flood = LXColor.hsb((float) ((((hueF + hueShift) % 360) + 360) % 360), 60, 100);
      final float fb = (float) (this.floodEnv * this.floodEnv * (0.55 + wow * 0.35));
      final int fc = LXColor.scaleBrightness(flood, fb);
      for (int x = 0; x < w; ++x) {
        for (int y = 0; y < h; ++y) {
          final int idx = o.point(x, y).index;
          this.colors[idx] = LXColor.lightest(this.colors[idx], fc);
        }
      }
    }

    // halo ring blooming at the top rim — the lingering magenta haze of the reference
    if (this.haloEnv > 0.01) {
      final int halo = LXColor.hsb((float) ((((290 + hueShift) % 360) + 360) % 360), 55, 100);
      for (int y = 0; y < 5; ++y) {
        double g = this.haloEnv * (0.9 + wow * 0.6) * Math.exp(-y / 1.6);
        for (int x = 0; x < w; ++x) {
          addPix(o, w, h, x, y, halo, g);
        }
      }
    }

    copyCylinderExterior();
  }

  private static double wrapDeg(double d) {
    d = ((d % 360) + 360) % 360;
    return (d > 180) ? d - 360 : d;
  }

  private void addPix(Apotheneum.Orientation o, int w, int h, int x, int y, int color, double b) {
    if (b <= 0.004 || y < 0 || y >= h) return;
    final int xi = ((x % w) + w) % w;
    final int idx = o.point(xi, y).index;
    this.colors[idx] = LXColor.lightest(this.colors[idx],
      LXColor.scaleBrightness(color, (float) LXUtils.clamp(b, 0, 1)));
  }
}
