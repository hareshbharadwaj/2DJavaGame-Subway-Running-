package com.javadash.game.storage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-platform player progress storage.
 * Uses libGDX Preferences (maps to Android SharedPreferences on Android,
 * and an XML file in ~/.prefs on Desktop).
 *
 * Keeps the exact same keys and structure as LocalStore.java.
 */
public final class SaveManager {

    private static final String PREF_NAME = "javadash_savegame";
    private static final int MAX_RECENT = 10;

    private static Preferences getPrefs() {
        return Gdx.app.getPreferences(PREF_NAME);
    }

    public static int getHighScore()     { return getPrefs().getInteger("highScore", 0); }
    public static int getTotalCoins()    { return getPrefs().getInteger("totalCoins", 0); }
    public static int getGamesPlayed()   { return getPrefs().getInteger("gamesPlayed", 0); }
    public static int getTotalDistance() { return getPrefs().getInteger("totalDistance", 0); }
    public static int getLastScore()     { return getPrefs().getInteger("lastScore", 0); }
    public static int getBestDistance()  { return getPrefs().getInteger("bestDistance", 0); }

    public static String getPlayerName() {
        String n = getPrefs().getString("playerName", "Player").trim();
        return n.isEmpty() ? "Player" : n;
    }

    public static void setPlayerName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            Preferences p = getPrefs();
            p.putString("playerName", name.trim());
            p.flush();
        }
    }

    public static boolean recordRun(int score, int coins, int distance, int durationMillis) {
        Preferences p = getPrefs();
        int games = getGamesPlayed() + 1;
        int best = getHighScore();
        int bestDist = getBestDistance();
        boolean isNewBest = score > best;

        p.putInteger("gamesPlayed", games);
        p.putInteger("totalCoins", getTotalCoins() + coins);
        p.putInteger("totalDistance", getTotalDistance() + distance);
        p.putInteger("lastScore", score);
        if (isNewBest) p.putInteger("highScore", score);
        if (distance > bestDist) p.putInteger("bestDistance", distance);

        // Prepend this run to the recent history, keeping newest MAX_RECENT
        List<String> recent = new ArrayList<>();
        recent.add(score + "|" + coins + "|" + distance + "|" + (durationMillis / 1000));
        for (int i = 0; i < MAX_RECENT - 1; i++) {
            String row = p.getString("recent." + i, null);
            if (row == null) break;
            recent.add(row);
        }
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < recent.size()) {
                p.putString("recent." + i, recent.get(i));
            } else {
                p.remove("recent." + i);
            }
        }

        p.flush();
        return isNewBest;
    }

    public static class Run {
        public final int score;
        public final int coins;
        public final int distance;
        public final int seconds;

        public Run(int score, int coins, int distance, int seconds) {
            this.score = score;
            this.coins = coins;
            this.distance = distance;
            this.seconds = seconds;
        }
    }

    public static List<Run> getRecentRuns() {
        Preferences p = getPrefs();
        List<Run> out = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String row = p.getString("recent." + i, null);
            if (row == null) continue;
            String[] parts = row.split("\\|");
            if (parts.length < 4) continue;
            try {
                out.add(new Run(
                    Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()),
                    Integer.parseInt(parts[3].trim())
                ));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public static void resetAll() {
        Preferences p = getPrefs();
        p.clear();
        p.flush();
    }
}
