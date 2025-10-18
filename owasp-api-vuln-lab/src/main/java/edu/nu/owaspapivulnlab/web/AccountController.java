package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import edu.nu.owaspapivulnlab.model.Account;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AccountRepository;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.dto.TransferRequest;  // Q9 FIX: Safe transfer DTO

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accounts;
    private final AppUserRepository users;

    public AccountController(AccountRepository accounts, AppUserRepository users) {
        this.accounts = accounts;
        this.users = users;
    }

    /**
     * Q3 FIX: Get Account Balance with Resource Ownership Enforcement
     * 
     * OWASP API Security Top 10 - API1: Broken Object Level Authorization (BOLA/IDOR)
     * 
     * Security Controls Implemented:
     * 1. Authentication required (401 if not logged in)
     * 2. Resource ownership validation (403 if accessing other user's account)
     * 3. Proper error codes for different failure scenarios
     * 
     * This prevents users from accessing other users' account balances
     * by enforcing that only the account owner can view their balance.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<Double> balance(@PathVariable("id") Long id, Authentication auth) {
        // Q2 FIX: Require authentication (no more anonymous access)
        if (auth == null || auth.getName() == null)
            return ResponseEntity.status(401).build(); // Unauthorized

        Account a = accounts.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AppUser me = users.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Q3 FIX: Critical security check - only account owner can access balance
        if (!a.getOwnerUserId().equals(me.getId()))
            return ResponseEntity.status(403).build(); // Forbidden - not your account

        return ResponseEntity.ok(a.getBalance());
    }

    /**
     * Q9 FIX: Secure Money Transfer with Comprehensive Input Validation
     * 
     * OWASP API Security Top 10 - Multiple vulnerabilities addressed:
     * - API1: Broken Object Level Authorization (ownership check)
     * - API4: Unrestricted Resource Consumption (amount limits)
     * - API9: Improper Inventory Management (input validation)
     * 
     * Security Controls Implemented:
     * 1. Authentication required
     * 2. Resource ownership validation (only account owner can transfer)
     * 3. Input validation via @Valid annotation and TransferRequest DTO
     * 4. Business rule validation (maximum transfer limits)
     * 5. Insufficient funds validation
     * 6. Secure response structure
     */
    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable("id") Long id, @Valid @RequestBody TransferRequest request, Authentication auth) {
        // Q2 FIX: Require authentication
        if (auth == null || auth.getName() == null)
            return ResponseEntity.status(401).build();

        Account a = accounts.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AppUser me = users.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Q3 FIX: Critical security - only account owner can initiate transfers
        if (!a.getOwnerUserId().equals(me.getId()))
            return ResponseEntity.status(403).build();

        Double amount = request.getAmount();
        Double currentBalance = a.getBalance();
        
        // Q9 FIX: Business rule validation to prevent abuse
        if (amount > 10000.0) {
            throw new IllegalArgumentException("Transfer amount exceeds maximum limit of 10,000");
        }
        
        // Q9 FIX: Financial validation - prevent overdrafts
        if (amount > currentBalance) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        // Q9 FIX: Perform the transfer with proper calculation
        Double newBalance = currentBalance - amount;
        a.setBalance(newBalance);
        accounts.save(a);

        // Return structured success response
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("transferAmount", amount);
        response.put("remainingBalance", newBalance);
        response.put("description", request.getDescription());
        return ResponseEntity.ok(response);
    }

    // Safe-ish helper to view my accounts (still leaks more than needed)
    @GetMapping("/mine")
    public Object mine(Authentication auth) {
        AppUser me = users.findByUsername(auth != null ? auth.getName() : "anonymous").orElse(null);
        return me == null ? Collections.emptyList() : accounts.findByOwnerUserId(me.getId());
    }
}