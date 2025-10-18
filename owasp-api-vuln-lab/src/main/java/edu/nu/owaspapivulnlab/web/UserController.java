package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.dto.UserDTO;
import edu.nu.owaspapivulnlab.dto.CreateUserRequest;  // Q6 FIX: Safe request DTO
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;  // Q6 FIX: For secure password handling

    public UserController(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    //  Return DTO instead of entity (hide password, role, isAdmin)
    @GetMapping("/{id}")
    public UserDTO get(@PathVariable Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.from(user);
    }

    /**
     * Q6 FIX: Secure User Creation with Mass Assignment Prevention
     * 
     * OWASP API Security Top 10 - API6: Mass Assignment
     * 
     * Security Controls Implemented:
     * 1. Uses CreateUserRequest DTO that excludes sensitive fields (role, isAdmin)
     * 2. Server-side validation with @Valid annotation
     * 3. Server controls all security-sensitive field assignments
     * 4. Password hashing with BCrypt (Q1 fix)
     * 5. Returns safe UserDTO without sensitive information (Q4 fix)
     * 
     * This prevents attackers from setting themselves as admin or assigning
     * privileged roles by including extra fields in the request.
     */
    @PostMapping
    public UserDTO create(@Valid @RequestBody CreateUserRequest request) {
        // Q6 FIX: Server-side validation and secure field assignment
        AppUser user = AppUser.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))  // Q1 FIX: BCrypt hashing
                .email(request.getEmail())
                .role("USER")        // Q6 FIX: Server controls role - cannot be overridden by client
                .isAdmin(false)      // Q6 FIX: Server controls admin status - secure default
                .build();
        
        AppUser saved = users.save(user);
        return UserDTO.from(saved);  // Q4 FIX: Return safe DTO without sensitive fields
    }

    // Return list of safe DTOs instead of full user data
    @GetMapping
    public List<UserDTO> list() {
        return users.findAll()
                .stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }

    // (you can leave this delete as-is for now; Q5/Q6 handle roles)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        users.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }

    // (optional) search endpoint — still returns DTOs safely
    @GetMapping("/search")
    public List<UserDTO> search(@RequestParam String q) {
        return users.search(q)
                .stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
}