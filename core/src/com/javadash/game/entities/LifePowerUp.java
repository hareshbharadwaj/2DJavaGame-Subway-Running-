package com.javadash.game.entities;

import com.badlogic.gdx.math.Rectangle;
import static com.javadash.game.GameConstants.*;

public class LifePowerUp {

    private final double x;
    private double y;
    private final int lane;
    private final double size = 22.0;

    private final Rectangle hitbox = new Rectangle();

    public LifePowerUp(double startX, double startY, int lane) {
        this.x = startX;
        this.y = startY;
        this.lane = lane;
    }

    public void update(double deltaTime, double speed) {
        y += speed * deltaTime;
    }

    public boolean isOffScreen() {
        return y > ROAD_Y + ROAD_H + 10;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getLane() { return lane; }
    public double getSize() { return size; }

    public Rectangle getHitbox() {
        hitbox.set((float)(x + 1.0), (float)(y + 1.0), (float)(size - 2.0), (float)(size - 2.0));
        return hitbox;
    }
}
