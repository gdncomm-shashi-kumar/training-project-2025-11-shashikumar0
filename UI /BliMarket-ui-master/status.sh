#!/bin/bash

echo "========================================"
echo "  🎉 BliMarket UI - Ready to Use! 🎉"
echo "========================================"
echo ""

# Check if server is running
SERVER_PID=$(ps aux | grep "node server.js" | grep -v grep | awk '{print $2}')

if [ ! -z "$SERVER_PID" ]; then
    echo "✅ Server is RUNNING"
    echo "   Process ID: $SERVER_PID"
    echo "   Port: 3000"
    echo ""
    echo "🌐 Open in browser:"
    echo "   http://localhost:3000"
    echo ""
    echo "To stop the server:"
    echo "   kill $SERVER_PID"
else
    echo "❌ Server is NOT running"
    echo ""
    echo "To start the server:"
    echo "   npm start"
fi

echo ""
echo "========================================"

