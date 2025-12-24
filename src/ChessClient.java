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
    
    // Sound and animation
    private SoundManager soundManager;
    private Map<String, Point> piecePositions = new HashMap<>();
    private boolean isAnimating = false;
    
    // View rotation
    private boolean shouldRotateBoard = false;
    
    // Game timer
    private String currentTime = "00:00";
    
    // Database related
    private ChessDatabase database;
    private int playerId = -1;
    private String playerName = "";
    private JMenuBar menuBar;
    private JLabel playerInfoLabel;
    
    public ChessClient() {
        setTitle("Chinese Chess Online");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        // Set window size and position
        setSize(1400, 1000);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1200, 900));
        
        // Set overall look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Initialize database
        database = new ChessDatabase();
        
        // Show login dialog
        var loginDialog = new LoginDialog(this, database);
        playerName = loginDialog.getLoginResult();
        
        if (playerName == null || playerName.isEmpty()) {
            // User cancelled login
            System.exit(0);
            return;
        }
        
        // Get player ID
        var player = database.getPlayerByName(playerName);
        if (player != null) {
            playerId = player.getPlayerId();
            System.out.println("Player " + playerName + " logged in, ID: " + playerId);
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
        
        titleLabel = new JLabel("Chinese Chess", JLabel.CENTER);
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
        
        // Create chessboard panel
        boardPanel = new ChessBoardPanel();
        boardPanel.setPreferredSize(new Dimension(
            BOARD_WIDTH * CELL_SIZE + BOARD_MARGIN * 2, 
            BOARD_HEIGHT * CELL_SIZE + BOARD_MARGIN * 2
        ));
        boardPanel.setMinimumSize(new Dimension(400, 400)); // Set minimum size
        
        // Chessboard panel layout settings - occupy main space
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // Chessboard occupies remaining space
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(boardPanel, gbc);
        
        // Create right panel
        var rightPanel = createRightPanel();
        
        // Right panel layout settings - fixed width
        gbc.gridx = 1;
        gbc.weightx = 0.0; // Right panel does not expand, maintains fixed width
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 10, 0, 0);
        mainPanel.add(rightPanel, gbc);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Add component listener to implement adaptive window size changes
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
        
        // Add window state listener
        addWindowStateListener(new WindowAdapter() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                // Readjust layout when window state changes
                SwingUtilities.invokeLater(() -> adjustLayout());
            }
        });
    }
    
    private void adjustLayout() {
        // Force recalculation of chessboard size
        if (boardPanel != null) {
            boardPanel.calculateOptimalSize();
        }
        
        // Ensure layout update
        revalidate();
        repaint();
    }
    
    private JPanel createRightPanel() {
        var rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBackground(new Color(245, 222, 179));
        rightPanel.setPreferredSize(new Dimension(650, 0)); // Set preferred width to 650px
        rightPanel.setMinimumSize(new Dimension(300, 0)); // Minimum width 300px
        
        // Game information panel
        var infoPanel = createGameInfoPanel();
        
        // Chat panel
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
            "Game Information",
            0,
            0,
            new Font("宋体", Font.BOLD, 14),
            new Color(139, 69, 19)
        ));
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90)); // Reduce height
        infoPanel.setPreferredSize(new Dimension(0, 90)); // Set preferred height
        
        var playerLabel = new JLabel("Waiting for assignment...", JLabel.CENTER);
        playerLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        playerLabel.setName("playerLabel");
        
        var turnLabel = new JLabel("Current turn: Red", JLabel.CENTER);
        turnLabel.setFont(new Font("宋体", Font.PLAIN, 14));
        turnLabel.setName("turnLabel");
        
        var timeLabel = new JLabel("Game time: 00:00", JLabel.CENTER);
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
            "Chat Window",
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
        
        var sendButton = new JButton("Send");
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
        
        statusLabel = new JLabel("Waiting to connect to server...");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        statusLabel.setForeground(Color.WHITE);
        
        playerInfoLabel = new JLabel("Player: " + playerName);
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
        
        var gameMenu = new JMenu("Game");
        gameMenu.setForeground(Color.WHITE);
        
        var leaderboardItem = new JMenuItem("View Leaderboard");
        leaderboardItem.addActionListener(e -> showLeaderboard());
        gameMenu.add(leaderboardItem);
        
        gameMenu.addSeparator();
        
        var exitItem = new JMenuItem("Exit");
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
        var leaderboardFrame = new JFrame("Leaderboard - " + playerName);
        leaderboardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        leaderboardFrame.setSize(800, 600);
        leaderboardFrame.setLocationRelativeTo(this);
        
        // Refactor: create panel first, set player info, then show window
        var panel = new LeaderboardPanel(database);
        
        // Ensure player info is set before showing window
        if (playerId > 0) {
            System.out.println("Setting leaderboard player info: " + playerName + " (ID: " + playerId + ")");
            panel.setCurrentPlayer(playerId, playerName);
        } else {
            System.out.println("Unauthenticated user views leaderboard");
        }
        
        leaderboardFrame.add(panel);
        
        // Add window listener to ensure data is loaded after the window is fully displayed
        leaderboardFrame.addWindowStateListener(e -> {
            if (e.getNewState() == Frame.NORMAL) {
System.out.println("Leaderboard window is displayed");
            }
        });
        
        leaderboardFrame.setVisible(true);
        System.out.println("Leaderboard window opened");
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
            
            // Add component listener to adjust size
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
            
