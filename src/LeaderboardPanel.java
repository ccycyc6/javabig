package src;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;

public class LeaderboardPanel extends JPanel {
    private JTable leaderboardTable;
    private JTable gameHistoryTable;
    private ChessDatabase database;
    private int currentPlayerId = -1;
    private String currentPlayerName = "";

    public LeaderboardPanel(ChessDatabase database) {
        this.database = database;
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 222, 179));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createLeaderboardPanel();
        createGameHistoryPanel();

        // ★ 构造完成后立即刷新排行榜，保证打开就显示数据
        SwingUtilities.invokeLater(this::refreshLeaderboard);
    }

    private void createLeaderboardPanel() {
        JPanel leaderboardPanel = new JPanel(new BorderLayout(5, 5));
        leaderboardPanel.setBackground(new Color(245, 222, 179));
        leaderboardPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
                "Global Leaderboard",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                new Color(139, 69, 19)
        ));

        leaderboardTable = createTable(new String[]{"Rank", "Player", "Wins", "Losses", "Total", "Win Rate"});
        JScrollPane scrollPane = new JScrollPane(leaderboardTable);
        scrollPane.getViewport().setBackground(Color.WHITE); // ★ 统一视口背景
        leaderboardPanel.add(scrollPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(139, 69, 19));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> refreshLeaderboard());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 222, 179));
        buttonPanel.add(refreshButton);
        leaderboardPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leaderboardPanel, BorderLayout.NORTH);
    }

    private void createGameHistoryPanel() {
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBackground(new Color(245, 222, 179));
        historyPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
                "Game History",
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                new Color(139, 69, 19)
        ));

        gameHistoryTable = createTable(new String[]{"Red", "Black", "Winner", "Duration(s)", "Time"});
        JScrollPane scrollPane = new JScrollPane(gameHistoryTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        JButton historyButton = new JButton("Load My Games");
        historyButton.setFont(new Font("Arial", Font.BOLD, 12));
        historyButton.setBackground(new Color(139, 69, 19));
        historyButton.setForeground(Color.WHITE);
        historyButton.setFocusPainted(false);
        historyButton.addActionListener(e -> refreshGameHistory());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(245, 222, 179));
        buttonPanel.add(historyButton);
        historyPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(historyPanel, BorderLayout.CENTER);
    }

    private JTable createTable(String[] columnNames) {
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setBackground(Color.WHITE);
        table.setGridColor(new Color(200, 200, 200));
        table.setFillsViewportHeight(true); // ★ 空表格也填充视口

        // 表头样式
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setBackground(new Color(222, 184, 135));
        header.setForeground(Color.BLACK);
        header.setOpaque(true);

        // 单元格居中显示
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < columnNames.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    public void setCurrentPlayer(int playerId, String playerName) {
        this.currentPlayerId = playerId;
        this.currentPlayerName = playerName;
        // 设置玩家后立即刷新游戏历史
        SwingUtilities.invokeLater(this::refreshGameHistory);
    }

    public void refreshLeaderboard() {
        List<PlayerInfo> leaderboard = database.getLeaderboard(20);
        DefaultTableModel model = (DefaultTableModel) leaderboardTable.getModel();
        model.setRowCount(0);

        int rank = 1;
        for (PlayerInfo player : leaderboard) {
            model.addRow(new Object[]{
                    rank++,
                    player.getPlayerName(),
                    player.getWins(),
                    player.getLosses(),
                    player.getTotalGames(),
                    String.format("%.1f%%", player.getWinRate() * 100)
            });
        }
    }

    public void refreshGameHistory() {
        if (currentPlayerId == -1) {
            JOptionPane.showMessageDialog(this, "Please login first!");
            return;
        }

        List<GameRecord> history = database.getGameHistory(currentPlayerId, 10);
        DefaultTableModel model = (DefaultTableModel) gameHistoryTable.getModel();
        model.setRowCount(0);

        for (GameRecord record : history) {
            model.addRow(new Object[]{
                    record.getRedPlayerName(),
                    record.getBlackPlayerName(),
                    record.getWinnerName(),
                    record.getGameDurationSeconds(),
                    record.getStartTime().toString().substring(0, 19)
            });
        }
    }
}
