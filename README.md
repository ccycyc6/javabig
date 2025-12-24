# 中国象棋网络版

一个功能完整的Java网络中国象棋游戏，集成了Socket编程、数据库持久化、玩家管理和排行榜系统。

## ✨ 功能特性

### 核心游戏功能
- 🎮 **完整的中国象棋规则**：包含所有棋子和移动规则
- 🌐 **网络对战**：支持TCP/IP网络连接，可实现远程对战
- 💬 **实时聊天**：游戏过程中可以与对手交流
- 🎨 **图形界面**：使用Swing开发的直观棋盘界面
- 👥 **多人支持**：支持2人对战和观战模式
- ⏱️ **游戏计时**：实时显示对局用时

### 数据库功能
- 👤 **玩家系统**：用户注册、登录、账户管理
- 📊 **排行榜系统**：实时全球排名，按胜率和胜场数排序
- 📝 **对局记录**：自动保存每场对局信息，包括双方玩家、胜负结果、对局时长
- 📈 **玩家统计**：自动追踪胜负数、胜率和对局历史
- 🗄️ **SQLite数据库**：轻量级本地数据库，支持数据持久化

## 🎯 项目结构

```
ChineseChess/
├── src/
│   ├── ChessClient.java          # 客户端主程序，包含GUI和登录功能
│   ├── ChessServer.java          # 服务器主程序，处理游戏逻辑和数据库操作
│   ├── PlayerInfo.java           # 玩家信息模型类
│   ├── GameRecord.java           # 对局记录模型类
│   ├── ChessDatabase.java        # 数据库管理类（SQLite）
│   ├── LoginDialog.java          # 登录注册对话框
│   └── LeaderboardPanel.java     # 排行榜和战绩面板
├── lib/
│   └── sqlite-jdbc-3.40.0.0.jar  # SQLite JDBC驱动
├── build/                         # 编译输出目录
├── test.sh                       # 编译和运行脚本
├── chinesechess.db              # SQLite数据库文件（自动生成）
└── README.md                    # 项目说明文档
```

## 📦 运行环境要求

- **Java 8+** 或更高版本
- **SQLite JDBC 驱动** （已包含在 lib 目录）
- TCP/IP 网络连接支持

## 🚀 快速开始

### 方式1：使用脚本启动（推荐）(Linux/MacOS)

```bash
cd /home/ccy/Documents/javabig/ChineseChess
bash test.sh   or    ./test.sh
```

脚本会自动编译、启动服务器和多个客户端实例。

### 方式2：手动启动

#### 第1步：编译

```bash
cd /home/ccy/Documents/javabig/ChineseChess
javac -cp "lib/*" -d build src/*.java
```

#### 第2步：启动服务器

```bash
java -cp "build:lib/*" src.ChessServer
```

服务器将在 **8888** 端口启动，等待客户端连接。

#### 第3步：启动客户端（新终端）

```bash
java -cp "build:lib/*" src.ChessClient
```

输入服务器地址（本地测试使用 `localhost`）即可连接。

## 🎮 游戏说明

### 用户流程

1. **启动客户端** → 自动弹出登录对话框 (注意：运行test.sh脚本会启动3个客户端窗口,每个窗口可独立登录，分别代表黑方、红方和观战者,最上层的窗口为观战者)
2. **选择操作**：
   - 注册新账户：输入用户名密码并点击"Register"
   - 已有账户：输入用户名密码并点击"Login"
3. **进入游戏** → 与对手对战
4. **查看排行榜** → 点击菜单栏"游戏" → "查看排行榜"

### 操作方式

1. **选择棋子**：点击己方棋子进行选择（选中后会有黄色边框）
2. **移动棋子**：选择棋子后点击目标位置完成移动
3. **发送消息**：在右侧聊天窗口输入消息并发送
4. **查看排行榜**：点击菜单栏"游戏" → "查看排行榜"，可查看全球排名和个人战绩

