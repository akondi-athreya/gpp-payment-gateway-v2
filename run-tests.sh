#!/bin/bash

echo "=========================================="
echo "Payment Gateway - Comprehensive Test"
echo "=========================================="
echo ""

API_BASE="http://localhost:8000"
API_KEY="key_test_abc123"
API_SECRET="secret_test_xyz789"

echo "1️⃣  Testing Health Endpoint"
curl -s "$API_BASE/health" | python3 -m json.tool
echo ""

echo "2️⃣  Creating Order"
ORDER_RESP=$(curl -s -X POST "$API_BASE/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: $API_KEY" \
  -H "X-Api-Secret: $API_SECRET" \
  -d '{"amount":10000,"currency":"INR","receipt":"test_001"}')
echo "$ORDER_RESP" | python3 -m json.tool
ORDER_ID=$(echo "$ORDER_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
echo "Extracted Order ID: $ORDER_ID"
echo ""

if [ ! -z "$ORDER_ID" ]; then
  echo "3️⃣  Creating Payment for Order"
  PAYMENT_RESP=$(curl -s -X POST "$API_BASE/api/v1/payments" \
    -H "Content-Type: application/json" \
    -H "X-Api-Key: $API_KEY" \
    -H "X-Api-Secret: $API_SECRET" \
    -d "{\"orderId\":\"$ORDER_ID\",\"amount\":10000,\"currency\":\"INR\",\"method\":\"card\"}")
  echo "$PAYMENT_RESP" | python3 -m json.tool
  PAYMENT_ID=$(echo "$PAYMENT_RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id',''))" 2>/dev/null)
  echo "Extracted Payment ID: $PAYMENT_ID"
  echo ""
  
  if [ ! -z "$PAYMENT_ID" ]; then
    echo "4️⃣  Getting Payment Status"
    curl -s "$API_BASE/api/v1/payments/$PAYMENT_ID" \
      -H "X-Api-Key: $API_KEY" \
      -H "X-Api-Secret: $API_SECRET" | python3 -m json.tool
    echo ""
    
    echo "5️⃣  Waiting for processing..."
    sleep 3
    
    echo "6️⃣  Capturing Payment"
    curl -s -X POST "$API_BASE/api/v1/payments/$PAYMENT_ID/capture" \
      -H "Content-Type: application/json" \
      -H "X-Api-Key: $API_KEY" \
      -H "X-Api-Secret: $API_SECRET" \
      -d '{}' | python3 -m json.tool
    echo ""
  fi
fi

echo "7️⃣  Listing Webhooks"
curl -s "$API_BASE/api/v1/webhooks" \
  -H "X-Api-Key: $API_KEY" \
  -H "X-Api-Secret: $API_SECRET" | python3 -m json.tool
echo ""

echo "8️⃣  Job Queue Status"
curl -s "$API_BASE/api/v1/test/jobs/status" | python3 -m json.tool
echo ""

echo "✅ Tests Complete"
