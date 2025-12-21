import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChessServer {
    private static final int PORT = 8888;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static String[][] board = new String[10][9];
    private static String currentPlayer = "红";
    private static long gameStartTime = System.currentTimeMillis();
    private static boolean gameEnded = false;
    private static ScheduledExecutorService timerExecutor;
    
    public static void main(String[] args) {
        initBoard();
        System.out.println("象棋服务器启动，端口: " + PORT);
        
        // 启动定时器，每秒向所有客户端发送时间更新
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
        timerExecutor.scheduleAtFixedRate(() -> {
            if (!gameEnded) {
                long elapsedSeconds = (System.currentTimeMillis() - gameStartTime) / 1000;
                int minutes = (int) (elapsedSeconds / 60);
                int seconds = (int) (elapsedSeconds % 60);
                String timeMessage = "TIME:" + String.format("%02d:%02d", minutes, seconds);
                
                for (ClientHandler client : clients) {
                    client.out.println(timeMessage);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                clients.add(client);
                new Thread(client).start();
                System.out.println("新玩家连接，当前玩家数: " + clients.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void initBoard() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                board[i][j] = "  ";
            }
        }
        
        // 黑方棋子
        board[0][0] = "車"; board[0][8] = "車";
        board[0][1] = "馬"; board[0][7] = "馬";
        board[0][2] = "象"; board[0][6] = "象";
        board[0][3] = "士"; board[0][5] = "士";
        board[0][4] = "將";
        board[2][1] = "炮"; board[2][7] = "炮";
        board[3][0] = "卒"; board[3][2] = "卒"; board[3][4] = "卒";
        board[3][6] = "卒"; board[3][8] = "卒";
        
        // 红方棋子
        board[9][0] = "车"; board[9][8] = "车";
        board[9][1] = "马"; board[9][7] = "马";
        board[9][2] = "相"; board[9][6] = "相";
        board[9][3] = "仕"; board[9][5] = "仕";
        board[9][4] = "帅";
        board[7][1] = "砲"; board[7][7] = "砲";
        board[6][0] = "兵"; board[6][2] = "兵"; board[6][4] = "兵";
        board[6][6] = "兵"; board[6][8] = "兵";
    }
    
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String playerColor;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                if (clients.size() == 1) {
                    playerColor = "红";
                    out.println("COLOR:红");
                } else if (clients.size() == 2) {
                    playerColor = "黑";
                    out.println("COLOR:黑");
                } else {
                    playerColor = "观战";
                    out.println("COLOR:观战");
                }
                
                sendBoardToAll();
                broadcastMessage("系统", playerColor + "方玩家已加入");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("MOVE:")) {
                        handleMove(message.substring(5));
                    } else if (message.startsWith("CHAT:")) {
                        String chatMsg = message.substring(5);
                        broadcastMessage(playerColor, chatMsg);
                    } else if (message.equals("GET_BOARD")) {
                        sendBoardToClient();
                    }
                }
            } catch (IOException e) {
                System.out.println("玩家断开连接");
            } finally {
                clients.remove(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        private void sendBoardToClient() {
            StringBuilder sb = new StringBuilder("BOARD:");
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 9; j++) {
                    sb.append(board[i][j]).append(",");
                }
            }
            sb.append(currentPlayer);
            out.println(sb.toString());
        }
        
        private void handleMove(String moveData) {
            if (gameEnded) {
                out.println("ERROR:游戏已结束，请等待重新开始!");
                return;
            }
            
            if (!playerColor.equals(currentPlayer)) {
                out.println("ERROR:不是你的回合!");
                return;
            }
            
            String[] parts = moveData.split(",");
            int fromRow = Integer.parseInt(parts[0]);
            int fromCol = Integer.parseInt(parts[1]);
            int toRow = Integer.parseInt(parts[2]);
            int toCol = Integer.parseInt(parts[3]);
            
            if (isValidMove(fromRow, fromCol, toRow, toCol)) {
                String capturedPiece = board[toRow][toCol];
                board[toRow][toCol] = board[fromRow][fromCol];
                board[fromRow][fromCol] = "  ";
                
                // 检查是否吃掉了将帅
                if (capturedPiece.equals("帅") || capturedPiece.equals("將")) {
                    gameEnded = true;
                    long gameTime = (System.currentTimeMillis() - gameStartTime) / 1000;
                    sendBoardToAll();
                    broadcastMessage("系统", playerColor + "方获胜!");
                    broadcastMessage("游戏结束", playerColor + "方吃掉了对方的" + 
                                   (capturedPiece.equals("帅") ? "帅" : "将") + 
                                   "，用时" + gameTime + "秒");
                    
                    // 3秒后重新开始游戏
                    new Thread(() -> {
                        try {
                            Thread.sleep(3000);
                            resetGame();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    return;
                }
                
                // 检查是否将军
                if (isInCheck(currentPlayer.equals("红") ? "黑" : "红")) {
                    broadcastMessage("系统", currentPlayer + "方被将军!");
                }
                
                currentPlayer = currentPlayer.equals("红") ? "黑" : "红";
                sendBoardToAll();
                broadcastMessage("系统", playerColor + "方移动了棋子");
            } else {
                out.println("ERROR:无效的移动!");
            }
        }
        
        private boolean isInCheck(String color) {
                // 找到将帅的位置
                int kingRow = -1, kingCol = -1;
                String kingChar = color.equals("红") ? "帅" : "將";
                
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 9; j++) {
                        if (board[i][j].equals(kingChar)) {
                            kingRow = i;
                            kingCol = j;
                            break;
                        }
                    }
                    if (kingRow != -1) break;
                }
                
                if (kingRow == -1) return false;
                
                // 检查是否有对方棋子可以攻击将帅
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 9; j++) {
                        String piece = board[i][j];
                                            if (!piece.equals("  ")) {
                                                boolean pieceIsRed = "车马相仕帅砲兵".contains(piece);
                                                boolean kingIsRed = color.equals("红");
                                                
                                                if (pieceIsRed != kingIsRed) {                                // 临时保存当前移动规则检查方法的状态
                                if (isValidMove(i, j, kingRow, kingCol)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                
                return false;
            }        
        private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
            if (fromRow < 0 || fromRow >= 10 || fromCol < 0 || fromCol >= 9) return false;
            if (toRow < 0 || toRow >= 10 || toCol < 0 || toCol >= 9) return false;
            if (board[fromRow][fromCol].equals("  ")) return false;
            
            String piece = board[fromRow][fromCol];
            String targetPiece = board[toRow][toCol];
            
            if (!targetPiece.equals("  ")) {
                boolean fromIsRed = "车马相仕帅砲兵".contains(piece);
                boolean toIsRed = "车马相仕帅砲兵".contains(targetPiece);
                if (fromIsRed == toIsRed) return false;
            }
            
            // 根据棋子类型判断移动规则
            switch (piece) {
                case "车":
                case "車":
                    return isValidRookMove(fromRow, fromCol, toRow, toCol);
                case "马":
                case "馬":
                    return isValidKnightMove(fromRow, fromCol, toRow, toCol);
                case "相":
                case "象":
                    return isValidElephantMove(fromRow, fromCol, toRow, toCol, piece.equals("相"));
                case "仕":
                case "士":
                    return isValidAdvisorMove(fromRow, fromCol, toRow, toCol, piece.equals("仕"));
                case "帅":
                case "將":
                    return isValidKingMove(fromRow, fromCol, toRow, toCol, piece.equals("帅"));
                case "炮":
                case "砲":
                    return isValidCannonMove(fromRow, fromCol, toRow, toCol);
                case "兵":
                case "卒":
                    return isValidPawnMove(fromRow, fromCol, toRow, toCol, piece.equals("兵"));
                default:
                    return false;
            }
        }
        
        private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
            // 车走直线
            if (fromRow != toRow && fromCol != toCol) return false;
            
            // 检查路径上是否有棋子
            if (fromRow == toRow) {
                int start = Math.min(fromCol, toCol) + 1;
                int end = Math.max(fromCol, toCol);
                for (int col = start; col < end; col++) {
                    if (!board[fromRow][col].equals("  ")) return false;
                }
            } else {
                int start = Math.min(fromRow, toRow) + 1;
                int end = Math.max(fromRow, toRow);
                for (int row = start; row < end; row++) {
                    if (!board[row][fromCol].equals("  ")) return false;
                }
            }
            return true;
        }
        
        private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
            // 马走日字
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);
            
            if (!((rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2))) {
                return false;
            }
            
            // 检查马脚
            if (rowDiff == 2) {
                int blockRow = fromRow + (toRow - fromRow) / 2;
                if (!board[blockRow][fromCol].equals("  ")) return false;
            } else {
                int blockCol = fromCol + (toCol - fromCol) / 2;
                if (!board[fromRow][blockCol].equals("  ")) return false;
            }
            return true;
        }
        
        private boolean isValidElephantMove(int fromRow, int fromCol, int toRow, int toCol, boolean isRed) {
            // 相/象走田字，不能过河
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);
            
            if (rowDiff != 2 || colDiff != 2) return false;
            
            // 不能过河
            if (isRed && toRow < 5) return false;
            if (!isRed && toRow > 4) return false;
            
            // 检查象眼
            int midRow = (fromRow + toRow) / 2;
            int midCol = (fromCol + toCol) / 2;
            if (!board[midRow][midCol].equals("  ")) return false;
            
            return true;
        }
        
        private boolean isValidAdvisorMove(int fromRow, int fromCol, int toRow, int toCol, boolean isRed) {
            // 仕/士走斜线，只能在九宫格内
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);
            
            if (rowDiff != 1 || colDiff != 1) return false;
            
            // 检查九宫格范围
            if (isRed) {
                if (toRow < 7 || toCol < 3 || toCol > 5) return false;
            } else {
                if (toRow > 2 || toCol < 3 || toCol > 5) return false;
            }
            
            return true;
        }
        
        private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol, boolean isRed) {
            // 帅/将走一步，只能在九宫格内
            int rowDiff = Math.abs(toRow - fromRow);
            int colDiff = Math.abs(toCol - fromCol);
            
            if (rowDiff + colDiff != 1) return false;
            
            // 检查九宫格范围
            if (isRed) {
                if (toRow < 7 || toCol < 3 || toCol > 5) return false;
            } else {
                if (toRow > 2 || toCol < 3 || toCol > 5) return false;
            }
            
            // 检查将帅对面
            String targetPiece = board[toRow][toCol];
            if ((targetPiece.equals("帅") && !isRed) || (targetPiece.equals("將") && isRed)) {
                // 将帅对面，检查中间是否有棋子
                if (fromCol == toCol) {
                    int start = Math.min(fromRow, toRow) + 1;
                    int end = Math.max(fromRow, toRow);
                    for (int row = start; row < end; row++) {
                        if (!board[row][fromCol].equals("  ")) return false;
                    }
                    return true;
                }
            }
            
            return true;
        }
        
        private boolean isValidCannonMove(int fromRow, int fromCol, int toRow, int toCol) {
            // 炮走直线
            if (fromRow != toRow && fromCol != toCol) return false;
            
            int pieceCount = 0;
            
            // 计算路径上的棋子数量
            if (fromRow == toRow) {
                int start = Math.min(fromCol, toCol) + 1;
                int end = Math.max(fromCol, toCol);
                for (int col = start; col < end; col++) {
                    if (!board[fromRow][col].equals("  ")) pieceCount++;
                }
            } else {
                int start = Math.min(fromRow, toRow) + 1;
                int end = Math.max(fromRow, toRow);
                for (int row = start; row < end; row++) {
                    if (!board[row][fromCol].equals("  ")) pieceCount++;
                }
            }
            
            // 如果目标位置有棋子，需要隔一个棋子吃子；否则路径必须畅通
            if (!board[toRow][toCol].equals("  ")) {
                return pieceCount == 1;
            } else {
                return pieceCount == 0;
            }
        }
        
        private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol, boolean isRed) {
            // 兵/卒的移动规则
            int rowDiff = toRow - fromRow;
            int colDiff = Math.abs(toCol - fromCol);
            
            if (isRed) {
                // 红兵向上走
                if (fromRow > 4) {
                    // 未过河，只能向前
                    return rowDiff == -1 && colDiff == 0;
                } else {
                    // 过河后可以向前或左右
                    return (rowDiff == -1 && colDiff == 0) || 
                           (rowDiff == 0 && colDiff == 1);
                }
            } else {
                // 黑卒向下走
                if (fromRow < 5) {
                    // 未过河，只能向前
                    return rowDiff == 1 && colDiff == 0;
                } else {
                    // 过河后可以向前或左右
                    return (rowDiff == 1 && colDiff == 0) || 
                           (rowDiff == 0 && colDiff == 1);
                }
            }
        }
        
        private void sendBoardToAll() {
            StringBuilder sb = new StringBuilder("BOARD:");
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 9; j++) {
                    sb.append(board[i][j]).append(",");
                }
            }
            sb.append(currentPlayer);
            
            for (ClientHandler client : clients) {
                client.out.println(sb.toString());
            }
        }
        
        private void broadcastMessage(String sender, String msg) {
            String chatMessage = "CHAT:" + sender + ": " + msg;
            for (ClientHandler client : clients) {
                client.out.println(chatMessage);
            }
        }
    }
    
    private static void resetGame() {
        initBoard();
        currentPlayer = "红";
        gameStartTime = System.currentTimeMillis();
        gameEnded = false;
        System.out.println("新游戏开始，红方先走");
        
        // 向所有客户端发送更新的棋盘状态
        StringBuilder sb = new StringBuilder("BOARD:");
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                sb.append(board[i][j]).append(",");
            }
        }
        sb.append(currentPlayer);
        
        for (ClientHandler client : clients) {
            client.out.println(sb.toString());
        }
        
        // 广播重新开始消息
        String chatMessage = "CHAT:系统: 新游戏开始！红方先走。";
        for (ClientHandler client : clients) {
            client.out.println(chatMessage);
        }
    }
}