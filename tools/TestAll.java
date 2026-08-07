import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless harness: constructs every piemonte pattern and checks the leading
 * parameter order (Color, Speed, Size, Wow). Run from the repo root:
 *   CP="$(cat /tmp/apoth_cp.txt):target/classes:../Apotheneum/target/classes"
 *   javac -cp "$CP" tools/TestAll.java -d /tmp/testall && java -cp "$CP:/tmp/testall" TestAll
 */
public class TestAll {
  static final String[] PATTERNS = {
    "Afterglow",
    "CandyFlip",
    "ComeUp",
    "Cooked",
    "Cope",
    "Crush",
    "Destination",
    "Diabolical",
    "Digits",
    "Downlink",
    "Drip",
    "FeelSomething",
    "Ghosted",
    "Likes",
    "Liftoff",
    "Origami",
    "Overclock",
    "Overflow",
    "ParticleWave",
    "PlayaStrobe",
    "Pressure",
    "Bruh",
    "Radiate",
    "Replies",
    "ReUp",
    "SpaceBoundSpecies",
    "SpecialKube",
    "Superbloom",
    "TheHumanRace",
    "TunnelVision",
    "UFOAbduction",
    "Whiteout",
    "WYD"
  };

  public static void main(String[] args) throws Exception {
    LX lx = new LX();
    int bad = 0;
    for (String name : PATTERNS) {
      try {
        Class<?> cls = Class.forName("apotheneum.piemonte." + name);
        LXPattern p = (LXPattern) cls.getConstructor(LX.class).newInstance(lx);
        List<String> lead = new ArrayList<>();
        for (LXParameter param : ((LXComponent) p).getParameters()) {
          String label = param.getLabel();
          if (label.equals("Color") || label.equals("Speed")
              || label.equals("Size") || label.equals("Wow")) {
            lead.add(label);
          }
        }
        boolean ok = lead.size() >= 4
          && lead.get(0).equals("Color") && lead.get(1).equals("Speed")
          && lead.get(2).equals("Size") && lead.get(3).equals("Wow");
        if (ok) {
          System.out.println("OK " + name);
        } else {
          System.out.println("BAD-ORDER " + name + " -> " + lead);
          ++bad;
        }
      } catch (Throwable e) {
        System.out.println("FAIL " + name + " -> " + e);
        ++bad;
      }
    }
    System.out.println(bad == 0 ? "ALL " + PATTERNS.length + " CLEAN" : bad + " PROBLEMS");
    System.exit(bad == 0 ? 0 : 1);
  }
}
