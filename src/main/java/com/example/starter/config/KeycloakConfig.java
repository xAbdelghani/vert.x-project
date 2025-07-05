package com.example.starter.config;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;

public class KeycloakConfig {

  public static OAuth2Auth create(Vertx vertx, JsonObject config) {
    JsonObject keycloakJson = new JsonObject()
      .put("realm", config.getString("realm"))
      .put("auth-server-url", config.getString("authServerUrl"))
      .put("ssl-required", "external")
      .put("resource", config.getString("clientId"))
      .put("bearer-only", true)
      .put("credentials", new JsonObject()
        .put("secret", config.getString("clientSecret")));

    return KeycloakAuth.create(vertx, keycloakJson);
  }


}
