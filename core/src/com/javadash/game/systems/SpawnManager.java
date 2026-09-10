package com.javadash.game.systems;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.javadash.game.entities.Coin;
import com.javadash.game.entities.LifePowerUp;
import com.javadash.game.entities.Obstacle;
import com.javadash.game.entities.PowerUp;

import java.util.List;
import java.util.Random;

import static com.javadash.game.GameConstants.*;

/**
 * Manages the spawning of obstacles, coins, and power-ups.
 *
 * CRITICAL INVARIANT: The reserved-safe-lane rule guarantees an open route
 * down the road at all times. Reserving a lane avoids collision drift caused
 * by obstacles moving at different speeds.
 */
public class SpawnManager {

    private final Random random = new Random();

    /**
     * The lane currently reserved as an escape route. Nothing ever spawns into
     * it, so there is always a clear path down the road.
     */
    private int safeLane = 1;
    private int lastSpawnedLane = 1;
    private int consecutiveLaneCount = 0;

    public void reset() {
        safeLane = 1;
        lastSpawnedLane = 1;
        consecutiveLaneCount = 0;
    }

    public int getSafeLane() {
        return safeLane;
    }

    /**
     * Rotate the escape lane, but only while the road above the player is
     * clear, so the player is never asked to cross traffic to reach it.
     */
    public void maybeRotateSafeLane(List<Obstacle> obstacles) {
        for (Obstacle o : obstacles) {
            if (o.getY() + o.getHeight() > -50.0 && o.getY() < GROUND_Y - 120.0) {
                return; // traffic still on the road: keep current route
            }
        }
        safeLane = random.nextInt(3);
    }

    /**
     * Spawn obstacles, coins, life power-ups, and ability power-ups.
     * Preserves the exact probabilities, bounds, and safe-lane guarantees.
     */
    public void spawnWorldObjects(List<Obstacle> obstacles,
                                  List<Coin> coins,
                                  List<LifePowerUp> lifePowerUps,
                                  List<PowerUp> powerUps,
                                  int distance,
                                  int lives,
                                  TextureRegion[] obstacleRegions) {
        // Guaranteed escape route and reaction window fairness:
        // 1. Never spawn 3 consecutive obstacles on the same lane
        // 2. Guarantee at least one open lane at all times
        int lane = random.nextInt(3);
        if (lane == lastSpawnedLane) {
            consecutiveLaneCount++;
            if (consecutiveLaneCount >= 2) {
                lane = (lane + 1 + random.nextInt(2)) % 3;
                consecutiveLaneCount = 0;
            }
        } else {
            consecutiveLaneCount = 0;
        }

        // Obstacle height between 34px and 44px
        int obsHeight = 34 + random.nextInt(11);
        double spawnY = ROAD_Y - obsHeight - 12.0;

        // --- Guarantee an escape route ---
        // Never spawn into the reserved safe lane, so a clear path always
        // exists no matter how the speeds work out.
        if (lane == safeLane) {
            lane = (lane + 1 + random.nextInt(2)) % 3;
        }
        if (lane == safeLane) {
            return; // nowhere else to put it; let the road breathe
        }
        lastSpawnedLane = lane;

        // Oncoming traffic appears once the run is under way and ramps up with distance
        int oncomingChance = distance < 150 ? 0 : Math.min(30, 8 + (distance - 150) / 60);
        boolean spawnOncoming = random.nextInt(100) < oncomingChance;

        int obsType;
        if (spawnOncoming) {
            int[] pool = {OBS_BIKE_ONCOMING, OBS_AUTO_ONCOMING, OBS_CAR_ONCOMING};
            obsType = pool[random.nextInt(pool.length)];
        } else {
            obsType = random.nextInt(STATIC_OBSTACLE_COUNT); // 0 to 8
        }

        double w = 44.0;
        double h = obsHeight;
        if (obstacleRegions != null && obsType >= 0 && obsType < obstacleRegions.length && obstacleRegions[obsType] != null) {
            TextureRegion reg = obstacleRegions[obsType];
            int imgW = reg.getRegionWidth();
            int imgH = reg.getRegionHeight();
            if (imgW > 0 && imgH > 0) {
                // Apply a constant scale factor of 0.35 to preserve relative sizes
                double scale = 0.35;
                w = imgW * scale;
                h = imgH * scale;

                // Cap width for lane constraints
                double maxW = (obsType == OBS_BIKE_ONCOMING) ? 40.0 : 70.0;
                if (w > maxW) {
                    scale = maxW / imgW;
                    w = maxW;
                    h = imgH * scale;
                }

                // The articulated lorry is deliberately long: max 300px
                if (obsType == OBS_TRUCK_LONG) {
                    h = Math.min(h, 300.0);
                }
            }
        }

        // Adjust spawn Y so the obstacle rests on the spawn line correctly
        double adjustedSpawnY = ROAD_Y - h - 12.0;

        // Center the obstacle in the lane properly
        double centeredX = LANE_CENTER_X[lane] - (w / 2.0);

        obstacles.add(new Obstacle(centeredX, adjustedSpawnY, lane, w, h, obsType));

        // Coin Generation (55% chance)
        if (random.nextInt(100) < 55) {
            // 30% chance: spawn elevated coin directly above obstacle to reward jumping
            if (random.nextInt(100) < 30) {
                coins.add(new Coin(LANE_CENTER_X[lane] - 9.0, spawnY - 52.0, lane));
            } else {
                // Spawn ground coin in a separate free lane without obstacle overlap
                int coinLane = (lane + 1 + random.nextInt(2)) % 3;
                double coinX = LANE_CENTER_X[coinLane] - 9.0;
                coins.add(new Coin(coinX, spawnY - 10.0, coinLane));
                // 25% chance of a 2-coin sequence
                if (random.nextInt(100) < 25) {
                    coins.add(new Coin(coinX, spawnY - 38.0, coinLane));
                }
            }
        }

        // Life Power-Up Generation (10% chance)
        if (lives < MAX_LIVES && random.nextInt(100) < 10) {
            int lifeLane = (lane + 1 + random.nextInt(2)) % 3;
            double lifeX = LANE_CENTER_X[lifeLane] - 11.0;
            double lifeY = spawnY - 26.0;
            lifePowerUps.add(new LifePowerUp(lifeX, lifeY, lifeLane));
        }

        // Ability Power-Up Generation (10% chance total)
        if (random.nextInt(100) < 10) {
            int puLane = (lane + 1 + random.nextInt(2)) % 3;
            double puX = LANE_CENTER_X[puLane] - 11.0;
            double puY = spawnY - 26.0;
            PowerUp.Type[] types = PowerUp.Type.values();
            PowerUp.Type type = types[random.nextInt(types.length)];
            powerUps.add(new PowerUp(puX, puY, puLane, type));
        }
    }
}
