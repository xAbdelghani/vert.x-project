package com.example.starter.security;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Set;


public class PolicyEnforcerHandler implements Handler<RoutingContext> {

  private final OAuth2Auth oauth2;

  private final List<AuthorizationConfig.ResourceMapping> resourceMappings;

  public PolicyEnforcerHandler(OAuth2Auth oauth2) {
    this.oauth2 = oauth2;
    this.resourceMappings = AuthorizationConfig.getResourceMappings();
  }

  @Override
  public void handle(RoutingContext ctx) {
    String token = extractToken(ctx);

    if (token == null) {
      deny(ctx, 401, "No token provided");
      return;
    }

    // First authenticate the token
    oauth2.authenticate(new TokenCredentials(token))
      .onSuccess(user -> {
        ctx.setUser(user);

        // Then check authorization based on path
        String path = ctx.request().path();
        String method = ctx.request().method().name();

        // Find matching resource
        AuthorizationConfig.ResourceMapping matchedResource = null;
        for (AuthorizationConfig.ResourceMapping resource : resourceMappings) {
          if (resource.matches(path)) {
            matchedResource = resource;
            break; // First match wins (order matters!)
          }
        }

        if (matchedResource == null) {
          // No resource matched - deny by default (ENFORCING mode)
          deny(ctx, 403, "No resource policy for path: " + path);
          return;
        }

        // Check if user has required roles
        if (matchedResource.allowedRoles.isEmpty()) {
          // Default resource - any authenticated user can access
          System.out.println("Access granted to " + path + " (Default Resource)");
          ctx.next();
        } else {
          // Check if user has any of the required roles
          Set<String> userRoles = extractUserRoles(user.principal());
          boolean hasAccess = matchedResource.allowedRoles.stream()
            .anyMatch(userRoles::contains);

            if (hasAccess) {
            System.out.println("Access granted to " + path + " (" + matchedResource.name + ")");
            ctx.next();
          } else {
            deny(ctx, 403, "Access denied. Required roles: " + matchedResource.allowedRoles);
          }
        }
      })
      .onFailure(err -> {
        deny(ctx, 401, "Invalid token");
      });
  }

  private String extractToken(RoutingContext ctx) {
    String auth = ctx.request().getHeader("Authorization");
    return (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7) : null;
  }

  private Set<String> extractUserRoles(JsonObject principal) {
    Set<String> roles = new java.util.HashSet<>();
    // Extract realm roles
    JsonObject realmAccess = principal.getJsonObject("realm_access");
    if (realmAccess != null) {
      JsonArray realmRoles = realmAccess.getJsonArray("roles");
      if (realmRoles != null) {
        realmRoles.forEach(role -> roles.add(role.toString()));
      }
    }
    // Extract client roles if needed
    JsonObject resourceAccess = principal.getJsonObject("resource_access");
    if (resourceAccess != null) {
      String clientId = principal.getString("azp");
      JsonObject clientAccess = resourceAccess.getJsonObject(clientId);
      if (clientAccess != null) {
        JsonArray clientRoles = clientAccess.getJsonArray("roles");
        if (clientRoles != null) {
          clientRoles.forEach(role -> roles.add(role.toString()));
        }
      }
    }
    return roles;
  }

  private void deny(RoutingContext ctx, int statusCode, String message) {
    ctx.response()
      .setStatusCode(statusCode)
      .putHeader("content-type", "application/json")
      .end(new JsonObject()
        .put("error", message)
        .put("path", ctx.request().path())
        .put("method", ctx.request().method().name())
        .encode());
  }


}
