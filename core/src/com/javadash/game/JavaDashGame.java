package com.javadash.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.javadash.game.audio.AudioManager;
import com.javadash.game.screens.GameScreen;
import com.javadash.game.screens.MenuScreen;
import com.javadash.game.screens.StatsScreen;

import static com.javadash.game.GameConstants.VIRTUAL_HEIGHT;
import static com.javadash.game.GameConstants.VIRTUAL_WIDTH;

public class JavaDashGame extends Game {

    public SpriteBatch batch;
    public OrthographicCamera camera;
    public FitViewport viewport;

    private MenuScreen menuScreen;
    private GameScreen gameScreen;
    private StatsScreen statsScreen;

    @Override
    public void create() {
        batch = new SpriteBatch();

        camera = new OrthographicCamera();
        // IMPORTANT: setToOrtho(true) sets Y-down convention, matching Swing!
        camera.setToOrtho(true, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply();

        Assets.loadAll();
        AudioManager.loadAll();

        menuScreen = new MenuScreen(this);
        gameScreen = new GameScreen(this);
        statsScreen = new StatsScreen(this);

        showMenu();
    }

    public void showMenu() {
        menuScreen.refresh();
        setScreen(menuScreen);
    }

    public void startGame() {
        gameScreen.resetGame();
        setScreen(gameScreen);
    }

    public void showStats() {
        statsScreen.refresh();
        setScreen(statsScreen);
    }

    @Override
    public void dispose() {
        super.dispose();
        batch.dispose();
        Assets.dispose();
        AudioManager.dispose();
    }
}
