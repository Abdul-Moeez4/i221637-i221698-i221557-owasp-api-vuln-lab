package edu.nu.owaspapivulnlab.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Q9 FIX: Secure Transfer Request DTO with Input Validation
 * 
 * OWASP API Security Top 10 - API9: Improper Inventory Management
 * 
 * This DTO provides comprehensive input validation for money transfers:
 * 
 * Validation Rules:
 * - @NotNull: Amount field is required
 * - @Positive: Amount must be greater than zero
 * - @DecimalMin: Minimum transfer amount is 0.01
 * 
 * Security Benefits:
 * - Prevents negative amount transfers
 * - Enforces minimum transfer amounts
 * - Validates required fields
 * - Type-safe with proper data types
 */
public class TransferRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMin(value = "0.01", message = "Minimum transfer amount is 0.01")
    private Double amount;
    
    private String description;

    // Constructors
    public TransferRequest() {}
    
    public TransferRequest(Double amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    // Getters and Setters
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return "TransferRequest{amount=" + amount + ", description='" + description + "'}";
    }
}