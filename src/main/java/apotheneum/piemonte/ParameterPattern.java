/**
 * Copyright 2026- Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * Created by patrick piemonte
 *
 * ParameterPattern — an intermediate base (ApotheneumPattern → ParameterPattern
 * → your pattern) that fixes a consistent leading parameter order: color, hue,
 * speed, size, wow. Every pattern that extends it gets those four controls in the same
 * place, and the correct LinkedColorParameter palette init (setMode after
 * addParameter) is centralized here. Subclasses extend this, then add their own
 * parameters (and any extra colors) after super(...). Kept self-contained so
 * last-minute changes don't ripple across other authors' content.
 *
 * "Wow" is a shared performance macro in the spirit of LXStudio-TE's WOW knobs:
 * a 0..1 "extra flourish" each pattern interprets in its own way. It defaults to
 * 0 (inert), so a pattern that ignores it is unaffected and its resting look is
 * unchanged; patterns that read getWow() add a special-effect layer as it rises.
 */

package apotheneum.piemonte;

import apotheneum.ApotheneumPattern;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.CompoundParameter;

public abstract class ParameterPattern extends ApotheneumPattern {

  /** Primary color — palette-linked by default. */
  public final LinkedColorParameter color =
    new LinkedColorParameter("Color")
    .setDescription("Primary color (follows the palette)");

  /** Hue shift (degrees) applied on top of the primary color. */
  public final CompoundParameter hue =
    new CompoundParameter("Hue", 0, 0, 360)
    .setDescription("Hue shift applied to the primary color")
    .setWrappable(true);

  /** Animation speed. Range/polarity set per-subclass via the constructor. */
  public final CompoundParameter speed;

  /** Element size. Range set per-subclass via the constructor. */
  public final CompoundParameter size;

  /** Shared "wow" performance macro (0..1). Defaults to 0 (inert). */
  public final CompoundParameter wow =
    new CompoundParameter("Wow", 0, 0, 1)
    .setDescription("Extra flourish / special-effect intensity");

  /** Defaults: bipolar speed -1..1 (0.4), size 0..1 (0.5). */
  protected ParameterPattern(LX lx) {
    this(lx, 0.4, -1, 1, 0.5, 0, 1);
  }

  /**
   * CompoundParameter ranges are immutable after construction, so subclasses
   * pass their desired speed/size ranges here. Speed becomes bipolar when its
   * minimum is negative.
   */
  protected ParameterPattern(LX lx,
      double speedDef, double speedMin, double speedMax,
      double sizeDef, double sizeMin, double sizeMax) {
    super(lx);

    this.speed = new CompoundParameter("Speed", speedDef, speedMin, speedMax)
      .setDescription("Animation speed");
    if (speedMin < 0) {
      this.speed.setPolarity(CompoundParameter.Polarity.BIPOLAR);
    }
    this.size = new CompoundParameter("Size", sizeDef, sizeMin, sizeMax)
      .setDescription("Element size");

    // Canonical leading order: color, hue, speed, size, wow. Subclasses add the rest.
    addParameter("color", this.color);
    addParameter("hue", this.hue);
    addParameter("speed", this.speed);
    addParameter("size", this.size);
    addParameter("wow", this.wow);

    // setMode after addParameter: the parameter needs its parent/LX set first.
    this.color.setMode(LinkedColorParameter.Mode.PALETTE);
  }

  // Convenience getters. Subclasses can also tweak the
  // inherited params directly, e.g. this.speed.setExponent(2) or
  // this.size.setUnits(LXParameter.Units.INTEGER), in their constructor.

  /**
   * Resolved primary color for this frame (palette- or static-linked), with
   * the Hue knob's shift applied — one dial to steer every pattern's colors.
   */
  protected int getColor() {
    final int c = this.color.calcColor();
    final float shift = this.hue.getValuef();
    if (shift == 0) {
      return c;
    }
    return LXColor.hsb((LXColor.h(c) + shift) % 360, LXColor.s(c), LXColor.b(c));
  }

  /** Current speed value (bipolar when the configured minimum is negative). */
  protected double getSpeed() {
    return this.speed.getValue();
  }

  /** Current size value. */
  protected double getSize() {
    return this.size.getValue();
  }

  /** Current "wow" macro value (0..1); 0 means the flourish is off. */
  protected double getWow() {
    return this.wow.getValue();
  }
}
