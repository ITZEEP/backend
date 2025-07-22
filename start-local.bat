@echo off
echo Starting ITZeep Backend Local Environment...
echo.

REM 환경변수 파일 로드
set ENV_FILE=.env.local

REM 이미 실행 중인 컨테이너 정리
echo Cleaning up existing containers...
docker compose -f docker-compose.local.yml --env-file %ENV_FILE% down

REM 네트워크 생성 (이미 존재할 수 있음)
echo Creating Docker network...
docker network create itzeep-network 2>nul || echo Network already exists

REM 컨테이너 빌드 및 실행
echo Building and starting containers...
docker compose -f docker-compose.local.yml --env-file %ENV_FILE% up -d --build

REM 컨테이너 상태 확인
echo.
echo Checking container status...
timeout /t 5 >nul
docker ps | findstr itzeep

echo.
echo Local environment is starting...
echo.
echo Application will be available at:
echo   - API: http://localhost:8080
echo   - Swagger UI: http://localhost:8080/swagger-ui.html
echo   - MySQL: localhost:3306
echo   - Redis: localhost:6379
echo.
echo To view logs: docker compose -f docker-compose.local.yml logs -f
echo To stop: docker compose -f docker-compose.local.yml down