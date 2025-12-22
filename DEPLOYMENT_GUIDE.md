# 中国象棋网络版 - 部署和运行指南

本文档说明如何在其他电脑上部署和运行这个项目。

## 📋 前置要求

### 必须安装
1. **Java 8+** (推荐 Java 11 或更高)
2. **git** (用于克隆项目) - 可选，也可手动下载

### 验证环境

在终端/命令行中运行：

```bash
# 检查Java版本
java -version

# 输出应该显示 Java 8 或更高版本
# 例如：openjdk version "11.0.13"
```

如果显示 "command not found"，说明需要先安装Java。

## 📥 获取项目代码

### 方式1：使用 Git 克隆（推荐）

```bash
# 克隆项目
git clone https://github.com/ccycyc6/javabig.git

# 进入项目目录
cd javabig/ChineseChess
```

### 方式2：手动下载

1. 访问：https://github.com/ccycyc6/javabig
2. 点击 "Code" → "Download ZIP"
3. 解压到本地
4. 进入 `ChineseChess` 目录

## 🚀 快速运行

### 步骤1：进入项目目录

```bash
cd ChineseChess
```

### 步骤2：编译项目

```bash
# Linux/Mac
javac -cp "lib/*" -d build src/*.java

# Windows (PowerShell)
javac -cp "lib\*" -d build src\*.java

# Windows (cmd)
javac -cp "lib\*" -d build src\*.java
```

**说明**：
- 这会编译所有 Java 源文件
- `-cp "lib/*"` 指定类路径，包含 SQLite JDBC 驱动
- `-d build` 将编译后的 .class 文件放在 build 文件夹

编译成功后应该看到：
```
✓ 编译成功（无错误提示）
```

### 步骤3：启动服务器

**在一个终端窗口中运行**：

```bash
# Linux/Mac
java -cp "build:lib/*" src.ChessServer

# Windows (PowerShell 或 cmd)
java -cp "build;lib\*" src.ChessServer
```

看到以下输出说明服务器启动成功：
```
象棋服务器启动，端口: 8888
数据库已初始化
```

**重要**：保持这个终端窗口打开！

### 步骤4：启动客户端

**在新的终端窗口中运行**：

```bash
# Linux/Mac
java -cp "build:lib/*" src.ChessClient

# Windows (PowerShell 或 cmd)
java -cp "build;lib\*" src.ChessClient
```

看到游戏窗口弹出，即表示客户端启动成功。

### 步骤5：使用游戏

1. **登录对话框出现**
   - 第一次使用：点击 "Register" 注册新账户
   - 已有账户：输入用户名密码，点击 "Login"

2. **输入服务器地址**
   - 本地测试：输入 `localhost` 或 `127.0.0.1`
   - 局域网：输入服务器电脑的 IP 地址
   - 互联网：输入服务器的公网 IP（需要端口转发）

3. **开始游戏**
   - 等待第二个玩家连接
   - 或点击菜单 "游戏" → "查看排行榜" 查看历史数据

## 🖥️ 多人联网运行

### 场景：两台电脑对战

#### 服务器电脑（Computer A）
```bash
# 进入项目目录
cd ChineseChess

# 启动服务器
java -cp "build:lib/*" src.ChessServer
```

记下服务器的 IP 地址：
- **Linux/Mac**：`ifconfig` 或 `ip addr`
- **Windows**：`ipconfig`

#### 客户端电脑（Computer B）
```bash
# 进入项目目录
cd ChineseChess

# 启动客户端
java -cp "build:lib/*" src.ChessClient
```

输入服务器电脑的 IP 地址（例如：`192.168.1.100`）

### 同一台电脑多个客户端

在同一电脑启动多个客户端窗口：

```bash
# 终端1：启动服务器
java -cp "build:lib/*" src.ChessServer

# 终端2：启动客户端1
java -cp "build:lib/*" src.ChessClient

# 终端3：启动客户端2
java -cp "build:lib/*" src.ChessClient

# 终端4：启动客户端3（观战）
java -cp "build:lib/*" src.ChessClient
```

所有客户端都连接到 `localhost`。

## 🔧 常见问题排查

### 问题1：找不到 Java 命令

**错误信息**：`command not found: java`

**解决方案**：
1. 安装 Java
2. 配置 Java 环境变量
3. 重启终端

### 问题2：编译错误 - 找不到 sqlite-jdbc

**错误信息**：`error: Package org.sqlite does not exist`

**解决方案**：
- 确保 `lib` 文件夹中有 `sqlite-jdbc-3.40.0.0.jar`
- 检查编译命令的 `-cp` 参数正确性
- 在项目根目录运行（不是在 src 子目录）

### 问题3：无法连接到服务器

**错误信息**：`无法连接到服务器！`

**解决方案**：
1. 确保服务器正在运行
2. 检查服务器地址是否正确
   - 本地：`localhost` 或 `127.0.0.1`
   - 远程：检查 IP 地址
3. 检查防火墙是否阻止 8888 端口

### 问题4：数据库文件冲突

**症状**：启动多个服务器实例

**解决方案**：
- 只能同时运行 **一个** 服务器
- 要停止服务器：在服务器终端按 `Ctrl+C`

### 问题5：Windows 编译文件名错误

**错误信息**：`找不到 src.ChessServer`

**解决方案**：
- Windows 命令行类路径分隔符是 `;` 不是 `:`
- 正确写法：`java -cp "build;lib\*" src.ChessServer`

## 📊 文件结构说明

第一次运行后，项目目录结构如下：

