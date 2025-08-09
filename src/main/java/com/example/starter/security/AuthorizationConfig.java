package com.example.starter.security;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


public class AuthorizationConfig {

  public static class ResourceMapping {

    public final Pattern pattern;
    public final Set<String> allowedRoles;
    public final String name;

    public ResourceMapping(String pathPattern, Set<String> allowedRoles, String name) {
      // Convert glob pattern to regex
      String regex = pathPattern
        .replace("/*", "/.*")
        .replace("/", "\\/");
      this.pattern = Pattern.compile("^" + regex + "$");
      this.allowedRoles = allowedRoles;
      this.name = name;
    }
    public boolean matches(String path) {
      return pattern.matcher(path).matches();
    }

  }

  // Define your resources exactly as in Keycloak
  public static List<ResourceMapping> getResourceMappings() {
    return List.of(
      // Administrateur resource
      new ResourceMapping("/users/*", Set.of("admin"), "Administrateur resource"),
      new ResourceMapping("/api/auth/register", Set.of("admin"), "Administrateur resource"),
      // new ResourceMapping("/api/compagnies", Set.of("admin"), "Administrateur resource"),
      //  new ResourceMapping("/api/v1/compagnie/save", Set.of("admin"), "Administrateur resource"),
      //new ResourceMapping("/api/v1/compagnies", Set.of("admin"), "Administrateur resource"),
      // // Require admin by default

      // DON'T DO THIS - Security Risk!
      new ResourceMapping("/*", Set.of("admin", "client-abonnement"), "Default Resource")    );
  }

}
