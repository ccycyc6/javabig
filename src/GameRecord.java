package src;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GameRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    private int recordId;
    private int redPlayerId;
    private String redPlayerName;
    private int blackPlayerId;
    private String blackPlayerName;
    private int winnerId;
    private String winnerName;
    private long gameDurationSeconds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    
    public GameRecord() {}
    
    public GameRecord(int redPlayerId, String redPlayerName, int blackPlayerId, String blackPlayerName) {
        this.redPlayerId = redPlayerId;
        this.redPlayerName = redPlayerName;
        this.blackPlayerId = blackPlayerId;
        this.blackPlayerName = blackPlayerName;
        this.startTime = LocalDateTime.now();
    }
    
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getRedPlayerId() { return redPlayerId; }
    public void setRedPlayerId(int redPlayerId) { this.redPlayerId = redPlayerId; }
    public String getRedPlayerName() { return redPlayerName; }
    public void setRedPlayerName(String redPlayerName) { this.redPlayerName = redPlayerName; }
    public int getBlackPlayerId() { return blackPlayerId; }
    public void setBlackPlayerId(int blackPlayerId) { this.blackPlayerId = blackPlayerId; }
    public String getBlackPlayerName() { return blackPlayerName; }
    public void setBlackPlayerName(String blackPlayerName) { this.blackPlayerName = blackPlayerName; }
    public int getWinnerId() { return winnerId; }
    public void setWinnerId(int winnerId) { this.winnerId = winnerId; }
    public String getWinnerName() { return winnerName; }
    public void setWinnerName(String winnerName) { this.winnerName = winnerName; }
    public long getGameDurationSeconds() { return gameDurationSeconds; }
    public void setGameDurationSeconds(long gameDurationSeconds) { this.gameDurationSeconds = gameDurationSeconds; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s (Red) vs %s (Black) -> Winner: %s (%ds)",
                startTime, redPlayerName, blackPlayerName, winnerName, gameDurationSeconds);
    }
}
