package edu.nu.owaspapivulnlab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Q6 FIX: Secure User Creation Request DTO - Mass Assignment Prevention
 * 
 * OWASP API Security Top 10 - API6: Mass Assignment
 * 
 * This DTO intentionally excludes sensitive fields to prevent mass assignment attacks:
 * 
 * Included Fields (Safe):
 * - username: User's chosen username (validated)
 * - password: User's password (will be hashed server-side)
 * - email: User's email address (validated)
 * 
 * Excluded Fields (Security-sensitive):
 * - role: Controlled server-side, defaults to "USER"
 * - isAdmin: Controlled server-side, defaults to false
 * 
 * This prevents attackers from creating admin accounts by including
 * {"role": "ADMIN", "isAdmin": true} in their registration request.
 */
public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    // Constructors
    public CreateUserRequest() {}
    
    public CreateUserRequest(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}