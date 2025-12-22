# 中国象棋网络版项目上下文

## 项目概述

这是一个功能完整的Java网络中国象棋游戏项目，采用客户端-服务器架构，集成了Socket编程、SQLite数据库持久化、玩家管理和排行榜系统。项目使用Java Swing开发图形界面，支持多人网络对战、实时聊天、游戏计时等功能。

## 技术栈

- **核心语言**: Java 8+
- **GUI框架**: Java Swing
- **网络通信**: Socket编程 (TCP/IP)
- **数据库**: SQLite (通过JDBC)
- **架构模式**: 客户端-服务器架构
- **并发处理**: 多线程服务器

## 项目结构

```
ChineseChess/
├── src/                        # 源代码目录
│   ├── ChessClient.java        # 客户端主程序（GUI、游戏逻辑）
│   ├── ChessServer.java        # 服务器主程序（多线程处理）
│   ├── ChessDatabase.java      # 数据库管理类
│   ├── PlayerInfo.java         # 玩家信息模型
│   ├── GameRecord.java         # 对局记录模型
│   ├── LoginDialog.java        # 登录注册对话框
│   └── LeaderboardPanel.java   # 排行榜面板
├── lib/                        # 依赖库
│   └── sqlite-jdbc-3.40.0.0.jar # SQLite JDBC驱动
├── build/                      # 编译输出目录
├── chinesechess.db            # SQLite数据库文件（自动生成）
├── test.sh                    # 自动化测试脚本
└── 文档文件/                   # 各种说明文档
```

## 核心功能模块

### 1. 游戏逻辑模块
- 完整的中国象棋规则实现
- 棋盘状态管理和移动验证
- 胜负判定逻辑
- 游戏计时功能

### 2. 网络通信模块
- TCP/IP Socket连接
- 多客户端并发处理
- 实时消息广播
- 客户端-服务器协议定义

### 3. 数据持久化模块
- SQLite数据库管理
- 玩家信息存储
- 对局记录保存
- 排行榜数据维护

### 4. 用户界面模块
- Swing图形界面
- 棋盘可视化
- 登录注册界面
- 排行榜显示
- 实时聊天功能

## 构建和运行

### 环境要求
- Java Development Kit (JDK) 8或更高版本
- SQLite JDBC驱动（已包含在lib目录）

### 编译命令
```bash
# Linux/Mac
javac -cp "lib/*" -d build src/*.java

# Windows (注意路径分隔符)
javac -cp "lib\*" -d build src\*.java
```

### 运行命令

#### 启动服务器
```bash
# Linux/Mac
java -cp "build:lib/*" src.ChessServer

# Windows
java -cp "build;lib\*" src.ChessServer
```

#### 启动客户端
```bash
# Linux/Mac
java -cp "build:lib/*" src.ChessClient

# Windows
java -cp "build;lib\*" src.ChessClient
```

### 自动化测试
```bash
bash test.sh
```
该脚本会自动编译并启动1个服务器和3个客户端实例进行测试。

## 开发约定

### 代码风格
- 使用标准Java命名约定
- 类名使用PascalCase
- 方法和变量使用camelCase
- 常量使用UPPER_SNAKE_CASE

### 包结构
- 所有类都在`src`包下
- 模型类以功能命名（如PlayerInfo、GameRecord）
- UI类包含其功能描述（如LoginDialog、LeaderboardPanel）

### 数据库约定
- 数据库文件：`chinesechess.db`
- 表名使用复数形式（players、game_records）
- 主键统一命名为`id`
- 时间戳字段使用`TIMESTAMP`类型

### 网络协议
- 客户端到服务器消息格式：`COMMAND:parameters`
- 服务器到客户端消息格式：`RESPONSE:data`
- 主要命令：LOGIN、MOVE、CHAT、GET_BOARD等

## 数据库设计

### players表
- `id`: 主键，自增
- `name`: 玩家名，唯一
- `password`: 密码（明文存储，待改进）
- `total_games`: 总对局数
- `wins`: 胜场数
- `losses`: 负场数
- `created_at`: 创建时间
- `last_played_at`: 最后游戏时间

### game_records表
- `id`: 主键，自增
- `red_player_id/name`: 红方玩家信息
- `black_player_id/name`: 黑方玩家信息
- `winner_id/name`: 胜者信息
- `game_duration`: 对局时长（秒）
- `start_time/end_time`: 开始和结束时间

## 关键配置

### 服务器配置
- 端口：8888
- 支持多客户端并发连接
- 自动定时器（每秒更新游戏时间）

### 客户端配置
- 棋盘大小：9×10格
- 单元格大小：85像素
- 棋盘边距：50像素

### 数据库配置
- SQLite文件位置：项目根目录
- 自动创建表结构
- 连接池：单连接模式

## 常见开发任务

### 添加新功能
1. 确定功能属于客户端、服务器还是数据库
2. 在相应类中添加方法
3. 更新网络协议（如需要）
4. 测试功能完整性

### 修复Bug
1. 定位问题所在模块
2. 检查相关日志输出
3. 修复代码并测试
4. 确保不影响其他功能

### 数据库操作
- 使用`ChessDatabase`类进行所有数据库操作
- 所有SQL语句使用PreparedStatement防止注入
- 记得关闭数据库连接

## 测试策略

### 单元测试
- 测试核心游戏逻辑
- 验证数据库操作
- 检查网络通信协议

### 集成测试
- 多客户端连接测试
- 游戏流程完整性测试
- 数据持久化验证

### 性能测试
- 并发连接数测试
- 大量数据查询性能
- 内存使用监控

## 部署注意事项

1. **Java环境**: 确保目标机器安装Java 8+
2. **路径问题**: Windows使用分号(;)作为类路径分隔符
3. **防火墙**: 确保服务器端口8888可访问
4. **数据库权限**: 确保程序有创建和写入数据库文件的权限
5. **资源清理**: 程序退出时正确关闭所有连接

## 安全考虑

- 当前密码使用明文存储（需要改进）
- 基本的SQL注入防护
- 网络通信未加密
- 无身份验证过期机制

## 性能优化点

1. 数据库连接池
2. 网络消息批处理
3. GUI界面响应优化
4. 内存使用优化

## 扩展方向

- 密码加密存储
- 对局回放功能
- AI对手集成
- 皮肤主题系统
- 语音聊天支持
- 移动端适配

## 项目统计

- **总代码行数**: 2000+行
- **主要类数量**: 7个
- **文档数量**: 5个
- **开发时间**: 2024年12月
- **代码质量**: 生产就绪级别