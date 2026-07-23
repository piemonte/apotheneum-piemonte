/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Heartthrob — the cinematic strand program: the curtains drift through slow,
 * eased color scenes — deep red wash, electric blue with warm lantern orbs
 * glowing among the strands, full-spectrum rainbow columns, golden glitter
 * rain — and finally the room fills with glowing red hearts. Everything
 * reflects in the floor like polished concrete. Big musical moments nudge the
 * scenes along; Wow brings the hearts sooner and thickens the glitter.
 * Modeled on the holotrigger multicolor showcase.
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
public class Heartthrob extends StrandPattern {

  private static final int MIRROR_ROWS = 9;
  private static final int MAX_ORBS = 10;
  private static final int MAX_GLITTER = 240;
  private static final int SCENE_RED = 0, SCENE_BLUE = 1, SCENE_RAINBOW = 2,
    SCENE_GOLD = 3, SCENE_HEARTS = 4, N_SCENES = 5;
  private static final double XFADE_MS = 400; // washes crossfade fast (~0.3-0.4s measured)

  // parametric heart rasterized once: x=16sin³θ, y=13cosθ-5cos2θ-2cos3θ-cos4θ
  private static final int HEART_W = 13, HEART_H = 12;
  private static final boolean[][] HEART = new boolean[HEART_W][HEART_H];
  static {
    for (int t = 0; t < 720; ++t) {
      double th = Math.PI * 2 * t / 720.0;
      double hx = 16 * Math.pow(Math.sin(th), 3);
      double hy = 13 * Math.cos(th) - 5 * Math.cos(2 * th) - 2 * Math.cos(3 * th) - Math.cos(4 * th);
      // scale/flip into the raster; fill by scanning inward
      int px = (int) Math.round((hx + 16) / 32.0 * (HEART_W - 1));
      int py = (int) Math.round((1 - (hy + 17) / 30.0) * (HEART_H - 1));
      if (px >= 0 && px < HEART_W && py >= 0 && py < HEART_H) {
        HEART[px][py] = true;
      }
    }
    // flood-fill rows between outline pixels
    for (int y = 0; y < HEART_H; ++y) {
      int first = -1, last = -1;
      for (int x = 0; x < HEART_W; ++x) {
        if (HEART[x][y]) { if (first < 0) first = x; last = x; }
      }
      for (int x = Math.max(0, first); x >= 0 && x <= last; ++x) {
        HEART[x][y] = true;
      }
    }
  }

  public final CompoundParameter scenes =
    new CompoundParameter("Scenes", 1.2, 0.6, 6.0)
    .setDescription("Seconds each color scene holds");

  public final DiscreteParameter orbs =
    new DiscreteParameter("Orbs", 5, 0, MAX_ORBS)
    .setDescription("Warm lantern orbs among the strands");

