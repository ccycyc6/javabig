#!/bin/bash

echo "中国象棋游戏测试脚本"
echo "===================="

# 启动服务器
echo "启动服务器..."
java ChessServer &
SERVER_PID=$!

# 等待服务器启动
sleep 2

# 启动两个客户端
echo "启动客户端1..."
java ChessClient &
CLIENT1_PID=$!

sleep 1

echo "启动客户端2..."
java ChessClient &
CLIENT2_PID=$!

echo "测试环境已启动！"
echo "服务器PID: $SERVER_PID"
echo "客户端1PID: $CLIENT1_PID"
echo "客户端2PID: $CLIENT2_PID"
echo ""
echo "按任意键停止所有进程..."
read -n 1

# 清理进程
kill $SERVER_PID $CLIENT1_PID $CLIENT2_PID 2>/dev/null
echo "测试完成！"