// Ensure panel has valid dimensions
            if (panelWidth <= 0 || panelHeight <= 0) return;
            
            // Calculate available drawing area (reserve sufficient margin)
            var availableWidth = panelWidth - 60; // Left and right margin
            var availableHeight = panelHeight - 60; // Top and bottom margin
            
            // Ensure available area is positive
            availableWidth = Math.max(availableWidth, 400);
            availableHeight = Math.max(availableHeight, 400);
            
            // Calculate optimal cell size (take the smaller value to ensure complete display)
            var maxCellWidth = availableWidth / BOARD_WIDTH;
            var maxCellHeight = availableHeight / BOARD_HEIGHT;
            
            // Choose the smaller size to ensure the board is fully visible
            currentCellSize = Math.min(maxCellWidth, maxCellHeight);
            
            // Set a reasonable size range
            currentCellSize = Math.max(currentCellSize, 30); // Minimum 30 pixels to ensure readability
            currentCellSize = Math.min(currentCellSize, 100); // Maximum 100 pixels to prevent it from being too large
            
            // Calculate centered margin
            var boardWidth = currentCellSize * BOARD_WIDTH;
            var boardHeight = currentCellSize * BOARD_HEIGHT;
            
            currentBoardMargin = Math.max((panelWidth - boardWidth) / 2, 20);
            currentBoardMargin = Math.max(currentBoardMargin, (panelHeight - boardHeight) / 2);
            currentBoardMargin = Math.min(currentBoardMargin, 50); // Limit maximum margin
            
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            var g2d = (Graphics2D) g.create();
            
            // Enable anti-aliasing
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            drawBoard(g2d);
            drawPieces(g2d);
            drawSelection(g2d);
            
            g2d.dispose();
        }
        
        private void drawBoard(Graphics2D g2d) {
            // Draw chessboard background
            g2d.setColor(new Color(245, 222, 179));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Ensure the drawing area is within the panel
            var boardRight = currentBoardMargin + (BOARD_WIDTH - 1) * currentCellSize;
            var boardBottom = currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize;
            
            // If the board exceeds the panel range, adjust the margin
            if (boardRight > getWidth() || boardBottom > getHeight()) {
                calculateOptimalSize(); // Recalculate size
                return;
            }
            
            // Draw board lines
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            
            // Horizontal lines
            for (var i = 0; i < BOARD_HEIGHT; i++) {
                var y = currentBoardMargin + i * currentCellSize;
                g2d.drawLine(currentBoardMargin, y, 
                           currentBoardMargin + (BOARD_WIDTH - 1) * currentCellSize, y);
            }
            
            // Vertical lines
            for (var i = 0; i < BOARD_WIDTH; i++) {
                var x = currentBoardMargin + i * currentCellSize;
                if (i == 0 || i == BOARD_WIDTH - 1) {
                    // Draw full border lines
                    g2d.drawLine(x, currentBoardMargin, 
                               x, currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize);
                } else {
                    // The middle line is broken at the river
                    g2d.drawLine(x, currentBoardMargin, 
                               x, currentBoardMargin + 4 * currentCellSize);
                    g2d.drawLine(x, currentBoardMargin + 5 * currentCellSize, 
                               x, currentBoardMargin + (BOARD_HEIGHT - 1) * currentCellSize);
                }
            }
            
            // Draw the palace diagonal lines
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            // Upper palace
            g2d.drawLine(currentBoardMargin + 3 * currentCellSize, currentBoardMargin,
                        currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 2 * currentCellSize);
            g2d.drawLine(currentBoardMargin + 5 * currentCellSize, currentBoardMargin,
                        currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 2 * currentCellSize);
            
            // Lower palace
            g2d.drawLine(currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 7 * currentCellSize,
                        currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 9 * currentCellSize);
            g2d.drawLine(currentBoardMargin + 5 * currentCellSize, currentBoardMargin + 7 * currentCellSize,
                        currentBoardMargin + 3 * currentCellSize, currentBoardMargin + 9 * currentCellSize);
            
            // Draw the river
            g2d.setColor(new Color(160, 82, 45));
            var fontSize = Math.max(16, currentCellSize / 3);
            g2d.setFont(new Font("楷体", Font.BOLD, fontSize));
            
            var fm = g2d.getFontMetrics();
            var textWidth = fm.stringWidth("楚河");
            var textHeight = fm.getHeight();
            
            // Determine text position and content based on view rotation
            if (shouldRotateBoard) {
                // Black's perspective, text needs to be rotated 180 degrees
                g2d.drawString("汉界", 
                             currentBoardMargin + currentCellSize * 2 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 5 + textHeight / 2);
                
                textWidth = fm.stringWidth("楚河");
                g2d.drawString("楚河",
                             currentBoardMargin + currentCellSize * 6 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 5 + textHeight / 2);
            } else {
                // Red's perspective, normal display
                g2d.drawString("楚河", 
                             currentBoardMargin + currentCellSize * 2 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 4 + textHeight / 2);
                
                textWidth = fm.stringWidth("汉界");
                g2d.drawString("汉界",
                             currentBoardMargin + currentCellSize * 6 - textWidth / 2,
                             currentBoardMargin + currentCellSize * 4 + textHeight / 2);
            }
            
            // Draw cannon and pawn markers
            g2d.setColor(new Color(139, 69, 19));
            int[] cannonPositions = {1, 7};
            int[] pawnPositions = {0, 2, 4, 6, 8};
            
            // Draw cannon positions
            for (var col : cannonPositions) {
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 2 * currentCellSize);
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 7 * currentCellSize);
            }
            
            // Draw pawn positions
            for (var col : pawnPositions) {
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 3 * currentCellSize);
                drawPositionMark(g2d, currentBoardMargin + col * currentCellSize, currentBoardMargin + 6 * currentCellSize);
            }
        }
        
        private void drawPositionMark(Graphics2D g2d, int x, int y) {
            var size = Math.max(4, currentCellSize / 15);
            g2d.setStroke(new BasicStroke(Math.max(1, currentCellSize / 60)));
            
            // Top left
            g2d.drawLine(x - size, y, x - size / 2, y);
            g2d.drawLine(x, y - size, x, y - size / 2);
            
            // Top right
            g2d.drawLine(x + size / 2, y, x + size, y);
            g2d.drawLine(x, y - size, x, y - size / 2);
            
            // Bottom left
            g2d.drawLine(x - size, y, x - size / 2, y);
            g2d.drawLine(x, y + size / 2, x, y + size);
            
            // Bottom right
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
                        
                        // Convert coordinates for display
                        var displayCoords = convertToDisplayCoordinates(i, j);
                        var x = currentBoardMargin + displayCoords[1] * currentCellSize;
                        var y = currentBoardMargin + displayCoords[0] * currentCellSize;
                        
                        // Ensure the piece is within the visible area
                        if (x >= 0 && y >= 0 && x < getWidth() && y < getHeight()) {
                            drawPiece(g2d, piece, x, y);
                        }
                    }
                }
            }
        }
        
        private void drawPiece(Graphics2D g2d, String piece, int x, int y) {
            // Determine piece color
            var isRed = "车马相仕帅砲兵".contains(piece); // Chariot, Horse, Elephant, Advisor, General, Cannon, Pawn
            
            // Calculate piece size (dynamically adjusted based on cell size)
            var pieceSize = (int) (currentCellSize * 0.7);
            var pieceRadius = pieceSize / 2;
            
            // Draw piece background
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
            
            // Draw piece border
            g2d.setColor(new Color(139, 69, 19));
            g2d.setStroke(new BasicStroke(Math.max(2, currentCellSize / 30)));
            g2d.drawOval(x - pieceRadius, y - pieceRadius, pieceSize, pieceSize);
            
            // Draw piece text
            g2d.setColor(isRed ? new Color(200, 0, 0) : Color.WHITE);
            var fontSize = Math.max(16, (int) (currentCellSize * 0.4));
            g2d.setFont(new Font("楷体", Font.BOLD, fontSize));
            
            var fm = g2d.getFontMetrics();
            var textWidth = fm.stringWidth(piece);
            var textHeight = fm.getHeight();
            
            g2d.drawString(piece, x - textWidth / 2, y + textHeight / 4);
        }
        
        private void drawSelection(Graphics2D g2d) {
            // Draw selected piece
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
            
            // Draw possible move locations
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
        
        // Rotate 180 degrees
        var actualRow = BOARD_HEIGHT - 1 - displayRow;
        var actualCol = BOARD_WIDTH - 1 - displayCol;
        
        return new int[]{actualRow, actualCol};
    }
    
    private int[] convertToDisplayCoordinates(int actualRow, int actualCol) {
        if (!shouldRotateBoard) {
            return new int[]{actualRow, actualCol};
        }
        
        // Rotate 180 degrees
        var displayRow = BOARD_HEIGHT - 1 - actualRow;
        var displayCol = BOARD_WIDTH - 1 - actualCol;
        
        return new int[]{displayRow, displayCol};
    }
    
    private void handleBoardClick(MouseEvent e) {
        // Get the size of the current chessboard panel
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
            // Check if the same position is clicked (deselect)
            if (selectedPiece.x == row && selectedPiece.y == col) {
                selectedPiece = null;
                possibleMove = null;
                boardPanel.repaint();
                return;
            }
            
            // Check if there is a friendly piece at the target location
            var targetPiece = board[row][col];
            if (!targetPiece.equals("  ")) {
                var targetIsRed = "车马相仕帅砲兵".contains(targetPiece); // Chariot, Horse, Elephant, Advisor, General, Cannon, Pawn
                var selectedIsRed = "车马相仕帅砲兵".contains(board[selectedPiece.x][selectedPiece.y]); // Chariot, Horse, Elephant, Advisor, General, Cannon, Pawn
                
                if (targetIsRed == selectedIsRed) {
                    // Clicked on another friendly piece, switch selection
                    selectedPiece = new Point(row, col);
                    soundManager.playSelectSound();
                    boardPanel.repaint();
                    return;
                }
            }
            
            // Execute move
            out.println("MOVE:" + selectedPiece.x + "," + selectedPiece.y + "," + row + "," + col);
            soundManager.playMoveSound(!targetPiece.equals("  ")); // If there is a target piece, play the capture sound
            selectedPiece = null;
            possibleMove = null;
            boardPanel.repaint();
        }
    }
    
    private void handleMouseMove(MouseEvent e) {
        // Get the size of the current chessboard panel
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
        var host = JOptionPane.showInputDialog(this, "Please enter the server address:", "localhost");
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        
        try {
            socket = new Socket(host, 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send login information to the server
            out.println("LOGIN:" + playerName);
            
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        handleServerMessage(message);
                    }
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> 
                        appendChat("System", "Disconnected from server")
                    );
                }
            }).start();
            
            // Immediately request board state after successful connection
            out.println("GET_BOARD");
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Unable to connect to the server!");
            if (database != null) {
                database.closeConnection();
            }
            System.exit(0);
        }
    }
    
    private void handleServerMessage(String message) {
        if (message.startsWith("COLOR:")) {
            playerColor = message.substring(6);
            // Black needs to rotate the board perspective
            shouldRotateBoard = playerColor.equals("黑");
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("You are " + playerColor + " | Current turn: " + currentPlayer);
                updateGameInfo();
                boardPanel.repaint();
            });
        } else if (message.startsWith("BOARD:")) {
            updateBoard(message.substring(6));
        } else if (message.startsWith("CHAT:")) {
            var chatMsg = message.substring(5);
            appendChat("", chatMsg);
            
            // Check if it is a game over message
            if (chatMsg.contains("获胜") || chatMsg.contains("吃掉了")) {
                soundManager.playWinSound();
                // Show game over dialog
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, chatMsg, "Game Over", JOptionPane.INFORMATION_MESSAGE);
                    resetGame();
                });
            } else if (chatMsg.contains("新游戏开始")) {
                resetGame();
            } else if (chatMsg.contains("被将军")) {
                // Check notification sound
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
            statusLabel.setText("You are " + playerColor + " | Current turn: " + currentPlayer);
            updateGameInfo();
        });
    }
    
    private void updateGameInfo() {
        // Update game info panel
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
                    label.setText("Your color: " + playerColor);
                } else if (label.getName().equals("turnLabel")) {
                    label.setText("Current turn: " + currentPlayer);
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
        var timeString = "Game time: " + currentTime;
        
        // Update time display
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
                if (label.getText().startsWith("Game time:")) {
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
    
    // SoundManager class
    class SoundManager {
        private boolean soundEnabled = true;
        
        public SoundManager() {
            // Check if the system supports sound effects
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
            // Play select sound
            playTone(800, 50);
        }
        
        public void playMoveSound(boolean capture) {
            if (!soundEnabled) return;
            // Play move sound, higher frequency for captures
            playTone(capture ? 1200 : 600, 100);
        }
        
        public void playErrorSound() {
            if (!soundEnabled) return;
            // Play error sound
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
