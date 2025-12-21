import java.io.*;
import java.net.*;
import java.util.*;

public class ChessServer {
    private static final int PORT = 8888;
    private static List<ClientHandler> clients = new ArrayList<>();
    private static String[][] board = new String[10][9];
    private static String currentPlayer = "红";
    
    public static void main(String[] args) {
        initBoard();
        System.out.println("象棋服务器启动，端口: " + PORT);
        
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
        board[7][1] = "炮"; board[7][7] = "炮";
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
        
        private void handleMove(String moveData) {
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
                board[toRow][toCol] = board[fromRow][fromCol];
                board[fromRow][fromCol] = "  ";
                
                currentPlayer = currentPlayer.equals("红") ? "黑" : "红";
                sendBoardToAll();
                broadcastMessage("系统", playerColor + "方移动了棋子");
            } else {
                out.println("ERROR:无效的移动!");
            }
        }
        
        private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
            if (fromRow < 0 || fromRow >= 10 || fromCol < 0 || fromCol >= 9) return false;
            if (toRow < 0 || toRow >= 10 || toCol < 0 || toCol >= 9) return false;
            if (board[fromRow][fromCol].equals("  ")) return false;
            
            String piece = board[fromRow][fromCol];
            String targetPiece = board[toRow][toCol];
            
            if (!targetPiece.equals("  ")) {
                boolean fromIsRed = "车马相仕帅炮兵".contains(piece);
                boolean toIsRed = "车马相仕帅炮兵".contains(targetPiece);
                if (fromIsRed == toIsRed) return false;
            }
            
            return true;
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
}