#!/bin/bash

# AI Provider Service Test Runner
# This script helps you run the AI provider tests locally

set -e  # Exit on error

echo "=========================================="
echo "AI Provider Service Test Runner"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print colored output
print_green() {
    echo -e "${GREEN}$1${NC}"
}

print_yellow() {
    echo -e "${YELLOW}$1${NC}"
}

print_red() {
    echo -e "${RED}$1${NC}"
}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    print_red "❌ Maven is not installed. Please install Maven first."
    exit 1
fi

print_green "✅ Maven found: $(mvn -version | head -n 1)"
echo ""

# Menu
echo "Select test type to run:"
echo "1) Unit Tests (Fast, no API key needed)"
echo "2) Integration Tests (Real API calls, requires API key)"
echo "3) Both (Unit + Integration)"
echo "4) All project tests"
echo ""
read -p "Enter choice [1-4]: " choice

case $choice in
    1)
        print_yellow "Running Unit Tests..."
        echo ""
        mvn test -Dtest=AIProviderServiceDebugTest
        ;;
    2)
        print_yellow "Running Integration Tests..."
        echo ""
        
        # Check if API key is set
        if [ -z "$GROQ_API_KEY" ] && [ -z "$GEMINI_API_KEY" ] && [ -z "$OPENAI_API_KEY" ]; then
            print_red "⚠️  WARNING: No API keys found in environment!"
            echo ""
            echo "To run integration tests, you need at least one API key:"
            echo ""
            echo "Option 1 - Groq (FREE, recommended):"
            echo "  1. Get key from: https://console.groq.com"
            echo "  2. Run: export GROQ_API_KEY='your-key-here'"
            echo "  3. Run: export GROQ_ENABLED=true"
            echo ""
            echo "Option 2 - Gemini (FREE):"
            echo "  1. Get key from: https://makersuite.google.com/app/apikey"
            echo "  2. Run: export GEMINI_API_KEY='your-key-here'"
            echo "  3. Run: export GEMINI_ENABLED=true"
            echo ""
            echo "Option 3 - OpenAI (PAID):"
            echo "  1. Get key from: https://platform.openai.com/api-keys"
            echo "  2. Run: export OPENAI_API_KEY='your-key-here'"
            echo "  3. Run: export OPENAI_ENABLED=true"
            echo ""
            read -p "Do you want to continue anyway? [y/N]: " continue_choice
            if [[ ! $continue_choice =~ ^[Yy]$ ]]; then
                print_yellow "Exiting..."
                exit 0
            fi
        else
            print_green "✅ API key(s) found in environment"
            [ ! -z "$GROQ_API_KEY" ] && echo "  - Groq: Configured"
            [ ! -z "$GEMINI_API_KEY" ] && echo "  - Gemini: Configured"
            [ ! -z "$OPENAI_API_KEY" ] && echo "  - OpenAI: Configured"
            echo ""
        fi
        
        mvn test -Dtest=AIProviderServiceIntegrationTest
        ;;
    3)
        print_yellow "Running Unit Tests first..."
        echo ""
        mvn test -Dtest=AIProviderServiceDebugTest
        
        echo ""
        print_yellow "Running Integration Tests..."
        echo ""
        mvn test -Dtest=AIProviderServiceIntegrationTest
        ;;
    4)
        print_yellow "Running ALL project tests..."
        echo ""
        mvn test
        ;;
    *)
        print_red "Invalid choice. Exiting."
        exit 1
        ;;
esac

echo ""
if [ $? -eq 0 ]; then
    print_green "=========================================="
    print_green "✅ ALL TESTS PASSED!"
    print_green "=========================================="
else
    print_red "=========================================="
    print_red "❌ SOME TESTS FAILED"
    print_red "=========================================="
    exit 1
fi

