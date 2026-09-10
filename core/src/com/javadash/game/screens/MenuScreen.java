package com.javadash.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Vector3;
import com.javadash.game.Assets;
import com.javadash.game.JavaDashGame;
import com.javadash.game.audio.AudioManager;
import com.javadash.game.storage.SaveManager;

import static com.javadash.game.GameConstants.VIRTUAL_HEIGHT;
import static com.javadash.game.GameConstants.VIRTUAL_WIDTH;

public class MenuScreen implements Screen {

    private final JavaDashGame game;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touchPoint = new Vector3();

    private double spinAngle = 0.0;
    private int highScore = 0;
    private int totalCoins = 0;
    private int gamesPlayed = 0;

    public MenuScreen(JavaDashGame game) {
        this.game = game;
    }

    public void refresh() {
        highScore = SaveManager.getHighScore();
        totalCoins = SaveManager.getTotalCoins();
        gamesPlayed = SaveManager.getGamesPlayed();
    }

    @Override
    public void show() {
        refresh();
        AudioManager.startMusic();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (pointer != 0) return false;
                touchPoint.set(screenX, screenY, 0);
                game.viewport.unproject(touchPoint);

                float tx = touchPoint.x;
                float ty = touchPoint.y;

                // PLAY BUTTON (x: 80 to 340, y: 520 to 570)
                if (tx >= 80 && tx <= 340 && ty >= 520 && ty <= 570) {
                    AudioManager.play("click");
                    game.startGame();
                    return true;
                }

                // STATS BUTTON (x: 80 to 340, y: 590 to 640)
                if (tx >= 80 && tx <= 340 && ty >= 590 && ty <= 640) {
                    AudioManager.play("click");
                    game.showStats();
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        spinAngle += delta * 1.5;

        Gdx.gl.glClearColor(0.05f, 0.07f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.camera.combined);
        game.batch.begin();

        // 1. Background / Road preview
        if (Assets.roadImage != null) {
            game.batch.setColor(0.35f, 0.35f, 0.35f, 1f);
            game.batch.draw(Assets.roadImage, 90, 0, 240, VIRTUAL_HEIGHT);
            game.batch.setColor(Color.WHITE);
        }

        // 2. Logo / Title
        if (Assets.logoImage != null) {
            game.batch.draw(Assets.logoImage, (VIRTUAL_WIDTH - 240) / 2f, 60, 240, 110);
        } else {
            layout.setText(Assets.fontLarge, "JAVADASH");
            Assets.fontLarge.setColor(1f, 0.85f, 0.2f, 1f);
            Assets.fontLarge.draw(game.batch, "JAVADASH", (VIRTUAL_WIDTH - layout.width) / 2f, 120);
        }

        // 3. Stats card
        float cardY = 190;
        drawRect(game, 40, cardY, 340, 85, new Color(0.1f, 0.14f, 0.22f, 0.9f));

        Assets.fontSmall.setColor(Color.LIGHT_GRAY);
        Assets.fontSmall.draw(game.batch, "HIGH SCORE", 60, cardY + 30);
        Assets.fontSmall.draw(game.batch, "COINS", 195, cardY + 30);
        Assets.fontSmall.draw(game.batch, "GAMES", 295, cardY + 30);

        Assets.fontMedium.setColor(1f, 0.84f, 0f, 1f);
        Assets.fontMedium.draw(game.batch, String.valueOf(highScore), 60, cardY + 62);
        Assets.fontMedium.setColor(1f, 0.92f, 0.3f, 1f);
        Assets.fontMedium.draw(game.batch, String.valueOf(totalCoins), 195, cardY + 62);
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, String.valueOf(gamesPlayed), 295, cardY + 62);

        // 4. Character turntable preview
        float charX = VIRTUAL_WIDTH / 2f;
        float charY = 380;
        // Shadow ellipse
        drawRect(game, charX - 35, charY + 50, 70, 12, new Color(0f, 0f, 0f, 0.4f));

        float scaleX = (float) Math.cos(spinAngle);
        if (Assets.playerRun1 != null) {
            float pw = 52f * Math.abs(scaleX);
            float ph = 66f;
            game.batch.draw(Assets.playerRun1, charX - pw / 2f, charY - ph / 2f + 20, pw, ph);
        }

        // 5. Buttons
        // PLAY BUTTON
        drawRect(game, 80, 520, 260, 50, new Color(0.18f, 0.65f, 0.35f, 1f));
        layout.setText(Assets.fontMedium, "PLAY GAME");
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, "PLAY GAME", (VIRTUAL_WIDTH - layout.width) / 2f, 552);

        // STATS BUTTON
        drawRect(game, 80, 590, 260, 50, new Color(0.2f, 0.3f, 0.45f, 1f));
        layout.setText(Assets.fontMedium, "MY STATS");
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, "MY STATS", (VIRTUAL_WIDTH - layout.width) / 2f, 622);

        // Controls hint
        Assets.fontSmall.setColor(new Color(0.6f, 0.7f, 0.8f, 0.8f));
        layout.setText(Assets.fontSmall, "Swipe or Arrow Keys / WASD to move");
        Assets.fontSmall.draw(game.batch, layout, (VIRTUAL_WIDTH - layout.width) / 2f, 680);

        game.batch.end();
    }

    private void drawRect(JavaDashGame g, float x, float y, float w, float h, Color c) {
        g.batch.setColor(c);
        g.batch.draw(Assets.pixel, x, y, w, h);
        g.batch.setColor(Color.WHITE);
    }

    @Override public void resize(int width, int height) { game.viewport.update(width, height); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
