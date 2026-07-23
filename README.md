# apotheneum-piemonte

Apotheneum patterns for Burning Man 2026 — a standalone [Chromatik](https://chromatik.co)
package by Patrick Piemonte, built for the [Apotheneum](https://github.com/Apotheneum/Apotheneum)
installation: two nested LED chambers (a 4-faced cube of 50×45 grids and a 120×43 cylinder,
exterior + interior surfaces, 28,512 nodes).

## Contents

33 patterns under `apotheneum.piemonte`, including:

- **Video-replica series** — Slipstream, Mainframe, HolyWater, Hexed, Overflow, Whiteout,
  Motherboard, Pressure, Biorhythm, Heartthrob (holotrigger / ksawerykomputery references)
- **Originals** — Afterglow, CandyFlip, ComeUp, Cope, Crush, Destination, FeelSomething,
  Handprint, Liftoff, Origami, ParticleWave, PlayaStrobe, Rain, Replies, ReUp,
  SpaceBoundSpecies, SpecialKube, Superbloom, TheHumanRace, Trains, Trip, TunnelVision,
  Vibe
- **Base classes** — `ParameterPattern` (canonical Color / Speed / Size / Wow leading
  controls) and `StrandPattern` (vertical light-strand engine with audio helpers)
- `projects/Apotheneum-VJ.lxp` — a dual-deck VJ project (EXT-A/B + INT-A/B channels,
  additive blending, 22 color-palette swatches, audio enabled)
- `tools/TestAll.java` — headless harness that constructs every pattern and verifies
  the control order

## Prerequisites

- Java 21+
- Maven
- [Chromatik](https://chromatik.co) installed with the Apotheneum fixtures in
  `~/Chromatik/Fixtures/Apotheneum`
- The **Apotheneum core package** installed — this package extends its base classes
  (`ApotheneumPattern`, model/geometry), so both jars must be present at runtime:

```sh
git clone https://github.com/Apotheneum/Apotheneum.git
cd Apotheneum
mvn -Pinstall install   # installs apotheneum-<version>.jar to ~/Chromatik/Packages
                        # and into ~/.m2 (needed to compile this package)
```

## Install

```sh
cd apotheneum-piemonte
mvn -Pinstall install
```

This compiles against the Apotheneum artifact in `~/.m2` and copies
`apotheneum-piemonte-<version>.jar` into `~/Chromatik/Packages`. On next launch,
Chromatik loads every jar in that folder into one classloader — the patterns appear
in the browser under **Apotheneum/piemonte**.

To use the bundled VJ project, copy it somewhere Chromatik opens projects from:

```sh
cp src/main/resources/projects/Apotheneum-VJ.lxp ~/Chromatik/Projects/
```

## The VJ project

- **EXT-A / INT-A** and **EXT-B / INT-B**: two decks, each split into exterior and
  interior views, every channel preloaded with the full pattern set. Channel faders are
  the blend controls (Add blend — overlaps reinforce); B deck starts at 0.
- **Color** channel: opt-in global palette tint (Multiply); bring its fader up to wash
  the whole mix through the current swatch.
- **Palette**: 22 swatches auto-cycling (45s cycle, 5s fade). Click a swatch to lerp to
  it manually; the swatch-cycle trigger is MIDI-mappable.
- **Audio**: enabled by default on the system input device. For direct routing from a
  DJ feed, install a loopback (e.g. `brew install blackhole-2ch`) and select it in the
  AUDIO section.

## Pattern conventions

Every pattern leads with the same four controls: **Color** (palette-linked),
**Speed**, **Size**, **Wow** — Wow is a 0..1 performance macro (0 = off) that each
pattern interprets as its own flourish: glitch, surge, storm, volley. Audio-reactive
patterns add a **Sens** knob and fall back to an idle animation in silence.

## Development

Verify all patterns construct after changes:

```sh
mvn -q dependency:build-classpath -DincludeScope=provided -Dmdep.outputFile=/tmp/apoth_cp.txt
CP="$(cat /tmp/apoth_cp.txt):target/classes:../Apotheneum/target/classes"
javac -cp "$CP" tools/TestAll.java -d /tmp/testall && java -cp "$CP:/tmp/testall" TestAll
```

After pulling upstream Apotheneum changes, reinstall it (`mvn install` in that repo)
and rebuild this package against the fresh artifact — the runtime link across jars
means base-API changes surface here at load time, not compile time.

## Resources

- [Chromatik](https://chromatik.co) — the digital lighting workstation these patterns run on
- [Apotheneum](https://github.com/Apotheneum/Apotheneum) — the installation's core package: model, fixtures, and base classes
- [skills](https://github.com/piemonte/skills) — agent skills for pattern development, including `apotheneum-pattern`
