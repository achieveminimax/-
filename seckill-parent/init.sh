#!/bin/bash

# ========================================================
# 电商秒杀系统环境初始化脚本
# ========================================================

echo "========================================"
echo "电商秒杀系统 - 环境初始化"
echo "========================================"
echo ""

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "错误: Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检查 Docker Compose 是否安装
if ! command -v docker-compose &> /dev/null; then
    echo "错误: Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "[1/4] 停止现有容器..."
docker-compose down 2>/dev/null || true

echo ""
echo "[2/4] 启动中间件服务..."
docker-compose up -d

echo ""
echo "[3/4] 等待服务启动..."
echo "等待 MySQL 启动..."
sleep 10

# 检查 MySQL 是否就绪
echo "检查 MySQL 连接状态..."
for i in {1..30}; do
    if docker exec seckill-mysql mysqladmin ping -h localhost --silent; then
        echo "MySQL 已就绪"
        break
    fi
    echo "等待 MySQL 就绪... ($i/30)"
    sleep 2
done

echo ""
echo "等待 Redis 启动..."
for i in {1..30}; do
    if docker exec seckill-redis redis-cli ping | grep -q PONG; then
        echo "Redis 已就绪"
        break
    fi
    echo "等待 Redis 就绪... ($i/30)"
    sleep 1
done

echo ""
echo "等待 RabbitMQ 启动..."
for i in {1..30}; do
    if docker exec seckill-rabbitmq rabbitmq-diagnostics ping 2>/dev/null | grep -q "Status: ok"; then
        echo "RabbitMQ 已就绪"
        break
    fi
    echo "等待 RabbitMQ 就绪... ($i/30)"
    sleep 2
done

echo ""
echo "[4/4] 检查服务状态..."
echo ""
docker-compose ps

echo ""
echo "========================================"
echo "环境初始化完成！"
echo "========================================"
echo ""
echo "服务访问信息:"
echo "  - MySQL:     localhost:3306 (root/root)"
echo "  - Redis:     localhost:6379"
echo "  - RabbitMQ:  localhost:5672"
echo "  - RabbitMQ管理界面: http://localhost:15672 (guest/guest)"
echo ""
echo "数据库初始化:"
echo "  - 数据库名: seckill_db"
echo "  - 初始化脚本已自动执行"
echo ""
echo "下一步: 编译并启动应用"
echo "  cd seckill-parent && mvn clean install && cd seckill-application && mvn spring-boot:run"
echo ""
