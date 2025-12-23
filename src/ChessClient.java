package src;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.sound.sampled.*;

public class ChessClient extends JFrame {
    private static final int CELL_SIZE = 85;
    private static final int BOARD_WIDTH = 9;
    private static final int BOARD_HEIGHT = 10;
    private static final int BOARD_MARGIN = 50;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private ChessBoardPanel boardPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    private JLabel titleLabel;
    
    private String[][] board = new String[BOARD_HEIGHT][BOARD_WIDTH];
    private String playerColor;
    private String currentPlayer = "红";
    private Point selectedPiece = null;
    private Point possibleMove = null;
    
    // 音效和动画
    private SoundManager soundManager;
    private Map<String, Point> piecePositions = new HashMap<>();
    private boolean isAnimating = false;
    
    // 视角旋转
    private boolean shouldRotateBoard = false;
    
    // 游戏计时
    private String currentTime = "00:00";
    
    // 数据库相关
    private ChessDatabase database;
    private int playerId = -1;
    private String playerName = "";
    private JMenuBar menuBar;
    private JLabel playerInfoLabel;
    
    public ChessClient() {
        setTitle("中国象棋网络版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // 设置窗口大小和位置
        setSize(1400, 1000);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 900));
        
        // 设置整体外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 初始化数据库
        database = new ChessDatabase();
        
        // 显示登录对话框
        var loginDialog = new LoginDialog(this, database);
        playerName = loginDialog.getLoginResult();
        
        if (playerName == null || playerName.isEmpty()) {
            // 用户取消登录
            System.exit(0);
            return;
        }
        
        // 获取玩家ID
        var player = database.getPlayerByName(playerName);
        if (player != null) {
            playerId = player.getPlayerId();
            System.out.println("玩家 " + playerName + " 已登录，ID: " + playerId);
        }
        
        initBoard();
        soundManager = new SoundManager();
        createTitlePanel();
        createMainPanel();
        createStatusPanel();
        createMenuBar();
        
        setVisible(true);
        
