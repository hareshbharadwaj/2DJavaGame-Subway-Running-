package com.javadash.game;

public final class GameConstants {

    public static final int VIRTUAL_WIDTH = 420;
    public static final int VIRTUAL_HEIGHT = 760;

    public static final int ROAD_Y = 24;
    public static final int ROAD_H = 712;

    public static final int SIDE_W = 90;
    public static final int ROAD_LEFT = 90;
    public static final int ROAD_RIGHT = 330;
    public static final int LANE_W = 80;

    public static final int[] LANE_CENTER_X = {
        ROAD_LEFT + LANE_W * 0 + LANE_W / 2, // 130
        ROAD_LEFT + LANE_W * 1 + LANE_W / 2, // 210
        ROAD_LEFT + LANE_W * 2 + LANE_W / 2  // 290
    };

    public static final int PLAYER_WIDTH = 44;
    public static final int PLAYER_HEIGHT = 56;

    /** Exact literal matching Swing (24 + 712 - 92.0 = 644.0). */
    public static final double GROUND_Y = 644.0;

    public static final double GRAVITY = 2200.0;
    public static final double JUMP_STRENGTH = 700.0;
    public static final double SLIDE_DURATION = 0.6;
    public static final double SLIDE_HEIGHT_RATIO = 0.5;
    public static final double JETPACK_HOVER_HEIGHT = 150.0;
    public static final double ABILITY_DURATION = 10.0;

    public static final double INITIAL_SPEED = 300.0;
    public static final double SPEED_CAP = 650.0; // The true speed cap from original game

    public static final int STARTING_LIVES = 1;
    public static final int MAX_LIVES = 3;

    // Obstacle types (0-8 static, 9-11 oncoming traffic)
    public static final int OBS_BARREL = 0;
    public static final int OBS_BARRIER = 1;
    public static final int OBS_CAR_RED = 2;
    public static final int OBS_CAR_BLUE = 3;
    public static final int OBS_CAR_GREEN = 4;
    public static final int OBS_CAR_YELLOW = 5;
    public static final int OBS_CONE = 6;
    public static final int OBS_POTHOLE = 7;
    public static final int OBS_TRUCK_LONG = 8;
    public static final int OBS_BIKE_ONCOMING = 9;
    public static final int OBS_AUTO_ONCOMING = 10;
    public static final int OBS_CAR_ONCOMING = 11;

    public static final int OBSTACLE_TYPE_COUNT = 12;
    public static final int STATIC_OBSTACLE_COUNT = 9;

    public static boolean isOncoming(int type) {
        return type >= OBS_BIKE_ONCOMING && type <= OBS_CAR_ONCOMING;
    }

    public static double oncomingClosingSpeed(int type) {
        switch (type) {
            case OBS_BIKE_ONCOMING: return 220.0;
            case OBS_AUTO_ONCOMING: return 160.0;
            case OBS_CAR_ONCOMING:  return 120.0;
            default: return 0.0;
        }
    }

    private GameConstants() {}
}
