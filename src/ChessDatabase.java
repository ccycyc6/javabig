package src;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChessDatabase {
    private static final String DB_URL = "jdbc:sqlite:chinesechess.db";
    private Connection connection;
    
    public ChessDatabase() {
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            System.out.println("Database initialized successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        String playerTable = """
            CREATE TABLE IF NOT EXISTS players (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                total_games INTEGER DEFAULT 0,
                wins INTEGER DEFAULT 0,
                losses INTEGER DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_played_at TIMESTAMP
            )""";
        
        String recordTable = """
            CREATE TABLE IF NOT EXISTS game_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                red_player_id INTEGER,
                red_player_name TEXT,
                black_player_id INTEGER,
                black_player_name TEXT,
                winner_id INTEGER,
                winner_name TEXT,
                game_duration INTEGER,
                start_time TIMESTAMP,
                end_time TIMESTAMP,
                FOREIGN KEY(red_player_id) REFERENCES players(id),
                FOREIGN KEY(black_player_id) REFERENCES players(id)
            )""";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(playerTable);
            stmt.execute(recordTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Player operations
    public boolean registerPlayer(String name, String password) {
        var sql = "INSERT INTO players(name, password) VALUES(?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Player already exists or error: " + e.getMessage());
            return false;
        }
    }
    
    public PlayerInfo loginPlayer(String name, String password) {
        var sql = "SELECT * FROM players WHERE name = ? AND password = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                var player = new PlayerInfo();
                player.setPlayerId(rs.getInt("id"));
                player.setPlayerName(rs.getString("name"));
                player.setPassword(rs.getString("password"));
                player.setTotalGames(rs.getInt("total_games"));
                player.setWins(rs.getInt("wins"));
                player.setLosses(rs.getInt("losses"));
                return player;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public PlayerInfo getPlayerByName(String name) {
        var sql = "SELECT * FROM players WHERE name = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            var rs = pstmt.executeQuery();
            if (rs.next()) {
                var player = new PlayerInfo();
                player.setPlayerId(rs.getInt("id"));
                player.setPlayerName(rs.getString("name"));
                player.setTotalGames(rs.getInt("total_games"));
                player.setWins(rs.getInt("wins"));
                player.setLosses(rs.getInt("losses"));
                return player;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public void updatePlayerStats(int playerId, boolean isWinner) {
        var sql = "UPDATE players SET total_games = total_games + 1, " +
                (isWinner ? "wins = wins + 1" : "losses = losses + 1") +
                ", last_played_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Game record operations
    public void saveGameRecord(GameRecord record) {
        var sql = "INSERT INTO game_records(red_player_id, red_player_name, " +
                "black_player_id, black_player_name, winner_id, winner_name, " +
                "game_duration, start_time, end_time) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (var pstmt = connection.prepareStatement(sql)) {
            var formatter = 
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            pstmt.setInt(1, record.getRedPlayerId());
            pstmt.setString(2, record.getRedPlayerName());
            pstmt.setInt(3, record.getBlackPlayerId());
            pstmt.setString(4, record.getBlackPlayerName());
            pstmt.setInt(5, record.getWinnerId());
            pstmt.setString(6, record.getWinnerName());
            pstmt.setLong(7, record.getGameDurationSeconds());
            pstmt.setString(8, record.getStartTime().format(formatter));
            pstmt.setString(9, record.getEndTime().format(formatter));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public List<GameRecord> getGameHistory(int playerId, int limit) {
        var records = new ArrayList<GameRecord>();
        var sql = "SELECT * FROM game_records WHERE red_player_id = ? OR black_player_id = ? " +
                "ORDER BY start_time DESC LIMIT ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, playerId);
            pstmt.setInt(2, playerId);
            pstmt.setInt(3, limit);
            var rs = pstmt.executeQuery();
            System.out.println("查询玩家 " + playerId + " 的对局记录");
            
            while (rs.next()) {
                var record = new GameRecord();
                record.setRecordId(rs.getInt("id"));
                record.setRedPlayerId(rs.getInt("red_player_id"));
                record.setRedPlayerName(rs.getString("red_player_name"));
                record.setBlackPlayerId(rs.getInt("black_player_id"));
                record.setBlackPlayerName(rs.getString("black_player_name"));
                record.setWinnerId(rs.getInt("winner_id"));
                record.setWinnerName(rs.getString("winner_name"));
                record.setGameDurationSeconds(rs.getLong("game_duration"));
                
                // Parse timestamps with error handling
                var startTimeStr = rs.getString("start_time");
                var endTimeStr = rs.getString("end_time");
                
                try {
                    if (startTimeStr != null && !startTimeStr.isEmpty()) {
                        record.setStartTime(LocalDateTime.parse(startTimeStr, 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                } catch (Exception e) {
                    System.err.println("解析开始时间失败: " + startTimeStr);
                }
                
                try {
                    if (endTimeStr != null && !endTimeStr.isEmpty()) {
                        record.setEndTime(LocalDateTime.parse(endTimeStr, 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                } catch (Exception e) {
                    System.err.println("解析结束时间失败: " + endTimeStr);
                }
                
                records.add(record);
                System.out.println("对局: " + record.getRedPlayerName() + " vs " + record.getBlackPlayerName());
            }
            System.out.println("对局记录加载完成，共 " + records.size() + " 条记录");
        } catch (SQLException e) {
            System.err.println("查询对局记录失败: " + e.getMessage());
            e.printStackTrace();
        }
        return records;
    }
    
    public List<PlayerInfo> getLeaderboard(int limit) {
        var leaderboard = new ArrayList<PlayerInfo>();
        var sql = "SELECT * FROM players ORDER BY " +
                "CASE WHEN total_games > 0 THEN (CAST(wins AS FLOAT) / total_games) ELSE 0 END DESC, " +
                "wins DESC LIMIT ?";
        try (var pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            var rs = pstmt.executeQuery();
            System.out.println("查询排行榜数据，共找到 " + limit + " 个玩家");
            while (rs.next()) {
                var player = new PlayerInfo();
                player.setPlayerId(rs.getInt("id"));
                player.setPlayerName(rs.getString("name"));
                player.setTotalGames(rs.getInt("total_games"));
                player.setWins(rs.getInt("wins"));
                player.setLosses(rs.getInt("losses"));
                leaderboard.add(player);
                System.out.println("玩家: " + player.getPlayerName() + " 胜场: " + player.getWins());
            }
            System.out.println("排行榜数据加载完成，共 " + leaderboard.size() + " 个玩家");
        } catch (SQLException e) {
            System.err.println("查询排行榜失败: " + e.getMessage());
            e.printStackTrace();
        }
        return leaderboard;
    }
    
    
    public void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
