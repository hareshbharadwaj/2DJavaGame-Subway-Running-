package com.javadash.game.input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameInputHandler implements InputProcessor {

    public interface GameActionListener {
        void onMoveLeft();
        void onMoveRight();
        void onJump();
        void onSlide();
        void onTogglePause();
        void onRestart();
        void onToggleMute();
        void onTouchTap(float worldX, float worldY);
    }

    private final Viewport viewport;
    private final GameActionListener listener;
    private final Vector3 touchCoords = new Vector3();

    private float startX = 0f;
    private float startY = 0f;
    private boolean swipedHorizontally = false;
    private boolean swipedVertically = false;
    private boolean isDragging = false;

    private static final float SWIPE_THRESHOLD = 20.0f; // virtual pixels

    public GameInputHandler(Viewport viewport, GameActionListener listener) {
        this.viewport = viewport;
        this.listener = listener;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (pointer != 0) return false;

        touchCoords.set(screenX, screenY, 0);
        viewport.unproject(touchCoords);

        startX = touchCoords.x;
        startY = touchCoords.y;
        swipedHorizontally = false;
        swipedVertically = false;
        isDragging = true;

        listener.onTouchTap(touchCoords.x, touchCoords.y);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer != 0 || !isDragging) return false;

        touchCoords.set(screenX, screenY, 0);
        viewport.unproject(touchCoords);

        float dx = touchCoords.x - startX;
        float dy = touchCoords.y - startY;

        // In Y-down orientation (setToOrtho(true)), up is negative dy and down is positive dy.
        // Trigger jump/slide/lane change on the immediate frame the drag threshold is reached!
        if (!swipedVertically && Math.abs(dy) > Math.abs(dx)) {
            if (dy < -SWIPE_THRESHOLD) {
                listener.onJump();
                swipedVertically = true;
                return true;
            } else if (dy > SWIPE_THRESHOLD) {
                listener.onSlide();
                swipedVertically = true;
                return true;
            }
        }

        if (!swipedHorizontally && Math.abs(dx) > Math.abs(dy)) {
            if (dx < -SWIPE_THRESHOLD) {
                listener.onMoveLeft();
                swipedHorizontally = true;
                return true;
            } else if (dx > SWIPE_THRESHOLD) {
                listener.onMoveRight();
                swipedHorizontally = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == 0) {
            isDragging = false;
            swipedHorizontally = false;
            swipedVertically = false;
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Keys.A:
            case Keys.LEFT:
                listener.onMoveLeft();
                return true;
            case Keys.D:
            case Keys.RIGHT:
                listener.onMoveRight();
                return true;
            case Keys.W:
            case Keys.UP:
            case Keys.SPACE:
                listener.onJump();
                return true;
            case Keys.S:
            case Keys.DOWN:
                listener.onSlide();
                return true;
            case Keys.P:
                listener.onTogglePause();
                return true;
            case Keys.R:
                listener.onRestart();
                return true;
            case Keys.M:
                listener.onToggleMute();
                return true;
        }
        return false;
    }

    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        isDragging = false;
        return true;
    }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
}
