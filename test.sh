#!/bin/bash

echo "中国象棋游戏测试脚本"
echo "===================="

# clean
rm -rf build

echo "compiling..."
mkdir build
javac -cp "lib/*" -d build src/*.java

sleep 2

echo "open Server..."
java -cp "build:lib/*" src.ChessServer &
SERVER_PID=$!

sleep 2

echo "start client 1..."
java -cp "build:lib/*" src.ChessClient &
CLIENT1_PID=$!

sleep 1

echo "start client 2..."
java -cp "build:lib/*" src.ChessClient &
CLIENT2_PID=$!


echo "start client 3..."
java -cp "build:lib/*" src.ChessClient &
CLIENT3_PID=$!

echo ""
echo "Server PID: $SERVER_PID"
echo "client1 PID: $CLIENT1_PID"
echo "client2 PID: $CLIENT2_PID"
echo "client3 PID: $CLIENT3_PID"
echo ""
echo "type any key to stop the program..."
read -n 1

kill $SERVER_PID $CLIENT1_PID $CLIENT2_PID $CLIENT3_PID 2>/dev/null
echo "finish test !"
