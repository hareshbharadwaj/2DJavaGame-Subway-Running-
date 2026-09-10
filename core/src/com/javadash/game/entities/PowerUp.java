package com.javadash.game.entities;

import com.badlogic.gdx.math.Rectangle;
import static com.javadash.game.GameConstants.*;

public class PowerUp {

    public enum Type {
        SHIELD, MAGNET, BOOST, DOUBLE_SCORE, JETPACK, SLOW_MO
    }

    private final double x;
    private double y;
    private final int lane;
    private final double size = 26.0;
    private final Type type;

    private final Rectangle hitbox = new Rectangle();

    public PowerUp(double startX, double startY, int lane, Type type) {
        this.x = startX;
        this.y = startY;
        this.lane = lane;
        this.type = type;
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
    public Type getType() { return type; }

    public Rectangle getHitbox() {
        hitbox.set((float)x, (float)y, (float)size, (float)size);
        return hitbox;
    }
}
