package com.javadash.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

import static com.javadash.game.GameConstants.VIRTUAL_HEIGHT;
import static com.javadash.game.GameConstants.VIRTUAL_WIDTH;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("JavaDash");
        config.setWindowedMode(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        config.setResizable(true);
        try {
            config.setWindowIcon("ui/logo.png");
        } catch (Exception ignored) {}
        new Lwjgl3Application(new JavaDashGame(), config);
    }
}
