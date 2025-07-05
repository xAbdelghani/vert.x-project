package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;


public class RegistrationService {

  private final KeycloakAdminService keycloakAdmin;
  private final EmailService emailService;


  public RegistrationService(KeycloakAdminService keycloakAdmin, EmailService emailService) {
    this.keycloakAdmin = keycloakAdmin;
    this.emailService = emailService;
  }

  public Future<JsonObject> registerUser(JsonObject registrationData) {
    return keycloakAdmin.createUser(registrationData)
      .compose(userId -> {
        // Send welcome email (don't fail registration if email fails)
        emailService.sendWelcomeEmail(
          registrationData.getString("email"),
          registrationData.getString("username")
        ).onFailure(err -> {
          System.err.println("Failed to send welcome email: " + err.getMessage());
        });

        return Future.succeededFuture(new JsonObject()
          .put("userId", userId)
          .put("message", "User created successfully"));
      });
  }

  private String validateRegistration(JsonObject data) {
    if (!data.containsKey("username") || data.getString("username").isEmpty()) {
      return "Username is required";
    }
    if (!data.containsKey("email") || !isValidEmail(data.getString("email"))) {
      return "Valid email is required";
    }
    if (!data.containsKey("password") || data.getString("password").length() < 8) {
      return "Password must be at least 8 characters";
    }
    return null;
  }

  private boolean isValidEmail(String email) {
    return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
  }
}
