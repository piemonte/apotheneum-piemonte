/**
 * Copyright 2026- Patrick Piemonte
 *
 * Created by patrick piemonte
 *
 * Crush — a soft beam with a hot white head descends from the top and settles
 * at the bottom with a white landing flash, then a new beam carrying the next
 * portion of the palette gradient descends and lands on top of it. Band by band
 * the piece fills from the floor upward into a full gradient; once it reaches
 * the top, all the colors begin to cycle.
 *
 * Best viewed in deep playa or in the dust.
 */

package apotheneum.piemonte;

import apotheneum.Apotheneum;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.utils.LXUtils;

@LXCategory("Apotheneum/piemonte")
public class Crush extends ParameterPattern {

  public enum Target {
    BOTH,
    CUBE,
    CYLINDER
  }

  private static final double DESC_RATE = 0.0006;  // normalized descent / ms at speed 1
  private static final double CYCLE_RATE = 0.00006; // gradient scroll / ms at speed 1

  public final DiscreteParameter bands =
    new DiscreteParameter("Bands", 8, 2, 24)
    .setDescription("Number of equal bands that stack up the height");

  public final EnumParameter<Target> target =
    new EnumParameter<Target>("Target", Target.BOTH)
    .setDescription("Which structures to render to");

  private final GradientUtils.ColorStops stops = new GradientUtils.ColorStops();

  // animation state
  private int settled = 0;        // number of settled bands
  private double beamBottom = 1;  // normalized (0 bottom .. 1 top) bottom of descending beam
  private boolean finale = false;
  private double cyclePhase = 0;
  private boolean started = false;
  private int lastN = -1;
  private double flashMs = 0;     // landing-flash envelope, counts down from 250ms

  public Crush(LX lx) {
    // Base registers color, speed, size; speed = beam/cycle rate, size = edge softness.
    // (Band colors come from the project palette gradient, not the Color param.)
    super(lx, 0.4, 0, 1, 0.5, 0, 1);
    addParameter("bands", this.bands);
    addParameter("target", this.target);
  }

  private void reset(int n) {
    this.settled = 0;
    this.finale = false;
    this.cyclePhase = 0;
    this.beamBottom = 1.0 - 1.0 / n;
    this.started = true;
    this.lastN = n;
  }

  private int gradient(double lerp) {
    return this.stops.getColor((float) LXUtils.clamp(lerp, 0, 1), GradientUtils.BlendFunction.HSVM);
  }

  @Override
  protected void render(double deltaMs) {
    setColors(LXColor.BLACK);

    final int n = this.bands.getValuei();
    if (!this.started || n != this.lastN) {
      reset(n);
    }

    final double speed = Math.max(0.02, getSpeed());
    final double bandH = 1.0 / n;
    this.flashMs = Math.max(0, this.flashMs - deltaMs);

    // --- advance the stacking / cycle ---
    if (!this.finale) {
      double targetBottom = (double) this.settled / n;
      this.beamBottom -= DESC_RATE * speed * deltaMs;
      if (this.beamBottom <= targetBottom) {
        this.beamBottom = targetBottom;
        this.settled++;
        this.flashMs = 250;
        if (this.settled >= n) {
          this.finale = true;
          this.cyclePhase = 0;
        } else {
          this.beamBottom = 1.0 - bandH; // next beam starts at the top
        }
      }
    } else {
      this.cyclePhase += CYCLE_RATE * speed * deltaMs;
    }

    // refresh palette gradient (in case the swatch changed)
    int numStops = this.lx.engine.palette.swatch.colors.size();
    this.stops.setPaletteGradient(this.lx.engine.palette, 0, Math.max(1, numStops));

    final double edge = Math.max(0.01, (0.08 + getSize() * 0.35) * bandH);
    final double fillTop = (double) this.settled / n;      // top of the settled stack
    final double beamLerp = (this.settled + 0.5) / n;      // gradient portion the beam carries
    final int beamColor = this.finale ? 0 : gradient(beamLerp);
    final double beamTop = this.beamBottom + bandH;
    final double flashE = this.flashMs / 250.0;

    final Target t = this.target.getEnum();
    if (t != Target.CYLINDER) {
      draw(Apotheneum.cube.exterior, n, edge, fillTop, beamColor, beamTop, bandH, flashE);
    }
    if (t != Target.CUBE) {
      draw(Apotheneum.cylinder.exterior, n, edge, fillTop, beamColor, beamTop, bandH, flashE);
    }
    copyExterior(); // mirror to interior
  }

  private void draw(Apotheneum.Orientation o, int n, double edge,
      double fillTop, int beamColor, double beamTop, double bandH, double flashE) {
    for (Apotheneum.Column column : o.columns()) {
      final LXPoint[] points = column.points;
      final int last = points.length - 1;
      for (int j = 0; j < points.length; ++j) {
        // Y=0 is the top of the column -> invert for a bottom-up fraction.
        final double vB = (last == 0) ? 0.0 : (double) (last - j) / last;
        int c = LXColor.BLACK;

        if (this.finale) {
          c = gradient(frac(vB + this.cyclePhase));
        } else if (vB < fillTop) {
          // settled stack: a smooth gradient over the filled height,
          // flashing white at the top where the last beam just landed
          c = gradient(vB);
          double gd = Math.abs(vB - fillTop);
          if (flashE > 0 && gd < bandH * 0.4) {
            c = LXColor.lerp(c, LXColor.WHITE,
              (float) (flashE * flashE * smoothstep(bandH * 0.4, 0.0, gd)));
          }
        } else if (vB >= this.beamBottom && vB <= beamTop) {
          // descending beam: soft-edged band with a hot white head at its bottom
          double d = Math.min(vB - this.beamBottom, beamTop - vB);
          double bri = LXUtils.clamp(d / edge, 0, 1);
          bri = bri * bri * (3 - 2 * bri);
          double head = smoothstep(edge * 2.0, 0.0, vB - this.beamBottom);
          c = LXColor.lerp(LXColor.scaleBrightness(beamColor, (float) bri),
            LXColor.WHITE, (float) (0.5 * head * bri));
        }
        this.colors[points[j].index] = c;
      }
    }
  }

  private static double frac(double x) {
    return x - Math.floor(x);
  }

  private static double smoothstep(double e0, double e1, double x) {
    double t = (x - e0) / (e1 - e0);
    t = t < 0 ? 0 : (t > 1 ? 1 : t);
    return t * t * (3 - 2 * t);
  }
}
