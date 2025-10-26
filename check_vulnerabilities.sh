#!/bin/bash

SRC_PATH="owasp-api-vuln-lab/src/main/java"
TEST_PATH="owasp-api-vuln-lab/src/test/java"

echo "==================== PHASE 1: CHECKING IMPLEMENTED FIXES ===================="

# 1. PASSWORD SECURITY
echo "[1] Checking for BCrypt password hashing..."
grep -R "BCrypt" $SRC_PATH || echo "❌ BCrypt not found — plaintext password usage suspected."

# 2. ACCESS CONTROL
echo "[2] Checking SecurityFilterChain for permitAll..."
grep -R "permitAll" $SRC_PATH || echo "✅ No unrestricted access found."

# 3. RESOURCE OWNERSHIP
echo "[3] Checking controller for ownership validation..."
grep -R "getPrincipal" $SRC_PATH || echo "❌ No ownership validation found (getPrincipal missing)."

# 4. DATA EXPOSURE CONTROL
echo "[4] Checking for DTO usage..."
grep -R "DTO" $SRC_PATH || echo "❌ No DTO classes found — may expose sensitive data."

# 5. RATE LIMITING
echo "[5] Checking for Bucket4j or Resilience4j..."
grep -R -E "Bucket4j|Resilience4j" $SRC_PATH || echo "❌ No rate limiter found."

# 6. MASS ASSIGNMENT
echo "[6] Checking for explicit request DTOs..."
grep -R "RequestDTO" $SRC_PATH || echo "❌ No explicit request DTOs found."

# 7. JWT HARDENING
echo "[7] Checking for strong JWT config..."
grep -R "io.jsonwebtoken" $SRC_PATH && grep -R "issuer" $SRC_PATH && grep -R "audience" $SRC_PATH || echo "❌ JWT config missing claims or weak secret."

# 8. ERROR HANDLING
echo "[8] Checking for custom exception handlers..."
grep -R "ControllerAdvice" $SRC_PATH || echo "❌ No @ControllerAdvice error handling found."

# 9. INPUT VALIDATION
echo "[9] Checking for input validation annotations..."
grep -R -E "@Valid|@NotNull|@Size|@Min|@Max" $SRC_PATH || echo "❌ No validation annotations found."

# 10. TESTING
echo "[10] Checking for test cases..."
grep -R "Test" $TEST_PATH || echo "❌ No integration or unit tests found."

echo "==================== CHECK COMPLETE ===================="
