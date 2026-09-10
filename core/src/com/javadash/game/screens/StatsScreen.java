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

import java.util.List;

import static com.javadash.game.GameConstants.VIRTUAL_HEIGHT;
import static com.javadash.game.GameConstants.VIRTUAL_WIDTH;

public class StatsScreen implements Screen {

    private final JavaDashGame game;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector3 touchPoint = new Vector3();

    private List<SaveManager.Run> runs;
    private int highScore;
    private int totalCoins;
    private int gamesPlayed;
    private int bestDist;

    public StatsScreen(JavaDashGame game) {
        this.game = game;
    }

    public void refresh() {
        highScore = SaveManager.getHighScore();
        totalCoins = SaveManager.getTotalCoins();
        gamesPlayed = SaveManager.getGamesPlayed();
        bestDist = SaveManager.getBestDistance();
        runs = SaveManager.getRecentRuns();
    }

    @Override
    public void show() {
        refresh();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (pointer != 0) return false;
                touchPoint.set(screenX, screenY, 0);
                game.viewport.unproject(touchPoint);

                float tx = touchPoint.x;
                float ty = touchPoint.y;

                // BACK BUTTON (x: 40 to 200, y: 680 to 730)
                if (tx >= 40 && tx <= 200 && ty >= 680 && ty <= 730) {
                    AudioManager.play("click");
                    game.showMenu();
                    return true;
                }

                // RESET PROGRESS BUTTON (x: 220 to 380, y: 680 to 730)
                if (tx >= 220 && tx <= 380 && ty >= 680 && ty <= 730) {
                    AudioManager.play("click");
                    SaveManager.resetAll();
                    refresh();
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.06f, 0.08f, 0.14f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.camera.combined);
        game.batch.begin();

        // Header Title
        layout.setText(Assets.fontLarge, "PLAYER STATS");
        Assets.fontLarge.setColor(1f, 0.85f, 0.2f, 1f);
        Assets.fontLarge.draw(game.batch, "PLAYER STATS", (VIRTUAL_WIDTH - layout.width) / 2f, 50);

        // Summary Card
        drawRect(40, 75, 340, 100, new Color(0.12f, 0.16f, 0.25f, 0.9f));

        Assets.fontSmall.setColor(Color.LIGHT_GRAY);
        Assets.fontSmall.draw(game.batch, "HIGH SCORE:", 60, 105);
        Assets.fontSmall.draw(game.batch, "TOTAL COINS:", 60, 130);
        Assets.fontSmall.draw(game.batch, "BEST DISTANCE:", 60, 155);

        Assets.fontSmall.setColor(Color.WHITE);
        Assets.fontSmall.draw(game.batch, String.format("%,d", highScore), 220, 105);
        Assets.fontSmall.draw(game.batch, String.format("%,d", totalCoins), 220, 130);
        Assets.fontSmall.draw(game.batch, bestDist + " m", 220, 155);

        // Recent Runs Title
        Assets.fontMedium.setColor(Color.WHITE);
        Assets.fontMedium.draw(game.batch, "RECENT RUNS (LAST 10)", 40, 210);

        // Table Header
        drawRect(40, 225, 340, 26, new Color(0.18f, 0.24f, 0.35f, 1f));
        Assets.fontSmall.setColor(1f, 0.84f, 0.2f, 1f);
        Assets.fontSmall.draw(game.batch, "#", 50, 243);
        Assets.fontSmall.draw(game.batch, "SCORE", 85, 243);
        Assets.fontSmall.draw(game.batch, "COINS", 175, 243);
        Assets.fontSmall.draw(game.batch, "DIST", 250, 243);
        Assets.fontSmall.draw(game.batch, "TIME", 325, 243);

        // Table Rows
        float rowY = 255;
        if (runs != null && !runs.isEmpty()) {
            for (int i = 0; i < runs.size(); i++) {
                SaveManager.Run r = runs.get(i);
                Color rowBg = (i % 2 == 0) ? new Color(0.10f, 0.13f, 0.20f, 0.9f) : new Color(0.08f, 0.10f, 0.16f, 0.9f);
                drawRect(40, rowY, 340, 36, rowBg);

                Assets.fontSmall.setColor(Color.LIGHT_GRAY);
                Assets.fontSmall.draw(game.batch, String.valueOf(i + 1), 50, rowY + 23);
                Assets.fontSmall.setColor(Color.WHITE);
                Assets.fontSmall.draw(game.batch, String.format("%,d", r.score), 85, rowY + 23);
                Assets.fontSmall.setColor(1f, 0.9f, 0.3f, 1f);
                Assets.fontSmall.draw(game.batch, String.valueOf(r.coins), 175, rowY + 23);
                Assets.fontSmall.setColor(0.5f, 0.8f, 1f, 1f);
                Assets.fontSmall.draw(game.batch, r.distance + "m", 250, rowY + 23);
                Assets.fontSmall.setColor(Color.LIGHT_GRAY);
                Assets.fontSmall.draw(game.batch, r.seconds + "s", 325, rowY + 23);

                rowY += 38;
            }
        } else {
            drawRect(40, rowY, 340, 60, new Color(0.10f, 0.13f, 0.20f, 0.9f));
            Assets.fontSmall.setColor(Color.GRAY);
            layout.setText(Assets.fontSmall, "No runs recorded yet.");
            Assets.fontSmall.draw(game.batch, layout, (VIRTUAL_WIDTH - layout.width) / 2f, rowY + 36);
        }

        // Action Buttons
        // BACK BUTTON
        drawRect(40, 680, 160, 48, new Color(0.2f, 0.35f, 0.55f, 1f));
        layout.setText(Assets.fontSmall, "BACK TO MENU");
        Assets.fontSmall.setColor(Color.WHITE);
        Assets.fontSmall.draw(game.batch, layout, 40 + (160 - layout.width) / 2f, 710);

        // RESET PROGRESS BUTTON
        drawRect(220, 680, 160, 48, new Color(0.65f, 0.2f, 0.2f, 1f));
        layout.setText(Assets.fontSmall, "RESET ALL");
        Assets.fontSmall.setColor(Color.WHITE);
        Assets.fontSmall.draw(game.batch, layout, 220 + (160 - layout.width) / 2f, 710);

        game.batch.end();
    }

    private void drawRect(float x, float y, float w, float h, Color c) {
        game.batch.setColor(c);
        game.batch.draw(Assets.pixel, x, y, w, h);
        game.batch.setColor(Color.WHITE);
    }

    @Override public void resize(int width, int height) { game.viewport.update(width, height); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {}
}
