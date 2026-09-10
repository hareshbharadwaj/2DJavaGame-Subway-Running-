import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Local, file-backed player progress. Replaces the old PostgreSQL/JDBC layer.
 *
 * Everything the game needs to remember lives in a single plain-text file,
 * {@code savegame.properties}, next to the jar:
 *
 * <pre>
 *   highScore=12480
 *   totalCoins=317
 *   gamesPlayed=42
 *   totalDistance=9160
 *   lastScore=8100
 *   recent.0=12480|317|9160|2026-09-10T14:03
 *   ...
 * </pre>
 *
 * Design notes:
 * <ul>
 *   <li>Writes are atomic: the data is written to a {@code .tmp} file and then
 *       renamed over the real one, so a crash mid-save cannot corrupt the
 *       existing save. A backup copy is also kept.</li>
 *   <li>Every read is defensive. A missing, empty or hand-edited file degrades
 *       to sensible defaults rather than throwing, so the game always starts.</li>
 *   <li>No login, no network, no external driver: the game is fully offline.</li>
 * </ul>
 */
public final class LocalStore {

    private static final String SAVE_FILE = "savegame.properties";
    private static final String TEMP_FILE = "savegame.properties.tmp";
    private static final String BACKUP_FILE = "savegame.properties.bak";

    /** How many recent runs to keep in the history list. */
    private static final int MAX_RECENT = 10;

    private static final Properties data = new Properties();
    private static boolean loaded = false;

    private LocalStore() {
    }

    // ------------------------------------------------------------------ load

    /** Read the save file into memory. Safe to call repeatedly. */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;

        File f = new File(SAVE_FILE);
        if (!f.exists()) {
            File backup = new File(BACKUP_FILE);
            if (backup.exists()) {
                f = backup; // primary missing: fall back to the backup
            } else {
                return;     // brand new player: defaults are fine
            }
        }

        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            data.load(r);
        } catch (IOException | IllegalArgumentException e) {
            // Corrupt or unreadable save: start fresh rather than crash.
            data.clear();
        }
    }

    // ------------------------------------------------------------------ save

    /** Persist the in-memory values to disk atomically. */
    private static synchronized void flush() {
        File tmp = new File(TEMP_FILE);
        try (FileWriter w = new FileWriter(tmp)) {
            data.store(w, "JavaDash local save - highest score, coins and run history");
        } catch (IOException e) {
            return; // disk full or read-only: keep the old save intact
        }

        File real = new File(SAVE_FILE);
        // Keep the previous save as a backup before replacing it.
        if (real.exists()) {
            File backup = new File(BACKUP_FILE);
            if (backup.exists()) backup.delete();
            real.renameTo(backup);
        }
        if (!tmp.renameTo(real)) {
            tmp.delete();
        }
    }

    // ---------------------------------------------------------------- getters

    private static int getInt(String key) {
        load();
        try {
            return Integer.parseInt(data.getProperty(key, "0").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int getHighScore()     { return getInt("highScore"); }
    public static int getTotalCoins()    { return getInt("totalCoins"); }
    public static int getGamesPlayed()   { return getInt("gamesPlayed"); }
    public static int getTotalDistance() { return getInt("totalDistance"); }
    public static int getLastScore()     { return getInt("lastScore"); }
    public static int getBestDistance()  { return getInt("bestDistance"); }

    /** Player name shown on the menu. Purely cosmetic now that login is gone. */
    public static String getPlayerName() {
        load();
        String n = data.getProperty("playerName", "").trim();
        return n.isEmpty() ? "Player" : n;
    }

    public static synchronized void setPlayerName(String name) {
        load();
        if (name != null && !name.trim().isEmpty()) {
            data.setProperty("playerName", name.trim());
            flush();
        }
    }

    // ---------------------------------------------------------------- recording

    /**
     * Record a finished run and update the lifetime totals.
     *
     * @return {@code true} if this run set a new high score.
     */
    public static synchronized boolean recordRun(int score, int coins, int distance, int durationMillis) {
        load();

        int games = getGamesPlayed() + 1;
        int best = getHighScore();
        int bestDist = getBestDistance();
        boolean isNewBest = score > best;

        data.setProperty("gamesPlayed", String.valueOf(games));
        data.setProperty("totalCoins", String.valueOf(getTotalCoins() + coins));
        data.setProperty("totalDistance", String.valueOf(getTotalDistance() + distance));
        data.setProperty("lastScore", String.valueOf(score));
        if (isNewBest) data.setProperty("highScore", String.valueOf(score));
        if (distance > bestDist) data.setProperty("bestDistance", String.valueOf(distance));

        // Prepend this run to the recent history, keeping the newest MAX_RECENT.
        List<String> recent = new ArrayList<>();
        recent.add(score + "|" + coins + "|" + distance + "|" + (durationMillis / 1000));
        for (int i = 0; i < MAX_RECENT - 1; i++) {
            String row = data.getProperty("recent." + i);
            if (row == null) break;
            recent.add(row);
        }
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < recent.size()) {
                data.setProperty("recent." + i, recent.get(i));
            } else {
                data.remove("recent." + i);
            }
        }

        flush();
        return isNewBest;
    }

    /** One finished run, newest first. */
    public static class Run {
        public final int score;
        public final int coins;
        public final int distance;
        public final int seconds;

        Run(int score, int coins, int distance, int seconds) {
            this.score = score;
            this.coins = coins;
            this.distance = distance;
            this.seconds = seconds;
        }
    }

    /** Recent runs, newest first. Never null; empty for a new player. */
    public static synchronized List<Run> getRecentRuns() {
        load();
        List<Run> out = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String row = data.getProperty("recent." + i);
            if (row == null) continue;
            String[] parts = row.split("\\|");
            if (parts.length < 4) continue;
            try {
                out.add(new Run(Integer.parseInt(parts[0].trim()),
                                Integer.parseInt(parts[1].trim()),
                                Integer.parseInt(parts[2].trim()),
                                Integer.parseInt(parts[3].trim())));
            } catch (NumberFormatException ignored) {
                // skip a malformed row rather than losing the whole history
            }
        }
        return out;
    }

    /** Wipe all saved progress (used by the Reset button). */
    public static synchronized void resetAll() {
        load();
        data.clear();
        flush();
    }
}