        connectToServer();
    }
    
    private void initBoard() {
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = "  ";
            }
        }
    }
    
    private void createTitlePanel() {
        var titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(139, 69, 19));
        titlePanel.setPreferredSize(new Dimension(0, 80));
        
        titleLabel = new JLabel("中国象棋", JLabel.CENTER);
        titleLabel.setFont(new Font("楷体", Font.BOLD, 42));
        titleLabel.setForeground(new Color(255, 215, 0));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);
    }
    
    private void createMainPanel() {
        var mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(245, 222, 179));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        var gbc = new GridBagConstraints();
        
        // 创建棋盘面板
        boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(
            BOARD_WIDTH * CELL_SIZE + BOARD_MARGIN * 2, 
            BOARD_HEIGHT * CELL_SIZE + BOARD_MARGIN * 2
        ));
        boardPanel.setMinimumSize(new Dimension(400, 400)); // 设置最小尺寸
        
        // 棋盘面板布局设置 - 占用主要空间
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // 棋盘占用剩余空间
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(boardPanel, gbc);
        
        // 创建右侧面板
        var rightPanel = createRightPanel();
        
        // 右侧面板布局设置 - 固定宽度
        gbc.gridx = 1;
        gbc.weightx = 0.0; // 右侧面板不扩展，保持固定宽度
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 10, 0, 0);
        mainPanel.add(rightPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 添加组件监听器，实现窗口大小变化时的自适应
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustLayout();
            }
            
            @Override
            public void componentShown(ComponentEvent e) {
                adjustLayout();
            }
        });
        
        // 添加窗口状态监听
        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                // 窗口状态改变时重新调整布局
                SwingUtilities.invokeLater(() -> adjustLayout());
            }
        });
    }
    
    private void adjustLayout() {
        // 强制重新计算棋盘尺寸
        if (boardPanel != null) {
            boardPanel.calculateOptimalSize();
        }
        
        // 确保布局更新
        revalidate();
        repaint();
    }
    
    private JPanel createRightPanel() {
        var rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(245, 222, 179));
        rightPanel.setPreferredSize(new Dimension(650, 0)); // 设置首选宽度为650px
        rightPanel.setMinimumSize(new Dimension(300, 0)); // 最小宽度 300px
        
        // 游戏信息面板
        var infoPanel = createGameInfoPanel();
        
        // 聊天面板
        var chatPanel = createChatPanel();
        
        rightPanel.add(infoPanel, BorderLayout.NORTH);
        rightPanel.add(chatPanel, BorderLayout.CENTER);
        
        return rightPanel;
    }
    
    private JPanel createGameInfoPanel() {
        var infoPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        infoPanel.setBackground(new Color(222, 184, 135));
        infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
            "游戏信息",
            0,
            0,
            new Font("宋体", Font.BOLD, 14),
            new Color(139, 69, 19)
        ));
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); // 减少高度
        infoPanel.setPreferredSize(new Dimension(0, 90)); // 设置首选高度
        
        var playerLabel = new JLabel("等待分配...", JLabel.CENTER);
        playerLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        playerLabel.setName("playerLabel");
        
        var turnLabel = new JLabel("当前回合: 红", JLabel.CENTER);
        turnLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        turnLabel.setName("turnLabel");
        
        var timeLabel = new JLabel("游戏时间: 00:00", JLabel.CENTER);
        timeLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        
        infoPanel.add(playerLabel);
        infoPanel.add(turnLabel);
        infoPanel.add(timeLabel);
        
        return infoPanel;
    }
    
    private JPanel createChatPanel() {
        var chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(new Color(245, 222, 179));
        chatPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(139, 69, 19), 2),
            "聊天窗口",
            0,
            0,
            new Font("宋体", Font.BOLD, 14),
            new Color(139, 69, 19)
        ));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        chatArea.setBackground(new Color(255, 248, 220));
        chatArea.setForeground(new Color(0, 0, 0));
        chatArea.setMargin(new Insets(4, 4, 4, 4));
        
        var scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().setBackground(new Color(255, 248, 220));
        
        var inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(new Color(245, 222, 179));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        
        chatInput = new JTextField();
        chatInput.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        chatInput.setPreferredSize(new Dimension(0, 35));
        chatInput.setOpaque(true);
        chatInput.setBackground(Color.WHITE);
        chatInput.setForeground(Color.WHITE);
        chatInput.setCaretColor(Color.BLACK);

        chatInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(139, 69, 19), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        chatInput.addActionListener(e -> sendChatMessage());
        
        var sendButton = new JButton("发送");
        sendButton.setFont(new Font("微软雅黑", Font.BOLD, 12));
        sendButton.setBackground(new Color(139, 69, 19));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(70, 35));
        sendButton.setBorder(BorderFactory.createRaisedBevelBorder());
        sendButton.addActionListener(e -> sendChatMessage());
        
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        return chatPanel;
    }
    
    private void createStatusPanel() {
        var statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(139, 69, 19));
        statusPanel.setPreferredSize(new Dimension(0, 40));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("等待连接服务器...");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        
        playerInfoLabel = new JLabel("玩家: " + playerName);
        playerInfoLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        playerInfoLabel.setForeground(Color.WHITE);
        
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(playerInfoLabel, BorderLayout.EAST);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void createMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(new Color(139, 69, 19));
        menuBar.setForeground(Color.WHITE);
        
        var gameMenu = new JMenu("游戏");
        gameMenu.setForeground(Color.WHITE);
        
        var leaderboardItem = new JMenuItem("查看排行榜");
        leaderboardItem.addActionListener(e -> showLeaderboard());
        gameMenu.add(leaderboardItem);
        
        gameMenu.addSeparator();
        
        var exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> {
            if (database != null) {
                database.closeConnection();
            }
            System.exit(0);
        });
        gameMenu.add(exitItem);
        
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }
    
    private void showLeaderboard() {
        var leaderboardFrame = new JFrame("排行榜 - " + playerName);
        leaderboardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        leaderboardFrame.setSize(800, 600);
        leaderboardFrame.setLocationRelativeTo(this);
        
        // 重构：先创建面板，设置玩家信息，再显示窗口
        var panel = new LeaderboardPanel(database);
        
        // 确保玩家信息设置完成后再显示窗口
        if (playerId > 0) {
            System.out.println("设置排行榜玩家信息: " + playerName + " (ID: " + playerId + ")");
            panel.setCurrentPlayer(playerId, playerName);
        } else {
            System.out.println("未登录用户查看排行榜");
        }
        
        leaderboardFrame.add(panel);
        
        // 添加窗口监听器，确保窗口完全显示后数据已加载
        leaderboardFrame.addWindowStateListener(e -> {
            if (e.getNewState() == Frame.NORMAL) {
                System.out.println("排行榜窗口已显示");
            }
        });
        
        leaderboardFrame.setVisible(true);
        System.out.println("排行榜窗口已打开");
    }
    
    private class ChessBoardPanel extends JPanel {
        private int currentCellSize = CELL_SIZE;
        private int currentBoardMargin = BOARD_MARGIN;
        
        public ChessBoardPanel() {
            setBackground(new Color(245, 222, 179));
            setBorder(BorderFactory.createLineBorder(new Color(139, 69, 19), 3));
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleBoardClick(e);
                }
                
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleMouseMove(e);
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    handleMouseMove(e);
                }
            });
            
            // 添加组件监听器来调整大小
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    calculateOptimalSize();
                }
            });
        }
        
        private void calculateOptimalSize() {
            var panelWidth = getWidth();
            var panelHeight = getHeight();
            
            // 确保面板有有效尺寸
            if (panelWidth <= 0 || panelHeight <= 0) return;
            
            // 计算可用的绘图区域（预留足够边距）
            var availableWidth = panelWidth - 60; // 左右边距
            var availableHeight = panelHeight - 60; // 上下边距
            
            // 确保可用区域为正数
            availableWidth = Math.max(availableWidth, 400);
            availableHeight = Math.max(availableHeight, 400);
            
            // 计算最佳单元格大小（取较小值确保完整显示）
            var maxCellWidth = availableWidth / BOARD_WIDTH;
            var maxCellHeight = availableHeight / BOARD_HEIGHT;
            
            // 选择较小的尺寸确保棋盘完整显示
            currentCellSize = Math.min(maxCellWidth, maxCellHeight);
            
            // 设置合理的尺寸范围
            currentCellSize = Math.max(currentCellSize, 30); // 最小30像素确保可读性
            currentCellSize = Math.min(currentCellSize, 100); // 最大100像素防止过大
            
            // 计算居中的边距
            var boardWidth = currentCellSize * BOARD_WIDTH;
            var boardHeight = currentCellSize * BOARD_HEIGHT;
            
            currentBoardMargin = Math.max((panelWidth - boardWidth) / 2, 20);
            currentBoardMargin = Math.max(currentBoardMargin, (panelHeight - boardHeight) / 2);
            currentBoardMargin = Math.min(currentBoardMargin, 50); // 限制最大边距
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2d = (Graphics2D) g.create();
            
            // 启用抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            drawBoard(g2d);
            drawPieces(g2d);
            drawSelection(g2d);
            
            g2d.dispose();
        }
        
        private void drawBoard(Graphics2D g2d) {
            // 绘制棋盘背景
            g2d.setColor(new Color(245, 222, 179));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // 确保绘制区域在面板范围内
            var boardRight = currentBoardMargin + (BOARD_WIDTH - 1) * currentCellSize;
            var boardBottom = currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize;
            
            // 如果棋盘超出面板范围，调整边距
            if (boardRight > getWidth() || boardBottom > getHeight()) {
                calculateOptimalSize(); // 重新计算尺寸
                return;
            }
            
            // 绘制棋盘线
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            
            // 横线
            for (var i = 0; i < BOARD_HEIGHT; i++) {
                var y = currentBoardMargin + i * currentCellSize;
                g2d.drawLine(currentBoardMargin, y, 
                           currentBoardMargin + (BOARD_WIDTH - 1) * currentCellSize, y);
            }
            
            // 竖线
            for (var i = 0; i < BOARD_WIDTH; i++) {
                var x = currentBoardMargin + i * currentCellSize;
                if (i == 0 || i == BOARD_WIDTH - 1) {
                    // 边线画完整
                    g2d.drawLine(x, currentBoardMargin, 
                               x, currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize);
                } else {
                    // 中间线在楚河汉界处断开
                    g2d.drawLine(x, currentBoardMargin, 
                               x, currentBoardMargin + 4 * currentCellSize);
                    g2d.drawLine(x, currentBoardMargin + 5 * currentCellSize, 
                               x, currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize);
                }
            }
            
            // 绘制九宫格斜线
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            // 上方九宫格
            g2d.drawLine(currentBoardMargin + 3 * currentCellSize, currentBoardMargin,
                        currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 2 * currentCellSize);
            g2d.drawLine(currentBoardMargin + 5 * currentCellSize, currentBoardMargin,
                        currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 2 * currentCellSize);
            
            // 下方九宫格
            g2d.drawLine(currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 7 * currentCellSize,
                        currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 9 * currentCellSize);
            g2d.drawLine(currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 7 * currentCellSize,
                        currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 9 * currentCellSize);
            
            // 绘制楚河汉界
            g2d.setColor(new Color(160, 82, 45));
            var fontSize = Math.max(16, currentCellSize / 3);
            g2d.setFont(new Font("楷体", Font.BOLD, fontSize));
            
            var fm = g2d.getFontMetrics();
            var textWidth = fm.stringWidth("楚河");
            var textHeight = fm.getHeight();
            
            // 根据视角旋转决定文字位置和内容
            if (shouldRotateBoard) {
                // 黑方视角，文字需要旋转180度
                g2d.drawString("汉界", 
                             currentBoardMargin + currentCellSize * 2 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 5 + textHeight / 2);
                
                textWidth = fm.stringWidth("楚河");
                g2d.drawString("楚河",
                             currentBoardMargin + currentCellSize * 6 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 5 + textHeight / 2);
            } else {
                // 红方视角，正常显示
                g2d.drawString("楚河", 
                             currentBoardMargin + currentCellSize * 2 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 4 + textHeight / 2);
                
                textWidth = fm.stringWidth("汉界");
                g2d.drawString("汉界",
                             currentBoardMargin + currentCellSize * 6 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 4 + textHeight / 2);
            }
            
            // 绘制炮位和兵位标记
            g2d.setColor(new Color(139, 69, 19));
            int[] cannonPositions = {1, 7};
            int[] pawnPositions = {0, 2, 4, 6, 8};
            
            // 绘制炮位
            for (var col : cannonPositions) {
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 2 * currentCellSize);
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 7 * currentCellSize);
            }
            
            // 绘制兵位
            for (var col : pawnPositions) {
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 3 * currentCellSize);
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 6 * currentCellSize);
            }
        }
        
        private void drawPositionMark(Graphics2D g2d, int x, int y) {
            var size = Math.max(4, currentCellSize / 15);
            g2d.setStroke(new BasicStroke(Math.max(1, currentCellSize / 60)));
            
            // 左上角
            g2d.drawLine(x - size, y, x - size / 2, y);
            g2d.drawLine(x, y - size, x, y - size / 2);
            
            // 右上角
            g2d.drawLine(x + size / 2, y, x + size, y);
            g2d.drawLine(x, y - size, x, y - size / 2);
            
            // 左下角
            g2d.drawLine(x - size, y, x - size / 2, y);
            g2d.drawLine(x, y + size / 2, x, y + size);
            
            // 右下角
            g2d.drawLine(x + size / 2, y, x + size, y);
            g2d.drawLine(x, y + size / 2, x, y + size);
        }
        
        private void drawPieces(Graphics2D g2d) {
            var fontSize = Math.max(16, currentCellSize / 2);
            g2d.setFont(new Font("楷体", Font.BOLD, fontSize));
            
            for (var i = 0; i < BOARD_HEIGHT; i++) {
                for (var j = 0; j < BOARD_WIDTH; j++) {
                    if (!board[i][j].equals("  ")) {
                        var piece = board[i][j];
                        
                        // 转换坐标用于显示
                        var displayCoords = convertToDisplayCoordinates(i, j);
                        var x = currentBoardMargin + displayCoords[1] * currentCellSize;
                        var y = currentBoardMargin + displayCoords[0] * currentCellSize;
                        
                        // 确保棋子在可见区域内
                        if (x >= 0 && y >= 0 && x < getWidth() && y < getHeight()) {
                            drawPiece(g2d, piece, x, y);
                        }
                    }
                }
            }
        }
        
        private void drawPiece(Graphics2D g2d, String piece, int x, int y) {
            // 判断棋子颜色
            var isRed = "车马相仕帅砲兵".contains(piece);
            
            // 计算棋子大小（根据单元格大小动态调整）
            var pieceSize = (int) (currentCellSize * 0.7);
            var pieceRadius = pieceSize / 2;
            
            // 绘制棋子背景
            GradientPaint gradient;
            if (isRed) {
                gradient = new GradientPaint(x - pieceRadius, y - pieceRadius, new Color(255, 200, 200),
                                           x + pieceRadius, y + pieceRadius, new Color(200, 50, 50));
            } else {
                gradient = new GradientPaint(x - pieceRadius, y - pieceRadius, new Color(100, 100, 100),
                                           x + pieceRadius, y + pieceRadius, new Color(20, 20, 20));
            }
            
            g2d.setPaint(gradient);
            g2d.fillOval(x - pieceRadius, y - pieceRadius, pieceSize, pieceSize);
            
            // 绘制棋子边框
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            g2d.drawOval(x - pieceRadius, y - pieceRadius, pieceSize, pieceSize);
            
            // 绘制棋子文字
            g2d.setColor(isRed ? new Color(200, 0, 0) : Color.WHITE);
            var fontSize = Math.max(16, (int) (currentCellSize * 0.4));
            g2d.setFont(new Font("楷体", Font.BOLD, fontSize));
            
            var fm = g2d.getFontMetrics();
            var textWidth = fm.stringWidth(piece);
            var textHeight = fm.getHeight();
            
            g2d.drawString(piece, x - textWidth / 2, y + textHeight / 4);
        }
        
        private void drawSelection(Graphics2D g2d) {
            // 绘制选中的棋子
            if (selectedPiece != null) {
                var displayCoords = convertToDisplayCoordinates(selectedPiece.x, selectedPiece.y);
                var x = currentBoardMargin + displayCoords[1] * currentCellSize;
                var y = currentBoardMargin + displayCoords[0] * currentCellSize;
                
                var pieceSize = (int) (currentCellSize * 0.7);
                var selectionSize = pieceSize + 10;
                
                g2d.setColor(new Color(255, 215, 0));
                g2d.setStroke(new BasicStroke(Math.max(3, currentCellSize / 20)));
                g2d.drawOval(x - selectionSize/2, y - selectionSize/2, selectionSize, selectionSize);
            }
            
            // 绘制可能的移动位置
            if (possibleMove != null) {
                var displayCoords = convertToDisplayCoordinates(possibleMove.x, possibleMove.y);
                var x = currentBoardMargin + displayCoords[1] * currentCellSize;
                var y = currentBoardMargin + displayCoords[0] * currentCellSize;
                
                var dotSize = Math.max(6, currentCellSize / 10);
                
                g2d.setColor(new Color(0, 255, 0, 128));
                g2d.fillOval(x - dotSize/2, y - dotSize/2, dotSize, dotSize);
            }
        }
    }
    
    private int[] convertCoordinates(int displayRow, int displayCol) {
        if (!shouldRotateBoard) {
            return new int[]{displayRow, displayCol};
        }
        
        // 旋转180度
        var actualRow = BOARD_HEIGHT - 1 - displayRow;
        var actualCol = BOARD_WIDTH - 1 - displayCol;
        
        return new int[]{actualRow, actualCol};
    }
    
    private int[] convertToDisplayCoordinates(int actualRow, int actualCol) {
        if (!shouldRotateBoard) {
            return new int[]{actualRow, actualCol};
        }
        
        // 旋转180度
        var displayRow = BOARD_HEIGHT - 1 - actualRow;
        var displayCol = BOARD_WIDTH - 1 - actualCol;
        
        return new int[]{displayRow, displayCol};
    }
    
    private void handleBoardClick(MouseEvent e) {
        // 获取当前棋盘面板的尺寸
        var panel = (ChessBoardPanel) e.getSource();
        var displayRow = (e.getY() - panel.currentBoardMargin + panel.currentCellSize / 2) / panel.currentCellSize;
        var displayCol = (e.getX() - panel.currentBoardMargin + panel.currentCellSize / 2) / panel.currentCellSize;
        
        var coords = convertCoordinates(displayRow, displayCol);
        var row = coords[0];
        var col = coords[1];
        
        if (row < 0 || row >= BOARD_HEIGHT || col < 0 || col >= BOARD_WIDTH) {
            return;
        }
        
        if (selectedPiece == null) {
            if (!board[row][col].equals("  ")) {
                var piece = board[row][col];
                var isRed = "车马相仕帅砲兵".contains(piece);
                
                if ((playerColor.equals("红") && isRed) || 
                    (playerColor.equals("黑") && !isRed)) {
                    selectedPiece = new Point(row, col);
                    soundManager.playSelectSound();
                    boardPanel.repaint();
                }
            }
        } else {
            // 检查是否点击了同一个位置（取消选择）
            if (selectedPiece.x == row && selectedPiece.y == col) {
                selectedPiece = null;
                possibleMove = null;
                boardPanel.repaint();
                return;
            }
            
            // 检查目标位置是否有己方棋子
            var targetPiece = board[row][col];
            if (!targetPiece.equals("  ")) {
                var targetIsRed = "车马相仕帅砲兵".contains(targetPiece);
                var selectedIsRed = "车马相仕帅砲兵".contains(board[selectedPiece.x][selectedPiece.y]);
                
                if (targetIsRed == selectedIsRed) {
                    // 点击了己方另一个棋子，切换选择
                    selectedPiece = new Point(row, col);
                    soundManager.playSelectSound();
                    boardPanel.repaint();
                    return;
                }
            }
            
            // 执行移动
            out.println("MOVE:" + selectedPiece.x + "," + selectedPiece.y + "," + row + "," + col);
            soundManager.playMoveSound(!targetPiece.equals("  ")); // 如果有目标棋子，播放吃子音效
            selectedPiece = null;
            possibleMove = null;
            boardPanel.repaint();
        }
    }
    
    private void handleMouseMove(MouseEvent e) {
        // 获取当前棋盘面板的尺寸
        var panel = (ChessBoardPanel) e.getSource();
        var displayRow = (e.getY() - panel.currentBoardMargin + panel.currentCellSize / 2) / panel.currentCellSize;
        var displayCol = (e.getX() - panel.currentBoardMargin + panel.currentCellSize / 2) / panel.currentCellSize;
        
        if (displayRow >= 0 && displayRow < BOARD_HEIGHT && displayCol >= 0 && displayCol < BOARD_WIDTH) {
            var coords = convertCoordinates(displayRow, displayCol);
            var newPossibleMove = new Point(coords[0], coords[1]);
            if (!newPossibleMove.equals(possibleMove)) {
                possibleMove = newPossibleMove;
                boardPanel.repaint();
            }
        } else {
            if (possibleMove != null) {
                possibleMove = null;
                boardPanel.repaint();
            }
        }
    }
    
    private void connectToServer() {
        var host = JOptionPane.showInputDialog(this, "请输入服务器地址:", "localhost");
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        
        try {
            socket = new Socket(host, 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // 发送登录信息到服务器
            out.println("LOGIN:" + playerName);
            
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        handleServerMessage(message);
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> 
                        appendChat("系统", "与服务器断开连接")
                    );
                }
            }).start();
            
            // 连接成功后立即请求棋盘状态
            out.println("GET_BOARD");
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法连接到服务器!");
            if (database != null) {
                database.closeConnection();
            }
            System.exit(0);
        }
    }
    
    private void handleServerMessage(String message) {
        if (message.startsWith("COLOR:")) {
            playerColor = message.substring(6);
            // 黑方需要旋转棋盘视角
            shouldRotateBoard = playerColor.equals("黑");
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("你是" + playerColor + "方 | 当前回合: " + currentPlayer);
                updateGameInfo();
                boardPanel.repaint();
            });
        } else if (message.startsWith("BOARD:")) {
            updateBoard(message.substring(6));
        } else if (message.startsWith("CHAT:")) {
            var chatMsg = message.substring(5);
            appendChat("", chatMsg);
            
            // 检查是否是游戏结束消息
            if (chatMsg.contains("获胜") || chatMsg.contains("吃掉了")) {
                soundManager.playWinSound();
                // 显示游戏结束对话框
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, chatMsg, "游戏结束", JOptionPane.INFORMATION_MESSAGE);
                    resetGame();
                });
            } else if (chatMsg.contains("新游戏开始")) {
                resetGame();
            } else if (chatMsg.contains("被将军")) {
                // 将军提示音效
                soundManager.playTone(1000, 150);
            }
        } else if (message.startsWith("TIME:")) {
            currentTime = message.substring(5);
            SwingUtilities.invokeLater(() -> updateTimeDisplay());
        } else if (message.startsWith("ERROR:")) {
            var error = message.substring(6);
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, error);
                soundManager.playErrorSound();
            });
        }
    }
    
    private void updateBoard(String data) {
        var parts = data.split(",");
        var idx = 0;
        
        for (var i = 0; i < BOARD_HEIGHT; i++) {
            for (var j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = parts[idx++];
            }
        }
        
        if (idx < parts.length) {
            currentPlayer = parts[idx];
        }
        
        SwingUtilities.invokeLater(() -> {
            boardPanel.repaint();
            statusLabel.setText("你是" + playerColor + "方 | 当前回合: " + currentPlayer);
            updateGameInfo();
        });
    }
    
    private void updateGameInfo() {
        // 更新游戏信息面板
        for (Component comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                updateGameInfoInPanel((JPanel) comp);
            }
        }
    }
    
    private void updateGameInfoInPanel(JPanel panel) {
        for (var comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                updateGameInfoInPanel((JPanel) comp);
            } else if (comp instanceof JLabel && comp.getName() != null) {
                var label = (JLabel) comp;
                if (label.getName().equals("playerLabel")) {
                    label.setText("你的颜色: " + playerColor);
                } else if (label.getName().equals("turnLabel")) {
                    label.setText("当前回合: " + currentPlayer);
                }
            }
        }
    }
    
    private void sendChatMessage() {
        var message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            out.println("CHAT:" + message);
            chatInput.setText("");
        }
    }
    
    private void appendChat(String sender, String message) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }
    
    private void updateTimeDisplay() {
        var timeString = "游戏时间: " + currentTime;
        
        // 更新时间显示
        for (var comp : getContentPane().getComponents()) {
            if (comp instanceof JPanel) {
                updateTimeInPanel((JPanel) comp, timeString);
            }
        }
    }
    
    private void updateTimeInPanel(JPanel panel, String timeString) {
        for (var comp : panel.getComponents()) {
            if (comp instanceof JPanel) {
                updateTimeInPanel((JPanel) comp, timeString);
            } else if (comp instanceof JLabel) {
                var label = (JLabel) comp;
                if (label.getText().startsWith("游戏时间:")) {
                    label.setText(timeString);
                }
            }
        }
    }
    
    private void resetGame() {
        currentTime = "00:00";
        selectedPiece = null;
        possibleMove = null;
        updateTimeDisplay();
        boardPanel.repaint();
    }
    
    // 音效管理器类
    class SoundManager {
        private boolean soundEnabled = true;
        
        public SoundManager() {
            // 检查系统是否支持音效
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(
                    new ByteArrayInputStream(new byte[0]));
                audioInputStream.close();
            } catch (Exception e) {
                soundEnabled = false;
            }
        }
        
        public void playSelectSound() {
            if (!soundEnabled) return;
            // 播放选择音效
            playTone(800, 50);
        }
        
        public void playMoveSound(boolean capture) {
            if (!soundEnabled) return;
            // 播放移动音效，吃子时频率更高
            playTone(capture ? 1200 : 600, 100);
        }
        
        public void playErrorSound() {
            if (!soundEnabled) return;
            // 播放错误音效
            playTone(300, 200);
        }
        
        public void playWinSound() {
            if (!soundEnabled) return;
            // 播放胜利音效
            new Thread(() -> {
                playTone(523, 200); // C
                try { Thread.sleep(200); } catch (InterruptedException e) {}
                playTone(659, 200); // E
                try { Thread.sleep(200); } catch (InterruptedException e) {}
                playTone(784, 400); // G
            }).start();
        }
        
        private void playTone(int frequency, int duration) {
            if (!soundEnabled) return;
            
            new Thread(() -> {
                try {
                    var line = AudioSystem.getSourceDataLine(
                        new AudioFormat(44100, 16, 1, true, false));
                    line.open();
                    line.start();
                    
                    var buffer = new byte[44100 * duration / 1000];
                    for (var i = 0; i < buffer.length; i++) {
                        var angle = 2.0 * Math.PI * frequency * i / 44100.0;
                        buffer[i] = (byte) (Math.sin(angle) * 127);
                    }
                    
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.close();
                } catch (Exception e) {
                    // 音效播放失败，静默处理
                }
            }).start();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessClient());
    }
}
