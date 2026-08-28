public class PlayerInfo {
    private String username;
    private int bestScore;
    private int bestCoins;
    private int gamesPlayed;

    public PlayerInfo(String username, int bestScore, int bestCoins, int gamesPlayed) {
        this.username = username;
        this.bestScore = bestScore;
        this.bestCoins = bestCoins;
        this.gamesPlayed = gamesPlayed;
    }

    public String getUsername() { return username; }
    public int getBestScore() { return bestScore; }
    public int getBestCoins() { return bestCoins; }
    public int getGamesPlayed() { return gamesPlayed; }
}
