package edu.nu.owaspapivulnlab.dto;

public class AppUserDTO {
    private Long id;
    private String username;
    private String email;

    public AppUserDTO(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // getters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
