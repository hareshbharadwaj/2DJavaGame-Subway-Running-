import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Playable characters and their outfit colours.
 *
 * Every character sprite is drawn with a flat blue shirt/tunic. Instead of
 * shipping one PNG per colour, the blue pixels are hue-shifted at load time
 * and the result cached, so a new outfit colour costs nothing on disk.
 *
 * The chosen character and colour are written to characters.dat so they
 * survive a restart, in the same spirit as SessionManager.
 */
public class CharacterManager {

    /** A selectable character: display name plus the sprite it runs with. */
    public static class Character {
        public final String id;
        public final String name;
        public final String spritePath;
        /** Fallback sprites reused for the non-running poses. */
        public final boolean useDefaultPoses;

        Character(String id, String name, String spritePath, boolean useDefaultPoses) {
            this.id = id;
            this.name = name;
            this.spritePath = spritePath;
            this.useDefaultPoses = useDefaultPoses;
        }
    }

    /** A named outfit colour the player can cycle through. */
    public static class Outfit {
        public final String name;
        public final int rgb;

        Outfit(String name, int rgb) {
            this.name = name;
            this.rgb = rgb;
        }
    }

    private static final String PREF_FILE = "characters.dat";

    private static final List<Character> CHARACTERS = new ArrayList<>();
    private static final List<Outfit> OUTFITS = new ArrayList<>();

    static {
        CHARACTERS.add(new Character("runner", "Runner", "assets/player/player_run (2).png", true));
        CHARACTERS.add(new Character("girl", "Dash", "assets/player/char_girl.png", false));
        CHARACTERS.add(new Character("robot", "Bolt", "assets/player/char_robot.png", false));
        CHARACTERS.add(new Character("ninja", "Shadow", "assets/player/char_ninja.png", false));

        OUTFITS.add(new Outfit("Blue", 0x2F9BE0));   // original colour, no shift
        OUTFITS.add(new Outfit("Red", 0xE04A3C));
        OUTFITS.add(new Outfit("Green", 0x3FB05A));
        OUTFITS.add(new Outfit("Purple", 0x9A5CD0));
        OUTFITS.add(new Outfit("Orange", 0xF08A22));
        OUTFITS.add(new Outfit("Pink", 0xF060A0));
    }

    private static int characterIndex = 0;
    private static int outfitIndex = 0;

    /** cache key -> recoloured sprite, so we only tint each combination once */
    private static final Map<String, Image> cache = new HashMap<>();

    static {
        load();
    }

    public static List<Character> characters() { return CHARACTERS; }
    public static List<Outfit> outfits() { return OUTFITS; }

    public static int getCharacterIndex() { return characterIndex; }
    public static int getOutfitIndex() { return outfitIndex; }

    public static Character current() { return CHARACTERS.get(characterIndex); }
    public static Outfit currentOutfit() { return OUTFITS.get(outfitIndex); }

    public static void nextCharacter() {
        characterIndex = (characterIndex + 1) % CHARACTERS.size();
        save();
    }

    public static void prevCharacter() {
        characterIndex = (characterIndex - 1 + CHARACTERS.size()) % CHARACTERS.size();
        save();
    }

    public static void nextOutfit() {
        outfitIndex = (outfitIndex + 1) % OUTFITS.size();
        save();
    }

    public static void prevOutfit() {
        outfitIndex = (outfitIndex - 1 + OUTFITS.size()) % OUTFITS.size();
        save();
    }

    /** The current character's run sprite, tinted to the current outfit. */
    public static Image sprite() {
        return sprite(current().spritePath);
    }

    /** Load {@code path} and tint its blue outfit pixels to the chosen colour. */
    public static Image sprite(String path) {
        String key = path + "#" + outfitIndex;
        Image cached = cache.get(key);
        if (cached != null) return cached;

        BufferedImage src = read(path);
        if (src == null) return null;

        Image out = (outfitIndex == 0) ? src : recolour(src, OUTFITS.get(outfitIndex).rgb);
        cache.put(key, out);
        return out;
    }

    private static BufferedImage read(String path) {
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Replace the character's blue clothing with {@code targetRgb}.
     *
     * Works in HSB: pixels whose hue sits in the blue band and which are
     * reasonably saturated are treated as fabric. Their hue and saturation are
     * replaced by the target colour's while their original brightness is kept,
     * so folds, shading and highlights all survive the swap.
     */
    private static BufferedImage recolour(BufferedImage src, int targetRgb) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        java.awt.Color target = new java.awt.Color(targetRgb);
        float[] t = java.awt.Color.RGBtoHSB(target.getRed(), target.getGreen(), target.getBlue(), null);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24);
                if (a == 0) {
                    out.setRGB(x, y, argb);
                    continue;
                }
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;

                float[] hsb = java.awt.Color.RGBtoHSB(r, g, b, null);
                float hueDeg = hsb[0] * 360f;

                // Blue band, excluding greys (low saturation) and near-black outlines.
                boolean isFabric = hueDeg >= 185f && hueDeg <= 235f && hsb[1] >= 0.35f && hsb[2] >= 0.20f;
                if (isFabric) {
                    int rgb = java.awt.Color.HSBtoRGB(t[0], Math.min(1f, hsb[1] * (t[1] / 0.80f)), hsb[2]);
                    out.setRGB(x, y, (a << 24) | (rgb & 0x00FFFFFF));
                } else {
                    out.setRGB(x, y, argb);
                }
            }
        }
        return out;
    }

    private static void save() {
        try (PrintWriter w = new PrintWriter(PREF_FILE)) {
            w.println(characterIndex);
            w.println(outfitIndex);
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        File f = new File(PREF_FILE);
        if (!f.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            characterIndex = clamp(Integer.parseInt(r.readLine().trim()), CHARACTERS.size());
            outfitIndex = clamp(Integer.parseInt(r.readLine().trim()), OUTFITS.size());
        } catch (Exception ignored) {
        }
    }

    private static int clamp(int v, int size) {
        return (v < 0 || v >= size) ? 0 : v;
    }
}
