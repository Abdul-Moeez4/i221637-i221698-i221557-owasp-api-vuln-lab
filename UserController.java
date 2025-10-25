/*
package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    // VULNERABILITY(API1: BOLA/IDOR) - no ownership check, any authenticated OR anonymous GET (due to SecurityConfig) can fetch any user
    @GetMapping("/{id}")
    public AppUser get(@PathVariable Long id) {
        return users.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    // VULNERABILITY(API6: Mass Assignment) - binds role/isAdmin from client
    @PostMapping
    public AppUser create(@Valid @RequestBody AppUser body) {
        return users.save(body);
    }

    // VULNERABILITY(API9: Improper Inventory + API8 Injection style): naive 'search' that can be abused for enumeration
    @GetMapping("/search")
    public List<AppUser> search(@RequestParam String q) {
        return users.search(q);
    }

    // VULNERABILITY(API3: Excessive Data Exposure) - returns all users including sensitive fields
    @GetMapping
    public List<AppUser> list() {
        return users.findAll();
    }

    // VULNERABILITY(API5: Broken Function Level Authorization) - allows regular users to delete anyone
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        users.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }
}
*/


/*
package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import edu.nu.owaspapivulnlab.dto.CreateUserRequest; //  added import
import edu.nu.owaspapivulnlab.dto.UserDTO;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;
import edu.nu.owaspapivulnlab.dto.UserDTO;  //  added import


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;          //  added import

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;

    public UserController(AppUserRepository users) {
        this.users = users;
    }

    //  Return DTO instead of entity (hide password, role, isAdmin)
    @GetMapping("/{id}")
    public UserDTO get(@PathVariable Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.from(user);
    }

    /* vulernable version
    // (still vulnerable to Mass Assignment - will be fixed in Q6)
    @PostMapping
    public AppUser create(@Valid @RequestBody AppUser body) {
        return users.save(body);
    }
    

   //fixed Q6
   @PostMapping
    public AppUser create(@Valid @RequestBody CreateUserRequest body) {
    AppUser user = new AppUser();
    user.setUsername(body.getUsername());
    user.setPassword(encoder.encode(body.getPassword()));
    user.setEmail(body.getEmail());

    // Safe defaults (no client control)
    user.setRole("USER");
    user.setAdmin(false);

    return users.save(user);
}




    // Return list of safe DTOs instead of full user data
    @GetMapping
    public List<UserDTO> list() {
        return users.findAll()
                .stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }

    // can leave this delete as-is for now)
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
*/

package edu.nu.owaspapivulnlab.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // <-- add
import org.springframework.web.bind.annotation.*;

import edu.nu.owaspapivulnlab.dto.CreateUserRequest;
import edu.nu.owaspapivulnlab.dto.UserDTO;
import edu.nu.owaspapivulnlab.model.AppUser;
import edu.nu.owaspapivulnlab.repo.AppUserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository users;
    private final PasswordEncoder encoder; // <-- add

    public UserController(AppUserRepository users, PasswordEncoder encoder) { // <-- add
        this.users = users;
        this.encoder = encoder;
    }

    // Return DTO instead of entity (hide password, role, isAdmin)
    @GetMapping("/{id}")
    public UserDTO get(@PathVariable Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return UserDTO.from(user);
    }

    /*
    // Q6: prevent mass assignment by using a safe request DTO and server-side defaults
    @PostMapping
    public AppUser create(@Valid @RequestBody CreateUserRequest body) {
        AppUser user = new AppUser();
        user.setUsername(body.getUsername());
        user.setPassword(encoder.encode(body.getPassword())); // <-- now compiles
        user.setEmail(body.getEmail());

        // Safe defaults (client cannot set these)
        user.setRole("USER");
        user.setAdmin(false);

        return users.save(user);
    }
    */
   @PostMapping
public ResponseEntity<UserDTO> create(@Valid @RequestBody CreateUserRequest body) {
    AppUser user = new AppUser();
    user.setUsername(body.getUsername());
    user.setPassword(encoder.encode(body.getPassword()));
    user.setEmail(body.getEmail());

    // server-side defaults (client cannot escalate)
    user.setRole("USER");
    user.setAdmin(false);

    AppUser saved = users.save(user);
    return ResponseEntity.status(201).body(UserDTO.from(saved)); // <-- DTO, no password
}

    // Return list of safe DTOs instead of full user data
    @GetMapping
    public List<UserDTO> list() {
        return users.findAll()
                .stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        users.deleteById(id);
        Map<String, String> response = new HashMap<>();
        response.put("status", "deleted");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public List<UserDTO> search(@RequestParam String q) {
        return users.search(q)
                .stream()
                .map(UserDTO::from)
                .collect(Collectors.toList());
    }
}

