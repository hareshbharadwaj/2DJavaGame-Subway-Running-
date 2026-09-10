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

    public static Texture playerRun1;
    public static Texture playerRun2;
    public static Texture playerLeft;
    public static Texture playerRight;
    public static Texture playerJump;
    public static Texture playerSlide;

    public static Texture roadImage;
    public static Texture barricadeLeft;
    public static Texture barricadeRight;

    public static final Map<String, Texture> sideLeft = new HashMap<>();
    public static final Map<String, Texture> sideRight = new HashMap<>();

    public static Texture[] obstacleTextures = new Texture[OBSTACLE_TYPE_COUNT];
    public static TextureRegion[] obstacleRegions = new TextureRegion[OBSTACLE_TYPE_COUNT];

    public static Texture[] coinTextures = new Texture[8];
    public static TextureRegion[] coinRegions = new TextureRegion[8];

    public static Texture heartImage;
    public static Texture shieldImage;
    public static Texture magnetImage;
    public static Texture boostImage;
    public static Texture doubleScoreImage;
    public static Texture jetpackImage;
    public static Texture slowMoImage;
    public static Texture logoImage;

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
                return t;
            }
        } catch (Exception e) {
            Gdx.app.error("Assets", "Failed loading " + path, e);
        }
        return null;
    }

    public static void loadAll() {
        // Create 1x1 white pixel texture for UI shapes/bars
        Pixmap px = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        px.setColor(Color.WHITE);
        px.fill();
        pixel = new Texture(px);
        px.dispose();

        // BitmapFonts
        fontSmall = new BitmapFont();
        fontSmall.getData().setScale(0.85f);
        fontMedium = new BitmapFont();
        fontMedium.getData().setScale(1.15f);
        fontLarge = new BitmapFont();
        fontLarge.getData().setScale(1.6f);

        // Player sprites
        playerRun1 = safeLoad("player/player_run (2).png");
        playerRun2 = safeLoad("player/player_run (1)_processed.png");
        playerLeft = safeLoad("player/player_left (1).png");
        playerRight = safeLoad("player/player_right (1).png");
        playerJump = safeLoad("player/player_jump (1).png");
        playerSlide = safeLoad("player/player_slide.png");

        // Environment
        roadImage = safeLoad("environment/road (1).png");
        barricadeLeft = safeLoad("environment/barricade_raw left.png");
        barricadeRight = safeLoad("environment/barricade_raw right.png");

        String[] envNames = {"city", "forest", "desert", "night"};
        for (String env : envNames) {
            Texture l = safeLoad("environment/" + env + "_side_raw left.png");
            Texture r = safeLoad("environment/" + env + "_side_raw right.png");
            if (l != null) sideLeft.put(env, l);
            if (r != null) sideRight.put(env, r);
        }

        // Obstacles
        String[] obsNames = {"barrel (1)", "barrier (1)", "car_blue (1)", "car_green", "car_red (1)",
                             "car_taxi (1)", "cone (1)", "pothole", "truck_long",
                             "bike_oncoming", "auto_oncoming", "car_oncoming"};
        for (int i = 0; i < obsNames.length; i++) {
            obstacleTextures[i] = safeLoad("obstacles/" + obsNames[i] + ".png");
            if (obstacleTextures[i] == null) {
                obstacleTextures[i] = safeLoad("obstacles/barrier (1).png");
            }
            if (obstacleTextures[i] != null) {
                obstacleRegions[i] = new TextureRegion(obstacleTextures[i]);
            }
        }

        // Coins (8 animation frames)
        for (int i = 0; i < 8; i++) {
            coinTextures[i] = safeLoad("collectibles/coin" + (i + 1) + ".png");
            if (coinTextures[i] != null) {
                coinRegions[i] = new TextureRegion(coinTextures[i]);
            }
        }

        // UI
        heartImage = safeLoad("ui/heart (1).png");
        shieldImage = safeLoad("ui/shield (1).png");
        magnetImage = safeLoad("ui/magnet (1).png");
        boostImage = safeLoad("ui/boost.png");
        doubleScoreImage = safeLoad("ui/x2.png");
        jetpackImage = safeLoad("ui/jetpack.png");
        slowMoImage = safeLoad("ui/slowmo.png");
        logoImage = safeLoad("ui/logo.png");
    }

    public static void dispose() {
        if (pixel != null) pixel.dispose();
        if (fontSmall != null) fontSmall.dispose();
        if (fontMedium != null) fontMedium.dispose();
        if (fontLarge != null) fontLarge.dispose();

        if (playerRun1 != null) playerRun1.dispose();
        if (playerRun2 != null) playerRun2.dispose();
        if (playerLeft != null) playerLeft.dispose();
        if (playerRight != null) playerRight.dispose();
        if (playerJump != null) playerJump.dispose();
        if (playerSlide != null) playerSlide.dispose();

        if (roadImage != null) roadImage.dispose();
        if (barricadeLeft != null) barricadeLeft.dispose();
        if (barricadeRight != null) barricadeRight.dispose();

        for (Texture t : sideLeft.values()) if (t != null) t.dispose();
        for (Texture t : sideRight.values()) if (t != null) t.dispose();

        for (Texture t : obstacleTextures) if (t != null) t.dispose();
        for (Texture t : coinTextures) if (t != null) t.dispose();

        if (heartImage != null) heartImage.dispose();
        if (shieldImage != null) shieldImage.dispose();
        if (magnetImage != null) magnetImage.dispose();
        if (boostImage != null) boostImage.dispose();
        if (doubleScoreImage != null) doubleScoreImage.dispose();
        if (jetpackImage != null) jetpackImage.dispose();
        if (slowMoImage != null) slowMoImage.dispose();
        if (logoImage != null) logoImage.dispose();
    }
}
