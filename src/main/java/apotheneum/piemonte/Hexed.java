/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Hexed — a wrapped terminal raster: big blocky hex digits stream around the
 * surface in electric blue with white-hot cores, re-triggering on the beat.
 * Each string carries a dim magenta rear-ghost offset a few columns behind —
 * the front/back strand parallax of the reference — and characters glitch:
 * columns drop out, jitter, and re-roll. Cyan and red rain flickers along the
 * bottom rows like data falling off the screen. Modeled on the holotrigger
 * glyph-display close-up.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class Hexed extends StrandPattern {

  // 4×6 block font, hex digits 0-F: 6 rows of 4-bit masks (MSB = left column)
  private static final int[][] FONT = {
    {0b0110,0b1001,0b1001,0b1001,0b1001,0b0110}, // 0
    {0b0010,0b0110,0b0010,0b0010,0b0010,0b0111}, // 1
    {0b0110,0b1001,0b0001,0b0010,0b0100,0b1111}, // 2
    {0b1110,0b0001,0b0110,0b0001,0b1001,0b0110}, // 3
    {0b1001,0b1001,0b1111,0b0001,0b0001,0b0001}, // 4
    {0b1111,0b1000,0b1110,0b0001,0b1001,0b0110}, // 5
    {0b0110,0b1000,0b1110,0b1001,0b1001,0b0110}, // 6
    {0b1111,0b0001,0b0010,0b0010,0b0100,0b0100}, // 7
    {0b0110,0b1001,0b0110,0b1001,0b1001,0b0110}, // 8
    {0b0110,0b1001,0b1001,0b0111,0b0001,0b0110}, // 9
    {0b0110,0b1001,0b1001,0b1111,0b1001,0b1001}, // A
    {0b1110,0b1001,0b1110,0b1001,0b1001,0b1110}, // B
    {0b0110,0b1001,0b1000,0b1000,0b1001,0b0110}, // C
    {0b1110,0b1001,0b1001,0b1001,0b1001,0b1110}, // D
    {0b1111,0b1000,0b1110,0b1000,0b1000,0b1111}, // E
    {0b1111,0b1000,0b1110,0b1000,0b1000,0b1000}, // F
  };
  private static final int CHAR_W = 5; // 4 px glyph + 1 px gap
  private static final int MAX_CHARS = 48;
  private static final int MAX_RAIN = 160;
  private static final double RETRIGGER_MS = 1250;

  public final CompoundParameter glitch =
    new CompoundParameter("Glitch", 0.4, 0, 1)
    .setDescription("How corrupted the readout gets");

  public final CompoundParameter sensitivity =
    new CompoundParameter("Sens", 0.5, 0, 1)
    .setDescription("Audio sensitivity of re-triggers");

  private final int[] chars = new int[MAX_CHARS];
  private final int[] accent = new int[MAX_CHARS]; // 0 = blue, 1 = red, 2 = magenta
  private int glitchSeed = 1;
  private int rowY = 14;
  private double retrigger = 0;
  private double flash = 0;
  private double floodP = 0; // periodic red flood pulse (~1.4s in the reference)

  private final class Surface {
    boolean inited;
    final double[] rx = new double[MAX_RAIN];
    final double[] ry = new double[MAX_RAIN];
    final double[] rv = new double[MAX_RAIN];
    final boolean[] rc = new boolean[MAX_RAIN]; // true = cyan, false = red
    final boolean[] alive = new boolean[MAX_RAIN];
    void reset() {
      for (int i = 0; i < MAX_RAIN; ++i) this.alive[i] = false;
      this.inited = true;
    }
  }

  private final Surface cube = new Surface();
  private final Surface cylinder = new Surface();

  public Hexed(LX lx) {
    // Base registers color (hue shift), speed (re-trigger rate), size (row placement drift).
    super(lx, 0.5, 0, 1, 0.5, 0, 1);
    addParameter("glitch", this.glitch);
    addParameter("sensitivity", this.sensitivity);
    addTargetParameter();
    reroll();
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

  private void reroll() {
    for (int i = 0; i < MAX_CHARS; ++i) {
      this.chars[i] = (int) (Math.random() * 16);
      double a = Math.random();
      this.accent[i] = (a < 0.78) ? 0 : (a < 0.9 ? 1 : 2);
    }
  }

  @Override
  protected void advance(double deltaMs) {
    final double speed = Math.max(0.05, getSpeed());
    final double wow = getWow();
    // glyph cells update independently, staggered (~350-500ms each in the reference)
    final double p = deltaMs / (380.0 / (speed * 2)) ;
    for (int i = 0; i < MAX_CHARS; ++i) {
      if (Math.random() < p) {
        this.chars[i] = (int) (Math.random() * 16);
        if (Math.random() < 0.25) {
          double a = Math.random();
          this.accent[i] = (a < 0.78) ? 0 : (a < 0.9 ? 1 : 2);
        }
      }
    }
    // the ~1.4s beat of the scene: red flood pulse + glitch re-seed
    this.retrigger += deltaMs * speed * (1 + wow * 0.8) * 2;
    final boolean due = this.retrigger >= RETRIGGER_MS;
    if (due || (this.beat && this.beatLevel > 0.35)) {
      this.retrigger = 0;
      this.glitchSeed = (int) (Math.random() * 100000) + 1;
      this.flash = 1;
      this.floodP = 1;
      if (Math.random() < 0.3) {
        this.rowY = 8 + (int) (Math.random() * 16);
      }
    }
    this.flash *= Math.exp(-deltaMs / 150.0);
    this.floodP *= Math.exp(-deltaMs / 300.0);
  }

  @Override
  protected void renderStrands(Apotheneum.Orientation o, double deltaMs, boolean isCube) {
    final Surface s = isCube ? this.cube : this.cylinder;
    if (!s.inited) s.reset();
    final int w = o.width();
    final int h = o.height();

    final double wow = getWow();
    final double gl = this.glitch.getValue() + wow * 0.5;
    final double hueShift = LXColor.h(getColor());
    final int blue = LXColor.hsb((float) (((220 + hueShift) % 360)), 95, 100);
    final int blueWhite = LXColor.lerp(blue, LXColor.WHITE, 0.65f); // white digits, blue backing
    final int red = LXColor.hsb((float) (((0 + hueShift) % 360) + 360) % 360, 100, 100);
    final int magenta = LXColor.hsb((float) (((315 + hueShift) % 360)), 90, 100);
    final int cyan = LXColor.hsb((float) (((185 + hueShift) % 360)), 90, 100);

    // glyph row: per-face on the cube (readable), continuous around the cylinder
    final int segW = isCube ? 50 : w;
    final int segs = w / segW;
    final int perSeg = (segW - 2) / CHAR_W;
    final int y0 = LXUtils.clamp(this.rowY + (int) ((getSize() - 0.5) * 10), 1, h - 8);
    final double bright = 0.9 + this.flash * 0.6;

    for (int seg = 0; seg < segs; ++seg) {
      final int xBase = seg * segW + 1;
      for (int ci = 0; ci < perSeg; ++ci) {
        final int gi = (seg * perSeg + ci) % MAX_CHARS;
        final int[] glyph = FONT[this.chars[gi]];
        int c = (this.accent[gi] == 0) ? blueWhite : (this.accent[gi] == 1 ? red : magenta);
        final int hot = LXColor.lerp(c, LXColor.WHITE, 0.55f);
        final int cx = xBase + ci * CHAR_W;
        // per-char glitch: jitter, column drop
        final int jit = (hashd(this.glitchSeed + gi * 31) < gl * 0.45)
          ? ((hashd(this.glitchSeed + gi * 77) < 0.5) ? 1 : -1) : 0;
        final int dropCol = (hashd(this.glitchSeed + gi * 131) < gl * 0.5)
          ? (int) (hashd(this.glitchSeed + gi * 7) * 4) : -1;

        for (int row = 0; row < 6; ++row) {
          final int bits = glyph[row];
          for (int b = 0; b < 4; ++b) {
            if ((bits & (0b1000 >> b)) == 0 || b == dropCol) continue;
            // chromatic dual ghost: red echo one side, cyan echo the other
            addPix(o, cx + b + jit + 3, y0 + row + 1, red, 0.20 * bright);
            addPix(o, cx + b + jit - 2, y0 + row, cyan, 0.18 * bright);
            // front glyph with white-hot center rows
            final int cc = (row >= 2 && row <= 3) ? hot : c;
            addPix(o, cx + b + jit, y0 + row, cc, bright);
          }
        }
      }
    }

    // periodic red flood pulse washing the whole surface (~1.4s scene beat)
    if (this.floodP > 0.02) {
      final int fc = LXColor.scaleBrightness(red, (float) (this.floodP * (0.30 + wow * 0.2)));
      for (int x = 0; x < w; ++x) {
        for (int y = 0; y < h; ++y) {
          final int idx = o.point(x, y).index;
          this.colors[idx] = LXColor.lightest(this.colors[idx], fc);
        }
      }
    }

    // cyan/red data-rain along the bottom rows
    final double spawnRate = w * 0.0006 * deltaMs * (1 + this.levelEnv + wow);
    int toSpawn = (int) spawnRate + ((Math.random() < spawnRate % 1) ? 1 : 0);
    for (int i = 0; i < MAX_RAIN && toSpawn > 0; ++i) {
      if (s.alive[i]) continue;
      s.rx[i] = Math.random() * w;
      s.ry[i] = h - 10 - Math.random() * 4;
      s.rv[i] = 0.035 + Math.random() * 0.03; // fast, motion-blurred in the reference
      s.rc[i] = Math.random() < 0.6;
      s.alive[i] = true;
      --toSpawn;
    }
    for (int i = 0; i < MAX_RAIN; ++i) {
      if (!s.alive[i]) continue;
      s.ry[i] += s.rv[i] * deltaMs;
      if (s.ry[i] >= h + 3) { s.alive[i] = false; continue; }
      comet(o, s.rx[i], s.ry[i], 3.5, s.rc[i] ? cyan : red, 0.8, false);
    }
  }
}
