package src;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import javax.swing.border.Border;


public class LeaderboardPanel extends JPanel {

    /* ==================== 统一配色（稳定不炸） ==================== */

    private static final Color BG_PANEL   = new Color(248, 248, 248);
    private static final Color BG_TABLE   = Color.WHITE;
    private static final Color BG_HEADER  = new Color(230, 230, 230);
    private static final Color GRID_COLOR = new Color(210, 210, 210);

    private static final Color FG_NORMAL  = Color.BLACK;
    private static final Color FG_HINT    = new Color(130, 130, 130);

    private static final Color BTN_BG     = new Color(139, 69, 19);

    /* ==================== 成员 ==================== */

    private JTable leaderboardTable;
    private JTable gameHistoryTable;

    private ChessDatabase database;
    private int currentPlayerId = -1;

    /* ==================== 构造 ==================== */

    public LeaderboardPanel(ChessDatabase database) {
        this.database = database;

        setLayout(new BorderLayout(10, 10));
        setBackground(BG_PANEL);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        createLeaderboardPanel();
        createGameHistoryPanel();

        // UI 创建完成后立刻加载排行榜（不用点）
        SwingUtilities.invokeLater(this::loadLeaderboardAsync);
    }

    /* ==================== UI 构建 ==================== */

    private void createLeaderboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(BG_PANEL);
        panel.setBorder(createTitleBorder("排行榜"));

        leaderboardTable = createTable(
                new String[]{"排名", "玩家", "胜场", "负场", "总局数", "胜率"}
        );

        JScrollPane sp = new JScrollPane(leaderboardTable);
        sp.getViewport().setBackground(BG_TABLE);
        panel.add(sp, BorderLayout.CENTER);

        JButton refresh = createButton("刷新");
        refresh.addActionListener(e -> loadLeaderboardAsync());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.add(refresh);

        panel.add(btnPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.NORTH);
    }

    private void createGameHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(BG_PANEL);
        panel.setBorder(createTitleBorder("我的对局记录"));

        gameHistoryTable = createTable(
                new String[]{"对局时间", "我的角色", "对局结果", "对手", "对局时长"}
        );

        JScrollPane sp = new JScrollPane(gameHistoryTable);
        sp.getViewport().setBackground(BG_TABLE);
        panel.add(sp, BorderLayout.CENTER);

        JButton refresh = createButton("刷新记录");
        refresh.addActionListener(e -> loadHistoryAsync());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_PANEL);
        btnPanel.add(refresh);

        panel.add(btnPanel, BorderLayout.SOUTH);
        add(panel, BorderLayout.CENTER);
    }

    private Border createTitleBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BTN_BG, 2),
                title,
                0, 0,
                new Font("Arial", Font.BOLD, 16),
                BTN_BG
        );
    }

    private JButton createButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 12));
        b.setBackground(BTN_BG);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        return b;
    }

    /* ==================== 表格（关键修复） ==================== */

    private JTable createTable(String[] columns) {

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Arial", Font.PLAIN, 12));
        table.setBackground(BG_TABLE);
        table.setGridColor(GRID_COLOR);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setBackground(BG_HEADER);
        header.setForeground(Color.BLACK);
        header.setOpaque(true);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                setOpaque(true);
                setHorizontalAlignment(JLabel.CENTER);

                if (value != null && column == 0 &&
                        (value.toString().contains("暂无")
                                || value.toString().contains("请先")
                                || value.toString().contains("加载"))) {

                    setForeground(FG_HINT);
                    setBackground(BG_TABLE);
                    setHorizontalAlignment(JLabel.LEFT);
                } else {
                    setForeground(FG_NORMAL);
                    setBackground(isSelected
                            ? table.getSelectionBackground()
                            : BG_TABLE);
                }
                return this;
            }
        };

        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        return table;
    }

    /* ==================== 数据加载 ==================== */

    private void loadLeaderboardAsync() {
        new SwingWorker<List<PlayerInfo>, Void>() {
            protected List<PlayerInfo> doInBackground() {
                return database.getLeaderboard(20);
            }
            protected void done() {
                try {
                    updateLeaderboard(get());
                } catch (Exception e) {
                    showLeaderboardMessage("加载失败");
                }
            }
        }.execute();
    }

    private void loadHistoryAsync() {
        if (currentPlayerId == -1) {
            showHistoryMessage("请先登录");
            return;
        }

        new SwingWorker<List<GameRecord>, Void>() {
            protected List<GameRecord> doInBackground() {
                return database.getGameHistory(currentPlayerId, 10);
            }
            protected void done() {
                try {
                    updateHistory(get());
                } catch (Exception e) {
                    showHistoryMessage("加载失败");
                }
            }
        }.execute();
    }

    /* ==================== UI 更新 ==================== */

    private void updateLeaderboard(List<PlayerInfo> list) {
        DefaultTableModel m =
                (DefaultTableModel) leaderboardTable.getModel();
        m.setRowCount(0);

        if (list.isEmpty()) {
            m.addRow(new Object[]{"暂无数据", null, null, null, null, null});
            return;
        }

        int rank = 1;
        for (PlayerInfo p : list) {
            m.addRow(new Object[]{
                    rank++,
                    p.getPlayerName(),
                    p.getWins(),
                    p.getLosses(),
                    p.getTotalGames(),
                    String.format("%.1f%%", p.getWinRate() * 100)
            });
        }
    }

    private void updateHistory(List<GameRecord> list) {
        DefaultTableModel m =
                (DefaultTableModel) gameHistoryTable.getModel();
        m.setRowCount(0);

        if (list.isEmpty()) {
            m.addRow(new Object[]{"暂无对局记录", null, null, null, null});
            return;
        }

        for (GameRecord r : list) {
            // My Role, Opponent, Result
            String myRole;
            String opponentName;
            String result;

            if (currentPlayerId == r.getRedPlayerId()) {
                myRole = "红";
                opponentName = r.getBlackPlayerName();
            } else {
                myRole = "黑";
                opponentName = r.getRedPlayerName();
            }

            if (r.getWinnerId() == -1) {
                result = "和";
            } else if (currentPlayerId == r.getWinnerId()) {
                result = "赢";
            } else {
                result = "输";
            }
            
            // Time
            String time = "";
            if (r.getStartTime() != null) {
                try {
                    time = r.getStartTime().format(
                        java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm")
                    );
                } catch (Exception e) {
                    String timeStr = r.getStartTime().toString();
                    time = timeStr.length() > 16 ? timeStr.substring(5, 16) : timeStr;
                }
            }
            
            // Duration
            String duration = "";
            if (r.getGameDurationSeconds() > 0) {
                long seconds = r.getGameDurationSeconds();
                long minutes = seconds / 60;
                long remainingSeconds = seconds % 60;
                duration = String.format("%d分 %02d秒", minutes, remainingSeconds);
            }
            
            m.addRow(new Object[]{
                    time,
                    myRole,
                    result,
                    opponentName,
                    duration
            });
        }
    }

    private void showLeaderboardMessage(String msg) {
        DefaultTableModel m =
                (DefaultTableModel) leaderboardTable.getModel();
        m.setRowCount(0);
        m.addRow(new Object[]{msg, null, null, null, null, null});
    }

    private void showHistoryMessage(String msg) {
        DefaultTableModel m =
                (DefaultTableModel) gameHistoryTable.getModel();
        m.setRowCount(0);
        m.addRow(new Object[]{msg, null, null, null, null});
    }

    /* ==================== 对外接口 ==================== */

    public void setCurrentPlayer(int id, String name) {
        this.currentPlayerId = id;
        loadHistoryAsync();
    }
}
