/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Handprint — a procedurally drawn human handprint (palm, four fingers and a
 * thumb) with fine fingerprint-ridge detail, stamped upright across the surfaces
 * (fingers up, palm out, like a high-five) at random positions and sizes, each
 * fading in and out like a hand pressed to glass and lifted away.
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
public class Handprint extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final int MASK_W = 96;
  private static final int MASK_H = 128;       // portrait: palm base (v=0) -> fingertips (v=1)
  private static final int MAX_STAMPS = 16;
  private static final double SPAWN_MS = 900;  // base interval between new prints
  private static final double LIFE_MS = 5000;  // base print lifetime
  private static final double RIDGE_FREQ = 95; // fingerprint ridge density
  private static final double ASPECT = (double) MASK_W / MASK_H;
  private static final int SPAWN_TRIES = 30;   // attempts to place a non-overlapping print
  // Conservative reference dims (smallest surface: cylinder) so spacing that clears
  // the cylinder also clears the wider/taller cube.
  private static final double REF_W = 120;
  private static final double REF_H = 43;

  public final DiscreteParameter quantity =
    new DiscreteParameter("Quantity", 6, 1, MAX_STAMPS)
    .setDescription("Maximum number of prints visible at once");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  // Procedural hand alpha mask (0..1), built once.
  private float[] mask;

  // Stamp pool
  private final double[] cx = new double[MAX_STAMPS];   // normalized 0..1 (x wraps)
  private final double[] cy = new double[MAX_STAMPS];
  private final double[] scale = new double[MAX_STAMPS]; // print height in grid cells
  private final double[] angle = new double[MAX_STAMPS];
  private final double[] age = new double[MAX_STAMPS];
  private final double[] maxAge = new double[MAX_STAMPS];
  private final boolean[] alive = new boolean[MAX_STAMPS];
  private final boolean[] flip = new boolean[MAX_STAMPS]; // mirror -> left vs right hand
  private double spawnAcc = 0;

  public Handprint(LX lx) {
    // Base registers color, speed, size; speed = spawn/fade rate, size = base print size.
    super(lx, 0.4, 0, 1, 0.5, 0, 1);
    addParameter("quantity", this.quantity);
    addParameter("target", this.target);
  }

  // ---- mask construction -------------------------------------------------

  private static double segDist(double px, double py, double ax, double ay, double bx, double by) {
    double dx = bx - ax, dy = by - ay;
    double len2 = dx * dx + dy * dy;
    double t = (len2 <= 1e-9) ? 0 : ((px - ax) * dx + (py - ay) * dy) / len2;
    t = LXUtils.clamp(t, 0, 1);
    double qx = ax + t * dx, qy = ay + t * dy;
    double ex = px - qx, ey = py - qy;
    return Math.sqrt(ex * ex + ey * ey);
  }

  private void buildMask() {
    this.mask = new float[MASK_W * MASK_H];
    // Finger capsules: {ax,ay,bx,by,radius}, coords in unit square (v up).
    final double[][] fingers = {
      { 0.34, 0.50, 0.32, 0.84, 0.052 }, // index
      { 0.45, 0.52, 0.45, 0.95, 0.052 }, // middle (longest)
      { 0.56, 0.52, 0.58, 0.92, 0.052 }, // ring
      { 0.67, 0.50, 0.71, 0.80, 0.050 }, // pinky (shortest)
    };
    final double[] thumb = { 0.34, 0.34, 0.15, 0.50, 0.060 };
    // Palm: a rounded oval pad (no straight wrist sides). Rounded base at the bottom.
    final double palmCy = 0.34, palmRw = 0.24, palmRh = 0.22;

    for (int yy = 0; yy < MASK_H; ++yy) {
      double v = (double) yy / (MASK_H - 1);
      for (int xx = 0; xx < MASK_W; ++xx) {
        double u = (double) xx / (MASK_W - 1);

        // Signed coverage: nearest distance to any hand part vs its radius.
        double cover = 0;
        double tipDist = 1e9;

        // palm: rounded oval, base rounded (no wrist)
        double pex = (u - 0.5) / palmRw, pey = (v - palmCy) / palmRh;
        double pell = Math.sqrt(pex * pex + pey * pey);
        cover = Math.max(cover, (1.0 - pell) * palmRh);

        for (double[] f : fingers) {
          double d = segDist(u, v, f[0], f[1], f[2], f[3]);
          cover = Math.max(cover, f[4] - d);
          double td = Math.hypot(u - f[2], v - f[3]); // distance to fingertip
          tipDist = Math.min(tipDist, td);
        }
        double td2 = segDist(u, v, thumb[0], thumb[1], thumb[2], thumb[3]);
        cover = Math.max(cover, thumb[4] - td2);
        tipDist = Math.min(tipDist, Math.hypot(u - thumb[2], v - thumb[3]));

        if (cover <= 0) {
          continue; // outside hand
        }
        // soft edge over ~1.5 mask cells
        double edge = LXUtils.clamp(cover / (1.5 / MASK_H), 0, 1);

        // fingerprint ridges: concentric rings out from the nearest fingertip,
        // plus a low palm-crease modulation, kept in [0.55, 1].
        double ridges = 0.5 + 0.5 * Math.sin(tipDist * RIDGE_FREQ);
        double crease = 0.5 + 0.5 * Math.sin((u + v) * 26 + Math.sin(v * 18) * 1.5);
        double detail = 0.40 + 0.52 * Math.pow(ridges, 1.6) + 0.08 * crease;

        this.mask[yy * MASK_W + xx] = (float) Math.pow(edge * LXUtils.clamp(detail, 0, 1), 1.35);
      }
    }
  }

  // ---- stamp lifecycle ---------------------------------------------------

  private static double wrapDistX(double a, double b) {
    double d = Math.abs(a - b);
    return Math.min(d, 1.0 - d);
  }

  /** Try to place a non-overlapping print in slot i; returns false if none fits. */
  private boolean trySpawn(int i, double baseSize) {
    for (int attempt = 0; attempt < SPAWN_TRIES; ++attempt) {
      double s = baseSize * (0.4 + Math.random() * 1.2);
      s = Math.min(s, REF_H); // keep giant prints from clipping into blobs
      double px = Math.random();
      double py = 0.15 + Math.random() * 0.7;
      double rx = 0.5 * s * ASPECT / REF_W; // normalized half-extents (conservative dims)
      double ry = 0.5 * s / REF_H;
      boolean ok = true;
      for (int j = 0; j < MAX_STAMPS; ++j) {
        if (j == i || !this.alive[j]) continue;
        double rxj = 0.5 * this.scale[j] * ASPECT / REF_W;
        double ryj = 0.5 * this.scale[j] / REF_H;
        if (wrapDistX(px, this.cx[j]) < rx + rxj && Math.abs(py - this.cy[j]) < ry + ryj) {
          ok = false;
          break;
        }
      }
      if (!ok) continue;
      this.cx[i] = px;
      this.cy[i] = py;
      this.flip[i] = Math.random() < 0.5;
      this.scale[i] = s;
      this.angle[i] = (Math.random() - 0.5) * Math.toRadians(50); // upright (high-five), tilt +/-25 degrees
      this.age[i] = 0;
      this.maxAge[i] = LIFE_MS * (0.6 + Math.random() * 0.8);
      this.alive[i] = true;
      return true;
    }
    return false; // too crowded right now — wait for prints to fade
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);
    if (this.mask == null) {
      buildMask();
    }

    final double speed = Math.max(0.02, getSpeed());
    final int count = this.quantity.getValuei();
    // base print height: 10..45 cells from Size
    final double baseSize = 10 + getSize() * 35;

    // age + retire
    int aliveCount = 0;
    for (int i = 0; i < MAX_STAMPS; ++i) {
      if (!this.alive[i]) continue;
      this.age[i] += deltaMs * speed;
      if (this.age[i] >= this.maxAge[i]) {
        this.alive[i] = false;
      } else {
        ++aliveCount;
      }
    }
    // spawn toward target count on a timer
    this.spawnAcc += deltaMs * speed;
    if (this.spawnAcc >= SPAWN_MS && aliveCount < count) {
      for (int i = 0; i < MAX_STAMPS; ++i) {
        if (!this.alive[i]) {
          if (trySpawn(i, baseSize)) {
            this.spawnAcc = 0; // only reset on success so we retry next frame
          }
          break;
        }
      }
    }

    final int base = getColor();
    final Target t = this.target.getEnum();
    if (t != Target.CYLINDER) {
      draw(Apotheneum.cube.exterior, count, base);
    }
    if (t != Target.CUBE) {
      draw(Apotheneum.cylinder.exterior, count, base);
    }
    copyExterior(); // mirror to interior
  }

  private void draw(Apotheneum.Orientation o, int count, int base) {
    final int w = o.width();
    final int h = o.height();
    final double aspect = (double) MASK_W / MASK_H; // hand is narrower than tall

    for (int i = 0; i < MAX_STAMPS; ++i) {
      if (!this.alive[i]) continue;
      // fade in (first 20%) -> hold -> fade out (last 35%)
      double a = this.age[i] / this.maxAge[i];
      double fade = LXUtils.clamp(a / 0.2, 0, 1) * Math.pow(LXUtils.clamp((1 - a) / 0.35, 0, 1), 1.6);
      if (fade <= 0) continue;

      final double hCells = this.scale[i];       // print height in cells
      final double wCells = hCells * aspect;     // print width
      final double ccx = this.cx[i] * w;
      final double ccy = this.cy[i] * h;
      final double ca = Math.cos(this.angle[i]);
      final double sa = Math.sin(this.angle[i]);
      final double rad = 0.5 * Math.hypot(wCells, hCells); // bounding radius

      int y0 = (int) Math.max(0, Math.floor(ccy - rad));
      int y1 = (int) Math.min(h - 1, Math.ceil(ccy + rad));
      int x0 = (int) Math.floor(ccx - rad);
      int x1 = (int) Math.ceil(ccx + rad);

      for (int yy = y0; yy <= y1; ++yy) {
        double gy = yy - ccy;
        for (int xx = x0; xx <= x1; ++xx) {
          double gx = xx - ccx;
          // inverse-rotate grid offset into the print's local frame
          double lx = gx * ca + gy * sa;
          double ly = -gx * sa + gy * ca;
          // map to mask UV (v up => invert y)
          double u = lx / wCells + 0.5;
          if (this.flip[i]) u = 1.0 - u; // mirror -> left/right hand
          double vv = 0.5 - ly / hCells;
          if (u < 0 || u >= 1 || vv < 0 || vv >= 1) continue;
          // 4-tap bilinear mask sample for smooth edges at any scale
          double mu = u * (MASK_W - 1), mv = vv * (MASK_H - 1);
          int mx0 = (int) mu, my0 = (int) mv;
          int mx1 = Math.min(MASK_W - 1, mx0 + 1), my1 = Math.min(MASK_H - 1, my0 + 1);
          double fx = mu - mx0, fy = mv - my0;
          float a00 = this.mask[my0 * MASK_W + mx0], a10 = this.mask[my0 * MASK_W + mx1];
          float a01 = this.mask[my1 * MASK_W + mx0], a11 = this.mask[my1 * MASK_W + mx1];
          float alpha = (float) ((a00 * (1 - fx) + a10 * fx) * (1 - fy)
            + (a01 * (1 - fx) + a11 * fx) * fy);
          if (alpha <= 0) continue;
          int xi = ((xx % w) + w) % w;
          int idx = o.point(xi, yy).index;
          this.colors[idx] = LXColor.lightest(this.colors[idx],
            LXColor.scaleBrightness(base, (float) (alpha * fade)));
        }
      }
    }
  }
}
