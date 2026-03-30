package com.fiipractic.stocks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoami(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<>();

        if (jwt == null) {
            response.put("userId", null);
            response.put("username", null);
            response.put("email", null);
            response.put("roles", Collections.emptyList());
            return ResponseEntity.ok(response);
        }

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = extractRoles(realmAccess);

        response.put("userId", jwt.getSubject());
        response.put("username", jwt.getClaimAsString("preferred_username"));
        response.put("email", jwt.getClaimAsString("email"));
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    private List<String> extractRoles(Map<String, Object> realmAccess) {
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> rawRoles)) {
            return Collections.emptyList();
        }

        return rawRoles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
    }
}