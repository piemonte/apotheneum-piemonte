# apotheneum-piemonte

Apotheneum patterns for Burning Man 2026 — a standalone [Chromatik](https://chromatik.co)
package by Patrick Piemonte, built for the [Apotheneum](https://github.com/Apotheneum/Apotheneum)
installation: two nested LED chambers (a 4-faced cube of 50×45 grids and a 120×43 cylinder,
exterior + interior surfaces, 28,512 nodes).

## Contents

33 patterns under `apotheneum.piemonte`:

| Pattern | What it looks like |
|---|---|
| **Afterglow** | Joints of light scattered across the surfaces, endlessly exhaling soft white dots outward along rays at arbitrary angles; kicks flare the field, drops send one synchronized wave down every ray |
| **CandyFlip** | Rainbow comets with white-hot heads roam the walls, each bursting into a fresh generation of comets — a cascading firework of color until the energy runs out |
| **ComeUp** | A glass of water filling: a turbulent, foamy waterline rises with sparkles and bubbles beneath, cycling to a new hue with every refill; cube, cylinder, or both |
| **Cooked** | Thick broken lines of light march down the surfaces in unison, four segments per side; Wow makes them glitch, jitter, and strobe cyan-white |
| **Cope** | Thick bars of light on the structure's vertical edges breathe with the music; bass drops erupt into radial shock rings that bloom across the surfaces |
| **Crush** | Segmented beams spiral down like a zoetrope drum and stack into a gradient; once full, new gradients pour down behind a churning surface line, then teardown slits empty the canvas and the build loops — with a strobe that fires only above the surface |
| **Destination** | A blazing point at the center of each face throws flickering radial streaks with per-ray hues; a strobe button pulses cyan-white glitch shockrings outward from the star |
| **Diabolical** | Thick concentric diamond bands spiral out from center in a shifting rainbow, each band whitening along its spine; Wow ripples the field like a flag |
| **Digits** | A terminal readout of white-hot characters (Size scales small ticker to giant wall) over a churning blue log-field — some digits pop in, others slide in from the edges or drift through with a slow-motion center stall |
| **Downlink** | Emerald matrix-rain and red data-bars persist over a room flood that snaps green↔red through amber, above a warm console ring and glittering green floor puddles |
| **Drip** | Standing beaded strands hang rim to floor, scintillating downward and curling into floor hooks; beats advance the scene journey and hard drops leap it to a random scene — green, red, blue, gold, white, blackout |
| **FeelSomething** | Bold glowing rods rain down, bounce and tumble, spraying vivid white-hot sparks that cool to color as they arc |
| **Ghosted** | Dotted red bars with hot amber cores orbit the fixture like slits in a zoetrope drum; Wow steps the spin into glitchy beat-locked jumps |
| **Liftoff** | Fuses climb every vertical string and burst in bright flashes, ember trails lingering; kicks detonate on the beat and drops launch a multi-rocket volley |
| **Likes** | Green circuit traces ignite on bass, pitch, or random hits — right-angle paths drawing themselves in, dashed and twinkling with white-hot junctions, eroding away dot by dot |
| **Origami** | A sheet ruled with drifting diagonal lines folds on the beat (or the button): flaps sweep over with a white crease flash, each fold revealing a new color layer |
| **Overclock** | A dense fixed strand curtain shimmers downward into a bright floor pool while a circuit maze re-routes at the hang line; the room breathes emerald↔azure through near-black |
| **Overflow** | A segmented band of red/green equalizer blocks hangs mid-wall, reconfiguring like letterforms, pulsing with the beat, pooling on the floor, and flooding the room green on drops |
| **ParticleWave** | A rolling swell of shimmering glitter sweeps around the structure through a dark starfield, its ends rocking slowly up and down at each corner; storms bring blue sparkle bursts |
| **PlayaStrobe** | A storm-torn glint band rakes continuously around the room over a faint churning floor; the operator's Strobe button fires pulsating white bursts that decay away |
| **Pressure** | Discrete curtain panels gate hard on/off to the music while the whole wall slowly orbits the vertical axis — green speckled with red over a warm floor ember that never goes out |
| **Radiate** | Dotted rays fan up the walls around you with red rings hugging the base (cylinder by default, cube optional); beats bloom brightness from the center outward, quiet collapses into a twinkling starfield |
| **Rain** | A heavy downpour of vertical streaks; each drop splashes on the floor line and the water arcs back up before falling again |
| **ReUp** | Every string lit solid; bass hits knock random groups dark and they fade back up — an audio curtain that keeps rebuilding itself |
| **Replies** | Glowing dots glide the interior rings and columns; when two collide they burst, and the matching exterior surface flickers in reply — the outer chamber answering the inner |
| **SpaceBoundSpecies** | A flight through a starfield, each face a window with its own vanishing point — drifting twinkles with diffraction spikes at rest, hyperspace warp streaks at speed |
| **SpecialKube** | Spinning wireframe 3D cubes tumble down the panels; the special ones glitch — jittering, flashing cyan-white, and strobing in sync |
| **Superbloom** | Green pixel-chunk buds swell and blossom into giant top-down flowers in wild colors — six species with alternating petals — spinning to the music before folding away |
| **TheHumanRace** | A vast landscape of little glowing stick figures stretching to the horizon, wrapped 360° around you, swaying as a slow camera drifts across the crowd |
| **TunnelVision** | Vertical tunnels of glowing hoops climb the walls ring by ring on every bass hit, hold, then collapse — more and taller tunnels the louder it gets |
| **UFOAbduction** | Chunky green blocks beam upward with long gradient tails — zooming in, passing through the center in slow motion, then bursting out the top — while thin red beams pulse down through them |
| **WYD** | A field of fine flowing waveform lines ripples like silk in wind, crests glowing bright over complementary-tinted shadow, streaming seamlessly around the sculpture |
| **Whiteout** | Wide silver waterfalls: a white-hot emitter band at the top feeds dense scintillating rain that dissolves toward a dark floor, split by thin dark seams |

Also in the package:

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

## Manual Setup Walkthrough

Step-by-step from a fresh machine, no tooling assumed beyond a terminal:

1. **Install Java 21+** — `brew install openjdk@21` (macOS) or your platform's OpenJDK
   package. Verify with `java -version`.
2. **Install Maven** — `brew install maven`. Verify with `mvn -version`.
3. **Install Chromatik** — download from [chromatik.co](https://chromatik.co), run it
   once so it creates `~/Chromatik/` (Fixtures, Packages, Projects folders), then quit.
4. **Install the Apotheneum fixtures** — clone the core repo and copy its fixture files:
   ```sh
   git clone https://github.com/Apotheneum/Apotheneum.git
   cp -r Apotheneum/src/main/resources/fixtures/Apotheneum ~/Chromatik/Fixtures/
   ```
5. **Build + install the core package** (also needed to compile this one):
   ```sh
   cd Apotheneum && mvn -Pinstall install && cd ..
   ```
6. **Build + install this package**:
   ```sh
   git clone https://github.com/piemonte/apotheneum-piemonte.git
   cd apotheneum-piemonte && mvn -Pinstall install
   ```
7. **Copy the VJ project** where Chromatik can open it:
   ```sh
   cp src/main/resources/projects/Apotheneum-VJ.lxp ~/Chromatik/Projects/
   ```
8. **Launch Chromatik** and open `Apotheneum-VJ.lxp` (File > Open Project). You should
   see the Apotheneum model (28,512 points) with channels EXT-A/INT-A (live),
   EXT-B/C + INT-B/C (faders at 0), and utility channels.
9. **Verify patterns loaded** — the browser lists them under **Apotheneum/piemonte**;
   check `~/Chromatik/Logs` for errors if any are missing.
10. **Enable audio** (optional) — AUDIO section in the left pane; the default input is
    the system microphone. For direct routing install a loopback device
    (`brew install blackhole-2ch`), set a Multi-Output Device as system output, and
    pick BlackHole as Chromatik's audio input.
11. **Blend** — bring up any B/C deck fader over the A deck; channel blend is additive.
    Palette section (top left) holds 22 swatches; click one to crossfade the global
    color story.

## Resources

- [Chromatik](https://chromatik.co) — the digital lighting workstation these patterns run on
- [Apotheneum](https://github.com/Apotheneum/Apotheneum) — the installation's core package: model, fixtures, and base classes
- [skills](https://github.com/piemonte/skills) — agent skills for pattern development, including `apotheneum-pattern`