### 游戏规则

- ♟️ **红方先手**，双方轮流移动
- 🎯 **每次一个棋子**，遵循标准中国象棋移动规则
- 🏆 **获胜条件**：吃掉对方"帅"或"将"即获胜
- 📊 **自动记录**：游戏结束后对局自动保存到数据库

## 💾 数据库设计

### 自动创建的表

#### players 表 - 玩家信息
```sql
CREATE TABLE players (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT UNIQUE NOT NULL,
  password TEXT NOT NULL,
  total_games INTEGER DEFAULT 0,    -- 总对局数
  wins INTEGER DEFAULT 0,            -- 胜场数
  losses INTEGER DEFAULT 0,          -- 负场数
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_played_at TIMESTAMP
)
```

#### game_records 表 - 对局记录
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
)
```

## 🔧 技术实现

### 架构设计

```
┌─────────────────┐         ┌──────────────────┐
│  ChessClient    │◄───────►│   ChessServer    │
│  (Swing GUI)    │ Socket  │ (多线程处理)       │
│  登录/排行榜      │         │ 游戏逻辑          │
└─────────────────┘         └────────┬─────────┘
                                     │
                                     ▼
                            ┌──────────────────┐
                            │ ChessDatabase    │
                            │ (SQLite JDBC)    │
                            │ 数据持久化         │
                            └──────────────────┘
```

### 核心模块

| 模块 | 说明 | 主要类 |
|------|------|--------|
| **游戏逻辑** | 棋盘状态、移动规则、胜负判定 | ChessServer, ChessClient |
| **网络通信** | Socket通信、消息协议 | ChessServer, ClientHandler, ChessClient |
| **数据持久化** | 数据库操作、SQL执行 | ChessDatabase |
| **用户管理** | 注册、登录、玩家信息 | LoginDialog, PlayerInfo |
| **排行榜系统** | 排名显示、战绩统计 | LeaderboardPanel, GameRecord |
| **用户界面** | 棋盘显示、聊天窗口 | ChessBoardPanel, 各UI组件 |

### 通信协议

```
客户端 → 服务器：
  LOGIN:playerName                  # 玩家登录
  MOVE:fromRow,fromCol,toRow,toCol  # 移动棋子
  CHAT:message                      # 发送聊天消息
  GET_BOARD                         # 请求棋盘状态

服务器 → 客户端：
  COLOR:red/black/观战               # 分配玩家颜色
  BOARD:棋盘数据,当前玩家              # 棋盘状态
  TIME:MM:SS                        # 对局计时
  CHAT:message                      # 广播消息
  LOGIN_OK/LOGIN_FAILED             # 登录响应
  ERROR:错误信息                     # 错误提示
```

## 📊 排行榜特性

### 排序规则
1. **胜场数降序**：胜场多的玩家排名靠前
2. **胜率降序**：胜场相同时，胜率高的排名靠前
3. **最多显示** 20 个玩家

### 查看功能
- **全球排行榜**：实时显示前20名玩家及其排名、胜负场数、胜率
- **个人战绩**：显示当前玩家最近10场对局
- **即时刷新**：支持刷新按钮实时更新数据

## 🔐 安全特性

- ✅ **SQL注入防护**：使用PreparedStatement防止SQL注入
- ✅ **异常处理**：完整的异常捕获和错误提示
- ✅ **资源管理**：数据库连接正确关闭，避免内存泄漏
- ✅ **并发控制**：服务器多线程安全处理

## 📝 使用示例

### 示例1：查看玩家排行榜

```
游戏菜单 → 查看排行榜
├─ 全球排行榜（前20名）
│  ├─ 排名1: Alice - 胜: 45  负: 10  胜率: 81.8%
│  ├─ 排名2: Bob   - 胜: 42  负: 15  胜率: 73.7%
│  └─ 排名3: Charlie - 胜: 38  负: 18  胜率: 67.9%
└─ 个人战绩（最近10场）
   ├─ 对局时间: Alice   我的角色: Bob     对局结果: Alice  对手: 342秒  对局时长: 14:30
   └─ ...
