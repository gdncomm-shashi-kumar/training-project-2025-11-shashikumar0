#!/bin/bash

echo "========================================"
echo "  BliMarket UI - Installation Script"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check Node.js
echo -e "${BLUE}Checking prerequisites...${NC}"
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    echo "Please install Node.js from https://nodejs.org/"
    exit 1
fi

echo -e "${GREEN}✅ Node.js $(node --version)${NC}"

# Check npm
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm is not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✅ npm $(npm --version)${NC}"
echo ""

# Install dependencies
echo -e "${BLUE}📦 Installing dependencies...${NC}"
npm install

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Dependencies installed successfully${NC}"
else
    echo -e "${RED}❌ Failed to install dependencies${NC}"
    exit 1
fi
echo ""

# Create dist directory if it doesn't exist
echo -e "${BLUE}📁 Setting up directories...${NC}"
mkdir -p public/dist
echo -e "${GREEN}✅ Directories created${NC}"
echo ""

# Create placeholder CSS file
echo -e "${BLUE}🎨 Creating CSS file...${NC}"
touch public/dist/styles.min.css
echo -e "${GREEN}✅ CSS file created${NC}"
echo ""

# Create placeholder JS file  
echo -e "${BLUE}📜 Creating JS file...${NC}"
touch public/dist/scripts.min.js
echo -e "${GREEN}✅ JS file created${NC}"
echo ""

# Summary
echo "========================================"
echo -e "${GREEN}✅ Installation Complete!${NC}"
echo "========================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Ensure backend services are running:"
echo "   - Member Service: http://localhost:8089"
echo "   - Product Service: http://localhost:8083"
echo "   - Cart Service: http://localhost:8089"
echo ""
echo "2. Start the application:"
echo "   ${YELLOW}npm start${NC}"
echo "   or"
echo "   ${YELLOW}./start.sh${NC}"
echo ""
echo "3. Open your browser:"
echo "   ${BLUE}http://localhost:3000${NC}"
echo ""
echo "For detailed setup instructions, see:"
echo "   - ${BLUE}SETUP_GUIDE.md${NC}"
echo "   - ${BLUE}API_TESTING_GUIDE.md${NC}"
echo ""
echo "========================================"
