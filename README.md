# 中国象棋网络版

一个功能完整的Java网络中国象棋游戏，集成了Socket编程、数据库持久化、玩家管理、排行榜系统以及实时语音聊天功能。

## ✨ 功能特性

### 核心游戏功能
- 🎮 **完整的中国象棋规则**：包含所有棋子和移动规则，服务器端进行移动校验。
- 🌐 **网络对战**：支持TCP/IP网络连接，可实现远程对战。
- 🎨 **图形界面**：使用Swing开发的直观棋盘界面，支持窗口缩放，为黑方玩家自动旋转棋盘。
- 👥 **多人支持**：服务器采用单局模式，前两个连接的客户端为红黑玩家，后续客户端为观战者。
- ⏱️ **游戏计时**：实时显示对局用时。
- 💬 **实时聊天**：游戏过程中可以与所有玩家（包括观战者）进行文本交流。
- 🎤 **语音聊天**：对战双方可通过“按键说话”进行实时语音交流。

### 数据库与玩家系统
- 👤 **玩家系统**：支持用户注册、登录及账户管理。
- 🗄️ **数据持久化**：使用SQLite数据库持久化存储玩家信息和对局记录。
- 📊 **排行榜系统**：实时全球排名，按胜场数和胜率排序。
- 📝 **对局记录**：自动保存每场对局信息，包括双方玩家、胜负结果、对局时长。
- 📈 **玩家统计**：自动追踪玩家的胜负数、胜率和详细对局历史。

## 🎯 项目结构

```
ChineseChess/
├── src/
│   ├── ChessServer.java          # 服务器主程序 (多线程, 监听8888端口)
│   ├── ChessClient.java          # 客户端主程序 (Swing GUI)
│   ├── ChessDatabase.java        # 数据库管理类 (SQLite & JDBC)
│   ├── LoginDialog.java          # 登录/注册对话框
│   ├── LeaderboardPanel.java     # 排行榜/战绩面板
│   ├── PlayerInfo.java           # 玩家信息模型类
│   ├── GameRecord.java           # 对局记录模型类
│   └── VoiceManager.java         # 语音聊天管理
├── lib/
│   └── sqlite-jdbc-3.40.0.0.jar  # SQLite JDBC驱动
├── build/                        # 编译输出目录
├── test.sh                       # 编译和运行脚本
├── chinesechess.db               # SQLite数据库文件 (自动生成)
└── README.md                     # 项目说明文档
```

## 📦 运行环境要求

- **Java 8+**
- **SQLite JDBC 驱动** (已包含在 `lib/` 目录中)

## 🚀 快速开始

### 方式1：使用脚本启动 (Linux/MacOS)

此脚本会自动编译所有`.java`文件，启动一个服务器实例，然后启动三个客户端实例（一个红方，一个黑方，一个观战者）。

```bash
# 导航到项目根目录
cd /path/to/ChineseChess

# 赋予脚本执行权限
chmod +x test.sh

# 运行脚本
./test.sh
```

### 方式2：手动启动

#### 第1步：编译

```bash
# 导航到项目根目录
cd /path/to/ChineseChess

# 编译所有Java文件
javac -cp "lib/*" -d build src/*.java
```

#### 第2步：启动服务器

服务器将在 **8888** 端口启动，并等待客户端连接。

```bash
java -cp "build:lib/*" src.ChessServer
```

#### 第3步：启动客户端（在新终端中）

可以启动多个客户端实例进行测试。

```bash
java -cp "build:lib/*" src.ChessClient
```

## 🎮 游戏说明

### 用户流程

1.  **启动客户端** → 自动弹出登录对话框。
    -   *注*：若使用 `test.sh` 脚本，会启动3个客户端窗口，可独立登录。
2.  **选择操作**：
    -   **注册新账户**：输入未注册的用户名和密码，点击"Register"。
    -   **登录已有账户**：输入已注册的用户名和密码，点击"Login"。
3.  **进入游戏**：
    -   前两个登录的玩家被分配为红方和黑方，开始对战。
    -   后续登录的玩家将作为观战者加入游戏。
4.  **查看排行榜**：点击菜单栏 "游戏" → "查看排行榜"。

### 操作方式

-   **移动棋子**：点击己方棋子（选中后有黄色边框），再点击目标位置完成移动。
-   **发送消息**：在右侧聊天窗口输入消息并按回车发送。
-   **语音聊天**：按住 "按键说话" 按钮进行实时语音交流 (仅对战双方)。

