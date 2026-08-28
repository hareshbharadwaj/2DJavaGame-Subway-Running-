import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DbManager {
    private static Properties props = new Properties();

    static {
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("Could not load db.properties: " + e.getMessage());
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(props.getProperty("db.url"), props.getProperty("db.user"), props.getProperty("db.password"));
    }

    public static boolean initializeTables() {
        String createPlayers = "CREATE TABLE IF NOT EXISTS players (" +
                               "player_id SERIAL PRIMARY KEY, " +
                               "player_name VARCHAR(50) UNIQUE NOT NULL, " +
                               "password VARCHAR(100) NOT NULL, " +
                               "total_coins INT DEFAULT 0, " +
                               "highest_score INT DEFAULT 0, " +
                               "total_distance INT DEFAULT 0, " +
                               "last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        String createLeaderboard = "CREATE TABLE IF NOT EXISTS leaderboard (" +
                                   "player_id INT PRIMARY KEY REFERENCES players(player_id) ON DELETE CASCADE, " +
                                   "high_score INT DEFAULT 0, " +
                                   "updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        
        String createSessions = "CREATE TABLE IF NOT EXISTS game_sessions (" +
                                "session_id SERIAL PRIMARY KEY, " +
                                "player_id INT REFERENCES players(player_id) ON DELETE CASCADE, " +
                                "score INT, " +
                                "coins_collected INT, " +
                                "distance INT, " +
                                "game_duration INT, " +
                                "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayers);
            stmt.execute(createLeaderboard);
            stmt.execute(createSessions);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean test() {
        try (Connection conn = getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    public static PlayerInfo getPlayer(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        String sql = "SELECT player_id, highest_score, total_coins FROM players WHERE player_name = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int bestScore = rs.getInt("highest_score");
                    int bestCoins = rs.getInt("total_coins");
                    
                    int gamesPlayed = 0;
                    String countSql = "SELECT COUNT(*) FROM game_sessions WHERE player_id = ?";
                    try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                        countStmt.setInt(1, playerId);
                        try (ResultSet countRs = countStmt.executeQuery()) {
                            if (countRs.next()) {
                                gamesPlayed = countRs.getInt(1);
                            }
                        }
                    }
                    return new PlayerInfo(username, bestScore, bestCoins, gamesPlayed);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean authenticatePlayer(String username, String password) {
        if (username == null || username.trim().isEmpty()) return false;
        String sql = "SELECT password FROM players WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return storedPassword.equals(password); // Not hashed for simplicity of demo
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean registerPlayer(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) return false;
        String sql = "INSERT INTO players (player_name, password, total_coins, highest_score, total_distance) VALUES (?, ?, 0, 0, 0)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean submitScore(String username, int score, int coins, int distance, int durationMillis) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            int playerId = -1;
            try (PreparedStatement getPlayer = conn.prepareStatement("SELECT player_id FROM players WHERE player_name = ?")) {
                getPlayer.setString(1, username);
                try (ResultSet rs = getPlayer.executeQuery()) {
                    if (rs.next()) {
                        playerId = rs.getInt(1);
                    }
                }
            }
            if (playerId == -1) {
                conn.rollback();
                return false; // Player must exist now since they logged in
            }

            try (PreparedStatement insertSession = conn.prepareStatement(
                    "INSERT INTO game_sessions (player_id, score, coins_collected, distance, game_duration) VALUES (?, ?, ?, ?, ?)")) {
                insertSession.setInt(1, playerId);
                insertSession.setInt(2, score);
                insertSession.setInt(3, coins);
                insertSession.setInt(4, distance);
                insertSession.setInt(5, durationMillis);
                insertSession.executeUpdate();
            }

            int newHighestScore = 0;
            try (PreparedStatement updatePlayer = conn.prepareStatement(
                    "UPDATE players SET total_coins = total_coins + ?, total_distance = total_distance + ?, highest_score = GREATEST(highest_score, ?), last_played = CURRENT_TIMESTAMP WHERE player_id = ? RETURNING highest_score")) {
                updatePlayer.setInt(1, coins);
                updatePlayer.setInt(2, distance);
                updatePlayer.setInt(3, score);
                updatePlayer.setInt(4, playerId);
                try (ResultSet rs = updatePlayer.executeQuery()) {
                    if (rs.next()) {
                        newHighestScore = rs.getInt(1);
                    }
                }
            }

            try (PreparedStatement updateLeaderboard = conn.prepareStatement(
                    "UPDATE leaderboard SET high_score = ?, updated_on = CURRENT_TIMESTAMP WHERE player_id = ?")) {
                updateLeaderboard.setInt(1, newHighestScore);
                updateLeaderboard.setInt(2, playerId);
                int rowsAffected = updateLeaderboard.executeUpdate();
                
                if (rowsAffected == 0) {
                    try (PreparedStatement insertLeaderboard = conn.prepareStatement(
                            "INSERT INTO leaderboard (player_id, high_score) VALUES (?, ?)")) {
                        insertLeaderboard.setInt(1, playerId);
                        insertLeaderboard.setInt(2, newHighestScore);
                        insertLeaderboard.executeUpdate();
                    }
                }
            }

            conn.commit();
            return true;
            
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static List<PlayerInfo> getLeaderboard(int limit) {
        List<PlayerInfo> leaderboard = new ArrayList<>();
        String sql = "SELECT p.player_name, l.high_score, p.total_coins, " +
                     "(SELECT COUNT(*) FROM game_sessions s WHERE s.player_id = p.player_id) as games_played " +
                     "FROM leaderboard l " +
                     "JOIN players p ON l.player_id = p.player_id " +
                     "ORDER BY l.high_score DESC LIMIT ?";
                     
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    leaderboard.add(new PlayerInfo(
                        rs.getString("player_name"),
                        rs.getInt("high_score"),
                        rs.getInt("total_coins"),
                        rs.getInt("games_played")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leaderboard;
    }
}