```

### 示例2：游戏结束自动保存

```
游戏进行中...
对局结束：Alice 击败 Bob（用时: 618秒）

自动执行：
✓ 保存对局记录到数据库
✓ 更新Alice的胜场数 (wins + 1)
✓ 更新Bob的负场数 (losses + 1)
✓ 计算双方新胜率
✓ 重新排序全球排行榜
```

## 🎓 代码统计

| 文件 | 行数 | 功能 |
|------|------|------|
| ChessClient.java | 1000+ | 客户端GUI和游戏逻辑 |
| ChessServer.java | 500+ | 服务器和对局管理 |
| ChessDatabase.java | 207 | 数据库操作 |
| LeaderboardPanel.java | 143 | 排行榜UI |
| LoginDialog.java | 110 | 登录注册UI |
| PlayerInfo.java | 48 | 玩家数据模型 |
| GameRecord.java | 55 | 对局记录模型 |

**总计：2000+ 行代码**

## 🔄 工作流程图

```
用户启动客户端
    ↓
显示登录对话框
    ├─ 新用户：注册账户
    └─ 已有用户：登录
    ↓
连接到游戏服务器
    ↓
分配玩家颜色（红/黑/观战）
    ↓
显示棋盘，游戏开始
    ├─ 玩家对战
    ├─ 实时计时
    └─ 实时聊天
    ↓
游戏结束（一方被将死）
    ↓
自动保存对局：
  ├─ 双方玩家信息
  ├─ 胜负结果
  ├─ 对局时长
  └─ 对局时间
    ↓
更新排行榜：
  ├─ 更新胜负数
  ├─ 计算胜率
  └─ 重新排序
    ↓
3秒后自动重新开始游戏
```

## 🎯 后续扩展方向

### 短期优化
- [ ] 密码加密存储（BCrypt）
- [ ] 离线消息保存
- [ ] 对局回放功能
- [ ] 玩家头像支持

### 中期功能
- [ ] 玩家段位系统
- [ ] 成就和徽章
- [ ] 对局评分分析
- [ ] 数据导出功能

### 长期规划
- [ ] 升级到 MySQL/PostgreSQL
- [ ] 云端数据同步
- [ ] 移动端 App 支持
- [ ] AI 对手集成

## 📖 常见问题

### Q: 数据库文件在哪里？
**A**: 数据库文件 `chinesechess.db` 会在服务器首次启动时自动创建在项目根目录。

### Q: 忘记密码怎么办？
**A**: 当前版本无密码恢复功能，请使用新账户注册。

### Q: 能支持多少并发玩家？
**A**: 当前实现支持多个玩家同时连接，只要服务器资源允许。

### Q: 数据库能升级吗？
**A**: 可以，ChessDatabase 类使用标准 JDBC，可轻松升级到 MySQL 或 PostgreSQL。


---

**项目完成时间**: 2024年12月22日
**技术栈**: Java 8+ | Swing | SQLite | Socket | JDBC  
## 开发约定

*   **代码风格**: 代码遵循标准的Java编码规范。
*   **图形界面**: 图形界面使用Java Swing构建，UI组件结构清晰，用户体验良好。
*   **网络通信**: 客户端和服务器通过TCP/IP套接字使用简单的基于文本的协议进行通信。协议由一组命令（如 `LOGIN`, `MOVE`, `CHAT`等）定义。
*   **数据库**: 应用程序使用SQLite数据库来持久化数据。`ChessDatabase` 类封装了所有数据库操作，并使用 `PreparedStatement` 来防止SQL注入。
*   **测试**: `test.sh` 脚本提供了一个基本的集成测试，通过启动一个服务器和三个客户端来完成。