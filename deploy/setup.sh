#!/bin/bash

# ITZEEP Backend Deployment Setup Script
# This script sets up the deployment environment on the server

set -e

echo "🚀 Setting up ITZEEP Backend deployment environment..."

# Create necessary directories
echo "📁 Creating directories..."
sudo mkdir -p /opt/itzeep-backend
sudo mkdir -p /opt/itzeep-backend/certbot/conf
sudo mkdir -p /opt/itzeep-backend/certbot/www
sudo mkdir -p /opt/itzeep-backend/logs

# Clone repository
echo "📥 Cloning repository..."
cd /opt
sudo git clone https://github.com/ITZEEP/backend.git itzeep-backend
cd /opt/itzeep-backend

# Initialize submodules
echo "📦 Initializing submodules..."
sudo git submodule init
sudo git submodule update

# Create Docker network
echo "🌐 Creating Docker network..."
docker network create itzeep-network || true

# Start Nginx first
echo "🚀 Starting Nginx..."
docker-compose -f docker-compose.nginx.yml up -d nginx

# Install SSL certificates
echo "🔒 Setting up SSL certificates..."
echo "Please make sure your domains are pointing to this server before continuing."
read -p "Press enter to continue with SSL setup..."

# Get SSL certificates
echo "🔐 Obtaining SSL certificates..."

# Dev certificate
sudo docker run -it --rm \
    -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
    -v "$(pwd)/certbot/www:/var/www/certbot" \
    certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email admin@ariogi.kr \
    --agree-tos \
    --no-eff-email \
    -d dev.itzeep.ariogi.kr

# Prod certificate  
sudo docker run -it --rm \
    -v "$(pwd)/certbot/conf:/etc/letsencrypt" \
    -v "$(pwd)/certbot/www:/var/www/certbot" \
    certbot/certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email admin@ariogi.kr \
    --agree-tos \
    --no-eff-email \
    -d api.itzeep.ariogi.kr

# Restart Nginx with SSL
echo "🔄 Restarting Nginx with SSL..."
docker-compose -f docker-compose.nginx.yml restart nginx

# Create environment file template
echo "📝 Creating environment file template..."
cat > .env.example << 'EOF'
# Development Environment
DEV_MYSQL_ROOT_PASSWORD=changeme
DEV_MYSQL_USER=itzeep_dev
DEV_MYSQL_PASSWORD=changeme
DEV_REDIS_PASSWORD=changeme

# Production Environment  
PROD_MYSQL_ROOT_PASSWORD=changeme
PROD_MYSQL_USER=itzeep_prod
PROD_MYSQL_PASSWORD=changeme
PROD_REDIS_PASSWORD=changeme

# Shared Settings
JWT_SECRET=changeme_use_strong_secret_in_production

# OAuth2 Settings
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret

# AWS S3 Settings
AWS_ACCESS_KEY=your_aws_access_key
AWS_SECRET_KEY=your_aws_secret_key
AWS_S3_BUCKET_DEV=itzeep-dev-bucket
AWS_S3_BUCKET_PROD=itzeep-prod-bucket

# Email Settings
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_specific_password
EOF

echo "⚠️  Please copy .env.example to .env and fill in all the values"
echo ""
echo "📋 GitHub Secrets to configure:"
echo "   - SERVER_HOST: Your server IP or hostname"
echo "   - SERVER_USERNAME: SSH username for deployment"
echo "   - SERVER_SSH_KEY: Private SSH key for authentication"
echo "   - All environment variables from .env file"
echo ""
echo "🎯 Next steps:"
echo "1. Copy and configure .env file: cp .env.example .env"
echo "2. Start Nginx: docker-compose -f docker-compose.nginx.yml up -d"
echo "3. Deploy dev environment: docker-compose -f docker-compose.dev.yml up -d"
echo "4. Deploy prod environment: docker-compose -f docker-compose.prod.yml up -d"
echo ""
echo "✅ Setup complete!"