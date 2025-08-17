#!/bin/bash
# 백엔드만 재배포하는 스크립트 (MongoDB, Redis는 유지)

set -e

echo "🚀 Starting backend-only deployment..."

# 설정
CONTAINER_NAME="itzeep-backend"
REDIS_CONTAINER="itzeep-redis"
MONGO_CONTAINER="itzeep-mongo"
PROJECT_DIR="$HOME/apps/itzeep"

# 프로젝트 디렉토리로 이동
cd $PROJECT_DIR

# 환경 파일 확인
if [ ! -f .env ]; then
    echo "❌ Error: .env file not found!"
    exit 1
fi

# MongoDB와 Redis 상태 확인
echo "📊 Checking existing services..."
if docker ps | grep -q $REDIS_CONTAINER; then
    echo "✅ Redis is running"
else
    echo "⚠️ Redis is not running, starting Redis..."
    docker-compose up -d redis
fi

if docker ps | grep -q $MONGO_CONTAINER; then
    echo "✅ MongoDB is running"
else
    echo "⚠️ MongoDB is not running, starting MongoDB..."
    docker-compose up -d mongo
fi

# 백엔드 컨테이너만 중지 및 제거
echo "🔄 Stopping backend container..."
docker-compose stop backend || true
docker-compose rm -f backend || true

# 최신 이미지 풀 (GHCR 사용 시)
if grep -q "ghcr.io" docker-compose.yml; then
    echo "📥 Pulling latest backend image from GHCR..."
    IMAGE=$(grep -A1 "backend:" docker-compose.yml | grep "image:" | awk '{print $2}')
    docker pull $IMAGE
fi

# 백엔드 컨테이너만 재시작
echo "🔄 Starting new backend container..."
docker-compose up -d backend

# 헬스체크
echo "🏥 Waiting for backend to be healthy..."
for i in {1..30}; do
    if curl -f http://localhost:8080/api/health 2>/dev/null; then
        echo "✅ Backend is healthy!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ Health check failed!"
        docker logs $CONTAINER_NAME --tail 50
        exit 1
    fi
    echo "⏳ Waiting... ($i/30)"
    sleep 5
done

# 상태 확인
echo ""
echo "📊 Final status:"
docker-compose ps

# 데이터 영속성 확인
echo ""
echo "💾 Data volumes (preserved):"
docker volume ls | grep -E "(redis-data|mongo-data|mongo-config)" || true

# 이미지 정리
echo ""
echo "🧹 Cleaning up old images..."
docker image prune -af

echo ""
echo "🎉 Backend deployment completed successfully!"
echo "📝 MongoDB and Redis data have been preserved"