## 🔧 技术实现

### 架构设计

项目采用经典的客户端/服务器（C/S）架构，服务器为中心节点，处理所有核心逻辑。

```
                                  ┌──────────────────┐
                                  │   ChessServer    │
                            ┌─────│  (多线程, 单局)    │<────┐
                            │     │  游戏逻辑, 状态同步  │     │
                            │     └────────┬─────────┘     │
                            │              │               │
                         Socket         Socket           Socket
                      (TCP, Port 8888)     │          (TCP, Port 8888)
                            │              │               │
  ┌─────────────────┐       │       ┌────────────────┐   ┌─────────────────┐
  │  ChessClient    │◄──────┘       │ ChessDatabase  │   │  ChessClient    │
  │  (红方)         │               │ (SQLite, JDBC) │   │  (黑方)         │
  └─────────────────┘               └────────────────┘   └─────────────────┘
                                                           (棋盘自动旋转)
```

### 通信协议

通信协议是基于文本的，通过TCP Socket进行，消息格式为 `命令:参数`。

#### 客户端 → 服务器
| 命令 | 格式 | 说明 |
| :--- | :--- | :--- |
| `LOGIN` | `LOGIN:username,password` | 玩家登录请求 |
| `MOVE` | `MOVE:fromRow,fromCol,toRow,toCol` | 移动棋子 |
| `CHAT` | `CHAT:message` | 发送聊天消息 |
| `GET_BOARD`| `GET_BOARD` | 请求完整棋盘状态 |
| `VOICE` | `VOICE:base64_encoded_data` | 发送语音数据 |

#### 服务器 → 客户端
| 命令 | 格式 | 说明 |
| :--- | :--- | :--- |
| `COLOR` | `COLOR:red/black/观战` | 分配玩家颜色/角色 |
| `BOARD` | `BOARD:board_data,current_player` | 广播棋盘状态和当前回合方 |
| `CHAT` | `CHAT:message` | 广播聊天消息 |
| `VOICE` | `VOICE:base64_encoded_data` | 转发语音数据给对手 |
| `TIME` | `TIME:MM:SS` | 广播游戏计时 |
| `LOGIN_OK`| `LOGIN_OK` | 登录成功响应 |
| `LOGIN_FAILED`| `LOGIN_FAILED:reason` | 登录失败响应 |
| `ERROR` | `ERROR:message` | 发送错误/提示信息 |


## 💾 数据库设计

数据库文件 `chinesechess.db` 会在服务器首次启动时在项目根目录自动创建。

### `players` 表 - 玩家信息

存储用户账户和统计数据。

```sql
CREATE TABLE players (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,          -- 密码明文存储
  total_games INTEGER DEFAULT 0,   -- 总对局数
  wins INTEGER DEFAULT 0,          -- 胜场数
  losses INTEGER DEFAULT 0,        -- 负场数
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_played_at TIMESTAMP
);
```

### `game_records` 表 - 对局记录

存储已完成的游戏历史。

```sql
CREATE TABLE game_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  red_player_id INTEGER,
  red_player_name TEXT,
  black_player_id INTEGER,
  black_player_name TEXT,
  winner_id INTEGER,
  winner_name TEXT,
  game_duration INTEGER,      -- 对局时长（秒）
  start_time TIMESTAMP,
  end_time TIMESTAMP
);
```

## 🔐 安全与健壮性

-   **SQL注入防护**：所有数据库查询均使用 `PreparedStatement` 防止SQL注入。
-   **并发处理**：服务器为每个客户端创建一个独立的 `ClientHandler` 线程进行管理。
-   **异常处理**：代码中包含了对网络和数据库操作的异常捕获。
-   **资源管理**：数据库连接和网络套接字在使用完毕后会正确关闭，防止资源泄漏。

## 🎯 后续扩展方向

-   [ ] **密码安全**：使用哈希算法 (如 BCrypt) 加密存储用户密码。
-   [ ] **多局支持**：重构服务器以支持多场游戏同时进行 (例如，为每场游戏创建一个独立的游戏室对象)。
-   [ ] **对局回放**：记录每一步棋的移动，实现游戏复盘功能。
-   [ ] **悔棋与和棋**：在UI和协议中加入悔棋与和棋请求的功能。
-   [ ] **AI 对手**：集成一个简单的象棋AI，允许玩家进行人机对战。
