#!/bin/bash

echo "��� POST /shipping"
curl -X POST http://localhost:8083/shipping \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Test Shipment",
        "address": "123 Test Street",
        "shipmentId": "shp-001"
      }'
echo -e "\n✅ POST terminé\n"

echo "��� GET /shipping"
curl http://localhost:8083/shipping
echo -e "\n✅ GET all terminé\n"

echo "��� GET /shipping/123"
curl http://localhost:8083/shipping/123
echo -e "\n✅ GET by ID terminé\n"

