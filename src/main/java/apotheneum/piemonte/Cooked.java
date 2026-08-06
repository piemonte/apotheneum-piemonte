/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Cooked — thick horizontal line segments march down the surfaces in a steady
 * rhythm. Each side of the geometry is split into four even quarter-lanes, one
 * segment per lane, so a broken horizontal line descends from top to bottom in
 * unison; when it exits the floor a fresh row re-enters at the top. Turn WOW up
 * and the segments glitch, jitter and strobe cyan-white, pulsating like
 * SpecialKube.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class Cooked extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final int CYAN = LXColor.rgb(0, 255, 255);
  private static final int STROBE_COLOR = LXColor.lerp(LXColor.WHITE, CYAN, 0.30f);
  private static final double BASE_CYCLE_MS = 2600; // full descent period at speed 1
  private static final double ACTIVE = 0.82;        // fraction of the cycle spent descending
  private static final double GAP = 0.16;           // lane-fraction gap between segments

  public final DiscreteParameter segments =
    new DiscreteParameter("Segments", 4, 1, 8)
    .setDescription("Line segments per side (split into even lanes)");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  private static final class Panel {
    final int[][] idx;
    final int w, h;
    Panel(int[][] idx, int w, int h) { this.idx = idx; this.w = w; this.h = h; }
  }

  private Panel[] panels;
  private Target builtTarget;

  private double phase = 0;
  private double timeMs = 0;

  public Cooked(LX lx) {
    // Base registers color, speed (descent rhythm), size (bar thickness), wow (glitch).
    super(lx, 0.4, 0, 1, 0.4, 0, 1);
    addParameter("segments", this.segments);
    addParameter("target", this.target);
  }

  private void buildPanels(Target t) {
    java.util.List<Panel> list = new java.util.ArrayList<>();
    if (t != Target.CYLINDER) {
      for (Apotheneum.Cube.Face face : Apotheneum.cube.exterior.faces) {
        int w = face.columns.length;
        int h = face.columns[0].points.length;
        int[][] g = new int[w][h];
        for (int x = 0; x < w; ++x) {
          for (int y = 0; y < h; ++y) {
            g[x][y] = face.columns[x].points[y].index;
          }
        }
        list.add(new Panel(g, w, h));
      }
    }
    if (t != Target.CUBE) {
      Apotheneum.Orientation o = Apotheneum.cylinder.exterior;
      int q = o.width() / 4;
      int h = o.height();
      for (int seg = 0; seg < 4; ++seg) {
        int[][] g = new int[q][h];
        for (int x = 0; x < q; ++x) {
          for (int y = 0; y < h; ++y) {
            g[x][y] = o.point(seg * q + x, y).index;
          }
        }
        list.add(new Panel(g, q, h));
      }
    }
    this.panels = list.toArray(new Panel[0]);
    this.builtTarget = t;
  }

  private static double frac(double x) {
    return x - Math.floor(x);
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);

    final Target t = this.target.getEnum();
    if (this.panels == null || this.builtTarget != t) {
      buildPanels(t);
    }
    if (this.panels.length == 0) {
      return;
    }

    this.timeMs += deltaMs;
    final double speed = getSpeed();
    final double period = BASE_CYCLE_MS / Math.max(0.03, speed);
    this.phase += deltaMs / period;
    this.phase -= Math.floor(this.phase);

    final int S = this.segments.getValuei();
    final double thick = 1.5 + getSize() * 10;
    final double half = thick * 0.5;
    final double wow = getWow();
    final int base = getColor();

    // SpecialKube-style glitch oscillators (only used when Wow is up)
    final double gt = this.timeMs * (0.02 + wow * 0.04);
    final double strobeHz = 6.0 + speed * 2.0 + wow * 2.0;
    final boolean strobeOn = frac(this.timeMs * 0.001 * strobeHz) < (0.12 + wow * 0.10);

    // soft tail-out through the pause instead of a hard blank
    final double pauseFade = (this.phase >= ACTIVE)
      ? Math.pow(1.0 - (this.phase - ACTIVE) / (1.0 - ACTIVE), 2)
      : 1.0;

    // 65% linear + 35% eased: steady rhythm with added drive
    final double tt = Math.min(1.0, this.phase / ACTIVE);
    final double travel = tt * tt * (3 - 2 * tt) * 0.35 + tt * 0.65; // 0..1 down the surface

    for (int pi = 0; pi < this.panels.length; ++pi) {
      final Panel p = this.panels[pi];
      final double laneW = (double) p.w / S;
      final double headY = -thick + travel * (p.h + 2 * thick);

      for (int k = 0; k < S; ++k) {
        double x0 = k * laneW + GAP * laneW;
        double x1 = (k + 1) * laneW - GAP * laneW;
        double yc = headY;
        int col = base;
        double bright = 1.0;
        double jx = 0, jy = 0;

        if (wow > 0.01) {
          double sd = frac(Math.sin(pi * 12.9898 + k * 78.233) * 43758.5453);
          double glitch = (frac(gt + sd * 10) > 0.5 && frac(gt * 3.7 + sd * 5) > 0.7) ? 1 : 0;
          bright = 1.0 + glitch * 2.4 * wow;
          col = LXColor.lerp(base, CYAN, (float) (glitch * 0.5 * wow));
          if (glitch > 0) {
            double j = wow * 4;
            jx = (Math.random() - 0.5) * j;
            jy = (Math.random() - 0.5) * j * 0.7;
          }
          if (strobeOn) {
            col = LXColor.lerp(col, STROBE_COLOR, (float) wow);
            bright = 1.0 + 2.5 * wow;
            jx += (Math.random() - 0.5) * 3 * wow;
            jy += (Math.random() - 0.5) * 3 * wow;
          }
        }

        bar(p, x0 + jx, x1 + jx, yc + jy, half, bright * pauseFade, col);
        // motion smear: dimmer echo trailing above the descending bar
        bar(p, x0 + jx, x1 + jx, yc + jy - thick * 0.9, half, bright * 0.3 * pauseFade, col);
      }
    }

    copyExterior();
  }

  /** Filled horizontal segment with soft vertical edges, clipped to the panel (no wrap). */
  private void bar(Panel p, double x0, double x1, double yc, double halfThick,
      double bright, int color) {
    int y0 = (int) Math.max(0, Math.floor(yc - halfThick));
    int y1 = (int) Math.min(p.h - 1, Math.ceil(yc + halfThick));
    int xa = (int) Math.max(0, Math.floor(x0));
    int xb = (int) Math.min(p.w - 1, Math.ceil(x1));
    final double denom = halfThick + 0.5;
    for (int yy = y0; yy <= y1; ++yy) {
      double vf = 1.0 - Math.abs(yy - yc) / denom;
      if (vf <= 0) continue;
      double f = LXUtils.clamp(vf * bright, 0, 1);
      if (f <= 0) continue;
      for (int xx = xa; xx <= xb; ++xx) {
        // feathered horizontal end-caps
        double hf = LXUtils.clamp(Math.min(xx - x0, x1 - xx) + 0.5, 0, 1);
        double f2 = f * hf;
        if (f2 <= 0) continue;
        int idx = p.idx[xx][yy];
        this.colors[idx] = LXColor.lightest(this.colors[idx], LXColor.scaleBrightness(color, (float) f2));
      }
    }
  }
}
