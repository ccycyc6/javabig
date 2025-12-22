# 🚀 快速开始卡片

## 对他人来说的三步启动

### 步骤1️⃣：编译
```bash
cd ChineseChess
javac -cp "lib/*" -d build src/*.java
```

### 步骤2️⃣：启动服务器（在终端1中）
```bash
java -cp "build:lib/*" src.ChessServer
```

### 步骤3️⃣：启动客户端（在新的终端2中）
```bash
java -cp "build:lib/*" src.ChessClient
```

---

## 💻 Windows 用户注意

```bash
# 编译（Windows）
javac -cp "lib\*" -d build src\*.java

# 启动服务器（Windows）
java -cp "build;lib\*" src.ChessServer

# 启动客户端（Windows）
java -cp "build;lib\*" src.ChessClient
```

**关键点**：Windows 用路径分隔符是 `;` 而不是 `:`

---

## 📋 项目包含文件

```
✓ src/ChessClient.java          （客户端）
✓ src/ChessServer.java          （服务器）
✓ src/ChessDatabase.java        （数据库）
✓ src/LeaderboardPanel.java     （排行榜UI）
✓ src/LoginDialog.java          （登录UI）
✓ src/PlayerInfo.java           （玩家模型）
✓ src/GameRecord.java           （对局模型）
✓ lib/sqlite-jdbc-3.40.0.0.jar  （JDBC驱动）
✓ README.md                     （完整文档）
✓ DEPLOYMENT_GUIDE.md           （部署指南）
```

---

## 🎮 使用流程

1. **服务器启动后** → 显示 "象棋服务器启动，端口: 8888"
2. **客户端启动后** → 输入服务器地址
   - 本地：`localhost` 或 `127.0.0.1`
   - 远程：服务器IP地址（如 `192.168.1.100`）
3. **登录对话框** → 注册或登录
4. **游戏开始** → 等待对手或查看排行榜

---

## ⚠️ 常见问题快速答案

| 问题 | 答案 |
|------|------|
| Java 找不到 | 需要安装 Java 8+ |
| SQLite 驱动找不到 | 确保 lib/ 文件夹中有 jar 文件 |
| 无法连接服务器 | 检查服务器是否在运行，地址是否正确 |
| Windows 编译失败 | 检查路径分隔符：`;` 不是 `:` |
| 数据丢失 | 数据在 chinesechess.db 中，永久保存 |

---

## 📞 完整文档位置

- **README.md** - 完整项目说明
- **DEPLOYMENT_GUIDE.md** - 详细部署和常见问题

---

**记住**：
- ✅ 同时只能运行**一个**服务器
- ✅ 可以运行**多个**客户端
- ✅ 数据**永久保存**在 chinesechess.db 中
- ✅ 关闭程序后数据**不会丢失**
