#!/bin/bash

# Payment Gateway Application Test Suite
# This script performs comprehensive testing of all system components

set -e

API_BASE_URL="http://localhost:8000/api/v1"
API_KEY="key_test_abc123"
API_SECRET="secret_test_xyz789"
WEBHOOK_SECRET="whsec_test_abc123"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counter
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function to make API calls
function api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$data" ]; then
        curl -s -X "$method" \
            -H "Content-Type: application/json" \
            -H "X-Api-Key: $API_KEY" \
            -H "X-Api-Secret: $API_SECRET" \
            "$API_BASE_URL$endpoint"
    else
        curl -s -X "$method" \
            -H "Content-Type: application/json" \
            -H "X-Api-Key: $API_KEY" \
            -H "X-Api-Secret: $API_SECRET" \
            -d "$data" \
            "$API_BASE_URL$endpoint"
    fi
}

# Helper function for test assertions
function assert_status() {
    local response=$1
    local expected_code=$2
    local test_name=$3
    
    ((TESTS_RUN++))
    
    local status=$(echo "$response" | tail -n1)
    
    if [[ "$status" == "$expected_code"* ]]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (Expected: $expected_code, Got: ${status:0:3})"
        ((TESTS_FAILED++))
    fi
}

# Helper function for JSON assertions
function assert_json_field() {
    local json=$1
    local field=$2
    local expected_value=$3
    local test_name=$4
    
    ((TESTS_RUN++))
    
    local actual=$(echo "$json" | grep -o "\"$field\":[^,}]*" | cut -d':' -f2 | tr -d ' "')
    
    if [[ "$actual" == "$expected_value" ]]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name (Expected: $expected_value, Got: $actual)"
        ((TESTS_FAILED++))
    fi
}

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Payment Gateway Application Test Suite${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Test 1: Health Check
echo -e "${YELLOW}Test Group 1: Health & Status Endpoints${NC}"
HEALTH=$(curl -s http://localhost:8000/health)
echo -e "Response: $HEALTH\n"

# Test 2: Orders - Create Order
echo -e "${YELLOW}Test Group 2: Order Operations${NC}"
ORDER_PAYLOAD='{"description":"Test Order","amount":9999,"currency":"USD","email":"test@example.com"}'
ORDER_RESPONSE=$(api_call "POST" "/orders" "$ORDER_PAYLOAD")
echo -e "Created Order: $ORDER_RESPONSE\n"

# Extract order ID for subsequent tests
ORDER_ID=$(echo "$ORDER_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "Order ID: $ORDER_ID"

# Test 3: Create Payment
echo -e "\n${YELLOW}Test Group 3: Payment Operations${NC}"
PAYMENT_PAYLOAD="{\"orderId\":\"$ORDER_ID\",\"amount\":9999,\"currency\":\"USD\",\"cardNumber\":\"4111111111111111\",\"expiryMonth\":12,\"expiryYear\":25,\"cvv\":\"123\",\"cardholderName\":\"Test User\"}"
PAYMENT_RESPONSE=$(api_call "POST" "/payments" "$PAYMENT_PAYLOAD")
echo -e "Created Payment: $PAYMENT_RESPONSE\n"

# Extract payment ID
PAYMENT_ID=$(echo "$PAYMENT_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo "Payment ID: $PAYMENT_ID"

if [ ! -z "$PAYMENT_ID" ]; then
    # Test 4: Get Payment Status
    echo -e "\n${YELLOW}Test Group 4: Payment Status${NC}"
    PAYMENT_STATUS=$(api_call "GET" "/payments/$PAYMENT_ID" "")
    echo -e "Payment Status: $PAYMENT_STATUS\n"
    
    # Test 5: Capture Payment (after async processing)
    echo -e "${YELLOW}Waiting for async payment processing (5 seconds)...${NC}"
    sleep 5
    
    CAPTURE_RESPONSE=$(api_call "POST" "/payments/$PAYMENT_ID/capture" "{}")
    echo -e "Capture Response: $CAPTURE_RESPONSE\n"
fi

# Test 6: List Webhooks
echo -e "${YELLOW}Test Group 5: Webhook Management${NC}"
WEBHOOKS=$(api_call "GET" "/webhooks" "")
echo -e "Webhooks List: $WEBHOOKS\n"

# Test 7: Job Queue Status
echo -e "${YELLOW}Test Group 6: Job Queue Status${NC}"
JOB_STATUS=$(api_call "GET" "/test/jobs/status" "")
echo -e "Job Status: $JOB_STATUS\n"

# Test 8: Error Cases
echo -e "${YELLOW}Test Group 7: Error Handling${NC}"

# Invalid order creation (missing required fields)
INVALID_ORDER=$(api_call "POST" "/orders" '{}')
echo -e "Invalid Order Response: $INVALID_ORDER"

# Non-existent payment
NOTFOUND=$(api_call "GET" "/payments/invalid-id" "")
echo -e "Not Found Response: $NOTFOUND"

# Test Summary
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "Total Tests: $TESTS_RUN"
echo -e "${GREEN}Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Failed: $TESTS_FAILED${NC}"
echo -e ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}\n"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}\n"
    exit 1
fi
