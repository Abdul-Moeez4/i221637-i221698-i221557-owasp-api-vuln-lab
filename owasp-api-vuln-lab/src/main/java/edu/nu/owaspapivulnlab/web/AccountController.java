package edu.nu.owaspapivulnlab.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import edu.nu.owaspapivulnlab.model.Account;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AccountRepository;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;

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

    // FIXED: Added ownership validation (Q3 fix)
    @GetMapping("/{id}/balance")
    public ResponseEntity<Double> balance(@PathVariable("id") Long id, Authentication auth) {
        if (auth == null || auth.getName() == null)
            return ResponseEntity.status(401).build(); // not logged in

        Account a = accounts.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AppUser me = users.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ownership check
        if (!a.getOwnerUserId().equals(me.getId()))
            return ResponseEntity.status(403).build(); // forbidden

        return ResponseEntity.ok(a.getBalance());
    }

    // FIXED: Added ownership validation (Q3 fix)
    @PostMapping("/{id}/transfer")
    public ResponseEntity<?> transfer(@PathVariable Long id, @RequestParam Double amount, Authentication auth) {
        if (auth == null || auth.getName() == null)
            return ResponseEntity.status(401).build();

        Account a = accounts.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        AppUser me = users.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // only owner can transfer
        if (!a.getOwnerUserId().equals(me.getId()))
            return ResponseEntity.status(403).build();

        a.setBalance(a.getBalance() - amount);
        accounts.save(a);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("remaining", a.getBalance());
        return ResponseEntity.ok(response);
    }

    // Safe-ish helper to view my accounts (still leaks more than needed)
    @GetMapping("/mine")
    public Object mine(Authentication auth) {
        AppUser me = users.findByUsername(auth != null ? auth.getName() : "anonymous").orElse(null);
        return me == null ? Collections.emptyList() : accounts.findByOwnerUserId(me.getId());
    }
}