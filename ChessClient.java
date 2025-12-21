import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChessClient extends JFrame {
    private static final int CELL_SIZE = 60;
    private static final int BOARD_WIDTH = 9;
    private static final int BOARD_HEIGHT = 10;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    private JPanel boardPanel;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JLabel statusLabel;
    
    private String[][] board = new String[BOARD_HEIGHT][BOARD_WIDTH];
    private String playerColor;
    private String currentPlayer = "红";
    private Point selectedPiece = null;
    
    public ChessClient() {
        setTitle("中国象棋网络版");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        
        initBoard();
        createBoardPanel();
        createChatPanel();
        createStatusPanel();
        
        pack();
        setLocationRelativeTo(null);
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
    
    private void createBoardPanel() {
        boardPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawBoard(g);
                drawPieces(g);
                if (selectedPiece != null) {
                    g.setColor(Color.YELLOW);
                    g.drawRect(selectedPiece.y * CELL_SIZE + 20, 
                              selectedPiece.x * CELL_SIZE + 20, 
                              CELL_SIZE, CELL_SIZE);
                }
            }
        };
        
        boardPanel.setPreferredSize(new Dimension(
            BOARD_WIDTH * CELL_SIZE + 40, 
            BOARD_HEIGHT * CELL_SIZE + 40
        ));
        boardPanel.setBackground(new Color(220, 179, 92));
        
        boardPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleBoardClick(e);
            }
        });
        
        add(boardPanel, BorderLayout.CENTER);
    }
    
    private void drawBoard(Graphics g) {
        g.setColor(Color.BLACK);
        
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            g.drawLine(20, i * CELL_SIZE + 20, 
                      BOARD_WIDTH * CELL_SIZE - 40, i * CELL_SIZE + 20);
        }
        
        for (int i = 0; i < BOARD_WIDTH; i++) {
            g.drawLine(i * CELL_SIZE + 20, 20, 
                      i * CELL_SIZE + 20, BOARD_HEIGHT * CELL_SIZE - 40);
        }
        
        g.setColor(Color.RED);
        g.drawRect(3 * CELL_SIZE + 20, 20, 3 * CELL_SIZE, 2 * CELL_SIZE);
        g.drawRect(3 * CELL_SIZE + 20, 7 * CELL_SIZE + 20, 3 * CELL_SIZE, 2 * CELL_SIZE);
    }
    
    private void drawPieces(Graphics g) {
        g.setFont(new Font("宋体", Font.BOLD, 24));
        
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (!board[i][j].equals("  ")) {
                    String piece = board[i][j];
                    
                    if ("车马相仕帅炮兵".contains(piece)) {
                        g.setColor(Color.RED);
                    } else {
                        g.setColor(Color.BLACK);
                    }
                    
                    g.fillOval(j * CELL_SIZE + 10, i * CELL_SIZE + 10, 40, 40);
                    g.setColor(Color.WHITE);
                    g.drawString(piece, j * CELL_SIZE + 18, i * CELL_SIZE + 38);
                }
            }
        }
    }
    
    private void createChatPanel() {
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setPreferredSize(new Dimension(300, 0));
        chatPanel.setBorder(BorderFactory.createTitledBorder("聊天窗口"));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        
        chatInput = new JTextField();
        chatInput.addActionListener(e -> sendChatMessage());
        
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendChatMessage());
        
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        
        add(chatPanel, BorderLayout.EAST);
    }
    
    private void createStatusPanel() {
        JPanel statusPanel = new JPanel();
        statusLabel = new JLabel("等待连接服务器...");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void connectToServer() {
        String host = JOptionPane.showInputDialog(this, "请输入服务器地址:", "localhost");
        if (host == null || host.trim().isEmpty()) {
            host = "localhost";
        }
        
        try {
            socket = new Socket(host, 8888);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        handleServerMessage(message);
                    }
                } catch (IOException e) {
                    appendChat("系统", "与服务器断开连接");
                }
            }).start();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "无法连接到服务器!");
            System.exit(0);
        }
    }
    
    private void handleServerMessage(String message) {
        if (message.startsWith("COLOR:")) {
            playerColor = message.substring(6);
            SwingUtilities.invokeLater(() -> 
                statusLabel.setText("你是" + playerColor + "方 | 当前回合: " + currentPlayer)
            );
        } else if (message.startsWith("BOARD:")) {
            updateBoard(message.substring(6));
        } else if (message.startsWith("CHAT:")) {
            String chatMsg = message.substring(5);
            appendChat("", chatMsg);
        } else if (message.startsWith("ERROR:")) {
            String error = message.substring(6);
            JOptionPane.showMessageDialog(this, error);
        }
    }
    
    private void updateBoard(String data) {
        String[] parts = data.split(",");
        int idx = 0;
        
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                board[i][j] = parts[idx++];
            }
        }
        
        if (idx < parts.length) {
            currentPlayer = parts[idx];
        }
        
        SwingUtilities.invokeLater(() -> {
            boardPanel.repaint();
            statusLabel.setText("你是" + playerColor + "方 | 当前回合: " + currentPlayer);
        });
    }
    
    private void handleBoardClick(MouseEvent e) {
        int row = (e.getY() - 20) / CELL_SIZE;
        int col = (e.getX() - 20) / CELL_SIZE;
        
        if (row < 0 || row >= BOARD_HEIGHT || col < 0 || col >= BOARD_WIDTH) {
            return;
        }
        
        if (selectedPiece == null) {
            if (!board[row][col].equals("  ")) {
                String piece = board[row][col];
                boolean isRed = "车马相仕帅炮兵".contains(piece);
                
                if ((playerColor.equals("红") && isRed) || 
                    (playerColor.equals("黑") && !isRed)) {
                    selectedPiece = new Point(row, col);
                    boardPanel.repaint();
                }
            }
        } else {
            out.println("MOVE:" + selectedPiece.x + "," + selectedPiece.y + "," + row + "," + col);
            selectedPiece = null;
            boardPanel.repaint();
        }
    }
    
    private void sendChatMessage() {
        String message = chatInput.getText().trim();
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChessClient());
    }
}