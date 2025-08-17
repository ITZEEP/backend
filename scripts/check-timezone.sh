#!/bin/bash
# 모든 컨테이너의 시간대 확인 스크립트

echo "🕐 Checking timezone for all containers..."
echo "================================================"

# Spring Backend 시간 확인
echo "📦 Spring Backend (itzeep-backend):"
docker exec itzeep-backend date
docker exec itzeep-backend sh -c 'echo "Timezone: $TZ"'
docker exec itzeep-backend sh -c 'ls -la /etc/localtime 2>/dev/null || echo "No localtime file"'
echo ""

# Redis 시간 확인
echo "📦 Redis (itzeep-redis):"
docker exec itzeep-redis date
docker exec itzeep-redis sh -c 'echo "Timezone: $TZ"'
docker exec itzeep-redis redis-cli TIME | head -1 | xargs -I {} date -d @{}
echo ""

# MongoDB 시간 확인  
echo "📦 MongoDB (itzeep-mongo):"
docker exec itzeep-mongo date
docker exec itzeep-mongo sh -c 'echo "Timezone: $TZ"'
docker exec itzeep-mongo mongosh --quiet --eval "new Date()"
echo ""

# 호스트 시스템 시간 확인
echo "🖥️ Host System:"
date
echo "Timezone: $(timedatectl | grep "Time zone" | awk '{print $3}')"
echo ""

echo "================================================"
echo "✅ Timezone check completed!"
echo ""
echo "📝 Note: All containers should show Asia/Seoul (KST) timezone"