package edu.nu.owaspapivulnlab.dto;

import edu.nu.owaspapivulnlab.model.AppUser;

/**
 * DTO to safely expose non-sensitive user fields only.
 * (No password, role, or isAdmin included)
 */
public class UserDTO {
    private Long id;
    private String username;
    private String email;

    public UserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    public static UserDTO from(AppUser u) {
        return new UserDTO(u.getId(), u.getUsername(), u.getEmail());
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}