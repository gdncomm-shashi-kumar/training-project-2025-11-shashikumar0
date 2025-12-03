#!/bin/bash

# BliMarket UI Startup Script
# This script helps you start the application with proper environment setup

echo "=========================================="
echo "  🛍️  BliMarket UI - Startup Script"
echo "=========================================="
echo ""

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 12+ to continue."
    exit 1
fi

echo "✅ Node.js version: $(node --version)"
echo ""

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
    echo ""
fi

# Check if environment variables are set
if [ -z "$MEMBER_SERVICE_URL" ]; then
    export MEMBER_SERVICE_URL="http://localhost:8089"
    echo "⚙️  MEMBER_SERVICE_URL not set, using default: $MEMBER_SERVICE_URL"
fi

if [ -z "$PRODUCT_SERVICE_URL" ]; then
    export PRODUCT_SERVICE_URL="http://localhost:8083"
    echo "⚙️  PRODUCT_SERVICE_URL not set, using default: $PRODUCT_SERVICE_URL"
fi

if [ -z "$CART_SERVICE_URL" ]; then
    export CART_SERVICE_URL="http://localhost:8089"
    echo "⚙️  CART_SERVICE_URL not set, using default: $CART_SERVICE_URL"
fi

if [ -z "$PORT" ]; then
    export PORT="3000"
    echo "⚙️  PORT not set, using default: $PORT"
fi

echo ""
echo "=========================================="
echo "  Configuration"
echo "=========================================="
echo "Member Service:  $MEMBER_SERVICE_URL"
echo "Product Service: $PRODUCT_SERVICE_URL"
echo "Cart Service:    $CART_SERVICE_URL"
echo "UI Port:         $PORT"
echo "=========================================="
echo ""

# Build assets if dist folder doesn't exist
if [ ! -d "public/dist" ]; then
    echo "🔨 Building assets..."
    npm run build
    echo ""
fi

echo "🚀 Starting BliMarket UI..."
echo ""
echo "📝 Once started, open your browser at: http://localhost:$PORT"
echo ""
echo "⚠️  Make sure your backend services are running:"
echo "   - Member Service at $MEMBER_SERVICE_URL"
echo "   - Product Service at $PRODUCT_SERVICE_URL"
echo "   - Cart Service at $CART_SERVICE_URL"
echo ""
echo "=========================================="
echo ""

# Start the server
npm start

