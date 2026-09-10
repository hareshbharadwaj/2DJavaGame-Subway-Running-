package com.javadash.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

import static com.javadash.game.GameConstants.*;

public final class Assets {

    // Every drawable is exposed as a TextureRegion that has already been flipped
    // for the Y-down camera (see safeLoadRegion). The raw Texture handles are kept
    // only so they can be disposed.
    public static TextureRegion playerRun1;
    public static TextureRegion playerRun2;
    public static TextureRegion playerLeft;
    public static TextureRegion playerRight;
    public static TextureRegion playerJump;
    public static TextureRegion playerSlide;

    public static TextureRegion roadImage;
    public static TextureRegion barricadeLeft;
    public static TextureRegion barricadeRight;

    public static final Map<String, TextureRegion> sideLeft = new HashMap<>();
    public static final Map<String, TextureRegion> sideRight = new HashMap<>();

    public static TextureRegion[] obstacleRegions = new TextureRegion[OBSTACLE_TYPE_COUNT];
    public static TextureRegion[] coinRegions = new TextureRegion[8];

    public static TextureRegion heartImage;
    public static TextureRegion shieldImage;
    public static TextureRegion magnetImage;
    public static TextureRegion boostImage;
    public static TextureRegion doubleScoreImage;
    public static TextureRegion jetpackImage;
    public static TextureRegion slowMoImage;
    public static TextureRegion logoImage;

    /** Everything loaded, tracked purely so dispose() can release it. */
    private static final java.util.List<Texture> owned = new java.util.ArrayList<>();

    // Solid 1x1 white texture for drawing colored boxes, progress bars, and overlays
    public static Texture pixel;

    // Default fonts
    public static BitmapFont fontSmall;
    public static BitmapFont fontMedium;
    public static BitmapFont fontLarge;

    private Assets() {}

    private static FileHandle resolve(String relativePath) {
        FileHandle fh = Gdx.files.internal(relativePath);
        if (fh.exists()) return fh;
        fh = Gdx.files.internal("assets/" + relativePath);
        if (fh.exists()) return fh;
        return Gdx.files.internal(relativePath);
    }

    public static Texture safeLoad(String path) {
        try {
            FileHandle fh = resolve(path);
            if (fh.exists()) {
                Texture t = new Texture(fh);
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                owned.add(t);
                return t;
            }
        } catch (Exception e) {
            Gdx.app.error("Assets", "Failed loading " + path, e);
        }
        return null;
    }

    /**
     * Load a texture as a region that is already flipped for the Y-down camera.
     *
     * The camera uses {@code setToOrtho(true, ...)} so that all the physics and
     * layout constants carried over from the Swing version keep working (y grows
     * downward, gravity pulls toward GROUND_Y). The side effect is that raw
     * textures, which libGDX stores bottom-up, come out upside down. Flipping the
     * region once here fixes every sprite at the point of loading, instead of
     * making each of the ~35 draw calls compensate for it.
     */
    public static TextureRegion safeLoadRegion(String path) {
        Texture t = safeLoad(path);
        if (t == null) return null;
        TextureRegion r = new TextureRegion(t);
        r.flip(false, true);
        return r;
    }

    /** Flip an already-loaded texture into a Y-down-correct region. */
    public static TextureRegion flipped(Texture t) {
        if (t == null) return null;
        TextureRegion r = new TextureRegion(t);
        r.flip(false, true);
        return r;
    }

    public static void loadAll() {
        // Create 1x1 white pixel texture for UI shapes/bars
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixel = new Texture(px);
        px.dispose();

        // BitmapFonts
        // "true" builds the font flipped, which is what a Y-down camera needs.
        fontSmall = new BitmapFont(true);
        fontSmall.getData().setScale(0.85f);
        fontMedium = new BitmapFont(true);
        fontMedium.getData().setScale(1.15f);
        fontLarge = new BitmapFont(true);
        fontLarge.getData().setScale(1.6f);

        // Player sprites
        playerRun1 = safeLoadRegion("player/player_run (2).png");
        playerRun2 = safeLoadRegion("player/player_run (1)_processed.png");
        playerLeft = safeLoadRegion("player/player_left (1).png");
        playerRight = safeLoadRegion("player/player_right (1).png");
        playerJump = safeLoadRegion("player/player_jump (1).png");
        playerSlide = safeLoadRegion("player/player_slide.png");

        // Environment
        roadImage = safeLoadRegion("environment/road (1).png");
        barricadeLeft = safeLoadRegion("environment/barricade_raw left.png");
        barricadeRight = safeLoadRegion("environment/barricade_raw right.png");

        String[] envNames = {"city", "forest", "desert", "night"};
        for (String env : envNames) {
            TextureRegion l = safeLoadRegion("environment/" + env + "_side_raw left.png");
            TextureRegion r = safeLoadRegion("environment/" + env + "_side_raw right.png");
            if (l != null) sideLeft.put(env, l);
            if (r != null) sideRight.put(env, r);
        }

        // Obstacles
        String[] obsNames = {"barrel (1)", "barrier (1)", "car_blue (1)", "car_green", "car_red (1)",
                             "car_taxi (1)", "cone (1)", "pothole", "truck_long",
                             "bike_oncoming", "auto_oncoming", "car_oncoming"};
        for (int i = 0; i < obsNames.length; i++) {
            obstacleRegions[i] = safeLoadRegion("obstacles/" + obsNames[i] + ".png");
            if (obstacleRegions[i] == null) {
                obstacleRegions[i] = safeLoadRegion("obstacles/barrier (1).png");
            }
        }

        // Coins (8 animation frames)
        for (int i = 0; i < 8; i++) {
            coinRegions[i] = safeLoadRegion("collectibles/coin" + (i + 1) + ".png");
        }

        // UI
        heartImage = safeLoadRegion("ui/heart (1).png");
        shieldImage = safeLoadRegion("ui/shield (1).png");
        magnetImage = safeLoadRegion("ui/magnet (1).png");
        boostImage = safeLoadRegion("ui/boost.png");
        doubleScoreImage = safeLoadRegion("ui/x2.png");
        jetpackImage = safeLoadRegion("ui/jetpack.png");
        slowMoImage = safeLoadRegion("ui/slowmo.png");
        logoImage = safeLoadRegion("ui/logo.png");
    }

    public static void dispose() {
        if (pixel != null) pixel.dispose();
        if (fontSmall != null) fontSmall.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontLarge != null) fontLarge.dispose();
        for (Texture t : owned) {
            if (t != null) t.dispose();
        }
        owned.clear();
    }
}