  private final class Surface {
    boolean inited;
    final double[] gx = new double[MAX_GLITTER];
    final double[] gy = new double[MAX_GLITTER];
    final double[] gv = new double[MAX_GLITTER];
    final boolean[] alive = new boolean[MAX_GLITTER];
    void reset() {
      for (int i = 0; i < MAX_GLITTER; ++i) this.alive[i] = false;
      this.inited = true;
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();
  private double sceneClock = 0;
  private int scene = SCENE_RED;
  private int prevScene = SCENE_HEARTS;
  private double glitchFlash = 0; // single-frame contrary-color flashes
  private double glitchTimer = 2500;

  public Heartthrob(LX lx) {
    // Base registers color (hue shift), speed (drift tempo), size (heart/orb scale).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("scenes", this.scenes);
    addParameter("orbs", this.orbs);
    addTargetParameter();
  }

  private static double hashd(int n) {
    int h = n * 374761393;
    h = (h ^ (h >>> 13)) * 1103515245;
    h ^= (h >>> 16);
    return (h & 0x7fffffff) / (double) 0x7fffffff;
  }

  @Override
  protected void advance(double deltaMs) {
    final double wow = getWow();
    final double hold = this.scenes.getValue() * 1000 * (1 - wow * 0.3);
    this.sceneClock += deltaMs
      * (1 + (this.beat && this.beatLevel > 0.6 ? 2.5 : 0)); // big hits nudge the timeline
    if (this.sceneClock >= hold + XFADE_MS) {
      this.sceneClock = 0;
      this.prevScene = this.scene;
      // Wow > 0.5: hearts return every other scene
      if (wow > 0.5 && this.scene != SCENE_HEARTS && hashd((int) this.timeMs) < 0.5) {
        this.scene = SCENE_HEARTS;
      } else {
        this.scene = (this.scene + 1) % N_SCENES;
      }
    }
    // occasional single-frame contrary-color flash (the reference's blue glitch mid-red)
    this.glitchTimer -= deltaMs;
    if (this.glitchTimer <= 0) {
      this.glitchTimer = 2200 + hashd((int) this.timeMs) * 2600;
      this.glitchFlash = 1;
    }
    this.glitchFlash *= Math.exp(-deltaMs / 90.0);
  }

  /** Column color for a scene; y-independent (vertical strand identity). */
  private int sceneColor(int scene, int x, double hueShift) {
    switch (scene) {
      case SCENE_RED:
        return LXColor.hsb((float) (((4 + hueShift) % 360)), 100, 100);
      case SCENE_BLUE: {
        double hue = 195 + 30 * hashd(x * 31 + 7);
        return LXColor.hsb((float) (((hue + hueShift) % 360)), 92, 100);
      }
      case SCENE_RAINBOW: {
        double hue = hashd(x * 53 + 11) * 360 + this.timeMs * 0.010;
        return LXColor.hsb((float) (((hue + hueShift) % 360 + 360) % 360), 88, 100);
      }
      case SCENE_GOLD:
        return LXColor.hsb((float) (((42 + hueShift) % 360)), 88, 100);
      default: { // hearts ride over dim blue strands
        double hue = 210 + 20 * hashd(x * 31 + 7);
        return LXColor.hsb((float) (((hue + hueShift) % 360)), 90, 100);
      }
    }
  }

  private double sceneBase(int scene) {
    switch (scene) {
      case SCENE_GOLD: return 0.14;   // dim under the glitter
      case SCENE_HEARTS: return 0.16; // dim under the hearts
      default: return 0.55;
    }
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    if (!s.inited) s.reset();
    final int w = o.width();
    final int h = o.height();
    final double wow = getWow();
    final double hueShift = LXColor.h(getColor());
    final double hold = this.scenes.getValue() * 1000 * (1 - wow * 0.3);
    final double xf = LXUtils.clamp((this.sceneClock - hold) / XFADE_MS, 0, 1);
    final double ease = xf * xf * (3 - 2 * xf);
    // during the hold, current scene is fully in; during xfade we blend to... the
    // clock structure: scene became current at clock 0, fades OUT to next at end.
    // Render = lerp(current, next-preview) — but next isn't chosen yet, so blend
    // from prevScene INTO current at the START instead:
    final double in = LXUtils.clamp(this.sceneClock / XFADE_MS, 0, 1);
    final double easeIn = in * in * (3 - 2 * in);
    final int tq = (int) (this.timeMs / 90);
    final double breathe = 0.92 + 0.08 * Math.sin(this.timeMs * 0.0012 * Math.max(0.05, getSpeed()) * 4);

    // strand columns, crossfading prev scene -> current scene
    for (int x = 0; x < w; ++x) {
      final int cPrev = sceneColor(this.prevScene, x, hueShift);
      final int cCur = sceneColor(this.scene, x, hueShift);
      final int c = LXColor.lerp(cPrev, cCur, (float) easeIn);
      final double base = LXUtils.lerp(sceneBase(this.prevScene), sceneBase(this.scene), easeIn)
        * breathe * (0.90 + 0.10 * hashd(x * 131 + tq * 7)) * (1 + this.beatLevel * 0.25);
      for (int y = 0; y < h - MIRROR_ROWS; ++y) {
        addPix(o, x, y, c, base * drip(y, h));
      }
    }

    final boolean showOrbs = (this.scene == SCENE_BLUE || this.scene == SCENE_HEARTS
      || this.prevScene == SCENE_BLUE);
    if (showOrbs) {
      final int nOrbs = this.orbs.getValuei();
      final int amber = LXColor.hsb(22, 88, 100);
      final double orbR = 2.5 + getSize() * 3;
      for (int i = 0; i < nOrbs; ++i) {
        final double ox = hashd(i * 97 + 13) * w;
        final double oy = 4 + hashd(i * 61 + 29) * (h * 0.35);
        final double pulseB = 0.75 + 0.25 * Math.sin(this.timeMs * 0.012 + i * 1.7); // ~2Hz lantern pulse
        for (int dy = (int) -orbR; dy <= orbR; ++dy) {
          for (int dx = (int) -orbR; dx <= orbR; ++dx) {
            final double d = Math.sqrt(dx * dx + dy * dy) / orbR;
            if (d > 1) continue;
            final double g = Math.exp(-d * d * 3.0) * pulseB;
            final int c = (d < 0.3) ? LXColor.lerp(amber, LXColor.WHITE, 0.4f) : amber;
            addPix(o, (int) ox + dx, (int) (oy + dy), c, g);
          }
        }
      }
    }

    // golden glitter rain
    if (this.scene == SCENE_GOLD || this.prevScene == SCENE_GOLD || wow > 0.6) {
      final int gold = LXColor.hsb(46, 70, 100);
      final double dens = (this.scene == SCENE_GOLD ? 1.0 : 0.4) * (1 + wow);
      final double spawnRate = w * 0.0009 * deltaMs * dens;
      int toSpawn = (int) spawnRate + ((Math.random() < spawnRate % 1) ? 1 : 0);
      for (int i = 0; i < MAX_GLITTER && toSpawn > 0; ++i) {
        if (s.alive[i]) continue;
        s.gx[i] = Math.random() * w;
        s.gy[i] = -Math.random() * 4;
        s.gv[i] = 0.010 + Math.random() * 0.014;
        s.alive[i] = true;
        --toSpawn;
      }
      for (int i = 0; i < MAX_GLITTER; ++i) {
        if (!s.alive[i]) continue;
        s.gy[i] += s.gv[i] * deltaMs;
        if (s.gy[i] >= h - MIRROR_ROWS + 2) { s.alive[i] = false; continue; }
        final double tw = 0.5 + 0.5 * hashd(i * 131 + tq * 7);
        comet(o, s.gx[i], s.gy[i], 2.5, gold, 0.85 * tw, tw > 0.8);
      }
    }

    // the heart finale
    if (this.scene == SCENE_HEARTS) {
      final int red = LXColor.hsb(0, 96, 100);
      final int hot = LXColor.lerp(red, LXColor.WHITE, 0.35f);
      final int nHearts = isCube ? 4 : 4; // one per face / four around the ring
      final double sc = 0.7 + getSize() * 0.8;
      final double throb = 1.0 + 0.10 * Math.sin(this.timeMs * 0.004) + this.beatLevel * 0.15;
      for (int i = 0; i < nHearts; ++i) {
        final int hx = (int) ((i + 0.5) * w / nHearts) - (int) (HEART_W * sc * throb / 2);
        final int hy = (int) (h * 0.28) - HEART_H / 2 + (int) (2 * hashd(i * 7));
        for (int dx = 0; dx < (int) (HEART_W * sc * throb); ++dx) {
          for (int dy = 0; dy < (int) (HEART_H * sc * throb); ++dy) {
            final int sxp = (int) (dx / (sc * throb));
            final int syp = (int) (dy / (sc * throb));
            if (sxp >= HEART_W || syp >= HEART_H || !HEART[sxp][syp]) continue;
            final double edge = (syp < 2) ? 0.85 : 1.0;
            addPix(o, hx + dx, hy + dy, (syp > 3 && syp < 8) ? hot : red, easeIn * edge);
          }
        }
      }
    }

    // single-frame contrary-color glitch flash over everything
    if (this.glitchFlash > 0.1) {
      final int contrary = (this.scene == SCENE_RED)
        ? LXColor.hsb(215, 90, 100) : LXColor.hsb(0, 95, 100);
      final int fc = LXColor.scaleBrightness(contrary, (float) (this.glitchFlash * 0.4));
      for (int x = 0; x < w; ++x) {
        for (int y = 0; y < h - MIRROR_ROWS; ++y) {
          final int idx = o.point(x, y).index;
          this.colors[idx] = LXColor.lightest(this.colors[idx], fc);
        }
      }
    }

    // polished-floor reflection
    mirrorFloor(o, MIRROR_ROWS, 0.5f);
  }
}