```
ChineseChess/
├── src/                          # 源代码目录
│   ├── ChessClient.java         # 客户端（约1000行）
│   ├── ChessServer.java         # 服务器（约500行）
│   ├── ChessDatabase.java       # 数据库管理（207行）
│   ├── LeaderboardPanel.java    # 排行榜UI（143行）
│   ├── LoginDialog.java         # 登录UI（110行）
│   ├── PlayerInfo.java          # 玩家模型（48行）
│   ├── GameRecord.java          # 对局模型（55行）
│   └── SoundManager.java        # 音效管理
│
├── build/                         # 编译输出（自动生成）
│   └── src/                      # .class 文件
│
├── lib/                           # 第三方库
│   └── sqlite-jdbc-3.40.0.0.jar # SQLite JDBC 驱动
│
├── chinesechess.db              # 数据库文件（自动生成）
│   # 包含所有玩家信息和对局记录
│
├── test.sh                      # 编译和运行脚本
├── README.md                    # 项目文档
└── .gitignore                  # Git 忽略文件
```

## 📝 作业提交清单

如果这是作业项目，提交时应该包括：

### ✅ 必须提交
- [ ] `src/` 目录中的所有 Java 源文件（7个文件）
- [ ] `lib/sqlite-jdbc-3.40.0.0.jar` （JDBC驱动）
- [ ] `README.md` （项目文档）
- [ ] `test.sh` （运行脚本）

### ✅ 可选提交（用于完整性）
- [ ] `.gitignore` （告诉 git 忽略哪些文件）
- [ ] `build/` 目录（非必须，可自动编译生成）

### ❌ 不需要提交
- [ ] `chinesechess.db` （数据库文件，会自动生成）
- [ ] `*.class` 文件（编译后生成）
- [ ] IDE 项目文件（.idea/, .vscode/ 等）

### 🎓 建议的提交形式

```
项目名称：中国象棋网络游戏
文件结构：
  ChineseChess/
    ├── src/
    │   └── 7个Java文件
    ├── lib/
    │   └── sqlite-jdbc-3.40.0.0.jar
    ├── test.sh
    ├── README.md
    └── [其他文档]
```

## 🔐 网络安全建议

### 本地网络（推荐）
```
┌─────────────┐           ┌─────────────┐
│ Computer A  │ ◄────────► │ Computer B  │
│ (Server)    │  局域网    │ (Client)    │
└─────────────┘           └─────────────┘
    IP: 192.168.1.100        IP: 192.168.1.101
```

**安全**：局域网内部通信，相对安全

### 互联网（需谨慎）

如果要通过互联网运行：

1. **端口转发**（在路由器上配置）
   ```
   外网访问: 你的公网IP:8888
   转发到: 内网服务器:192.168.x.x:8888
   ```

2. **防火墙配置**
   - 仅允许必要的端口（8888）
   - 定期修改端口号
   - 监听连接日志

3. **考虑添加密码验证**
   - 当前版本没有身份验证
   - 建议在作业阶段只在本地/局域网使用

## 💡 调试技巧

### 查看服务器日志

在服务器控制台可以看到：
```
象棋服务器启动，端口: 8888
数据库已初始化
新玩家连接，当前玩家数: 1
新玩家连接，当前玩家数: 2
player1 已登录
player2 已登录
对局已保存到数据库
```

### 查看数据库内容

```bash
# 查看所有玩家
sqlite3 chinesechess.db "SELECT name, wins, losses FROM players;"

# 查看最近10场对局
sqlite3 chinesechess.db "SELECT red_player_name, black_player_name, winner_name FROM game_records LIMIT 10;"

# 查看排行榜
sqlite3 chinesechess.db "SELECT name, wins, losses, ROUND(wins*100.0/(wins+losses),1) FROM players ORDER BY wins DESC LIMIT 5;"
```

### 清空数据库（重新开始）

```bash
# 删除数据库文件（谨慎操作）
rm chinesechess.db

# 下次启动服务器时会自动创建新的空数据库
```

## 📱 使用 test.sh 自动化

项目包含 `test.sh` 脚本，可以一键启动：

```bash
bash test.sh
```

该脚本会自动：
1. 清理旧的编译文件
2. 编译所有源文件
3. 启动服务器
4. 启动3个客户端
5. 显示所有进程 PID
6. 等待用户按任意键后关闭所有进程

## 🎓 教学要点总结

这个项目涉及的核心技能：

| 技术 | 文件位置 | 说明 |
|------|--------|------|
| **Socket 网络编程** | ChessServer.java | TCP/IP 通信、多线程处理 |
| **Swing GUI** | ChessClient.java | 用户界面、事件处理 |
| **数据库** | ChessDatabase.java | SQLite、JDBC、SQL |
| **并发编程** | ChessServer.java | 多线程、线程安全 |
| **面向对象** | PlayerInfo.java 等 | 类设计、模块化 |
| **游戏逻辑** | ChessServer.java | 规则实现、状态管理 |

## 📞 常见的提问和回答

**Q: 需要配置 Maven/Gradle 吗？**
A: 不需要。项目使用传统的 Java 编译方式，JDBC 驱动已经包含在 lib 文件夹中。

**Q: 可以修改端口号吗？**
A: 可以。在 ChessServer.java 中修改 `private static final int PORT = 8888;` 为其他端口。

**Q: 可以添加更多功能吗？**
A: 完全可以。项目架构清晰，易于扩展。可以添加密码加密、棋谱回放等功能。

**Q: 数据安全吗？**
A: 本地 SQLite 数据库相对安全。如果需要更高安全性，可升级到 MySQL/PostgreSQL。

**Q: 支持录制回放吗？**
A: 当前版本不支持。但保存了所有对局记录，可以后续实现棋谱回放功能。

---

**最后提醒**：
- 确保 Java 版本正确 ✓
- 确保 lib 文件夹中有 JDBC 驱动 ✓  
- 确保只运行一个服务器实例 ✓
- 保持项目文件夹结构完整 ✓

祝运行顺利！🎮✨
