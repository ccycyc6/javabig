package src;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PlayerInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private int playerId;
    private String playerName;
    private String password;
    private int totalGames;
    private int wins;
    private int losses;
    private LocalDateTime createdAt;
    
    public PlayerInfo() {}
    
    public PlayerInfo(String playerName, String password) {
        this.playerName = playerName;
        this.password = password;
        this.totalGames = 0;
        this.wins = 0;
        this.losses = 0;
        this.createdAt = LocalDateTime.now();
    }
    
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }
    public int getWins() { return wins; }
    public void setWins(int wins) { this.wins = wins; }
    public int getLosses() { return losses; }
    public void setLosses(int losses) { this.losses = losses; }
    public double getWinRate() { return totalGames > 0 ? (double) wins / totalGames : 0.0; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return String.format("Player: %s | Games: %d | Wins: %d | Rate: %.1f%%",
                playerName, totalGames, wins, getWinRate() * 100);
    }
}
