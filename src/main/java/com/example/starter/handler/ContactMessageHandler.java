package com.example.starter.handler;

import com.example.starter.service.EmailService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ContactMessageHandler {

  private final EmailService emailService;

  private final String adminEmail;

  public ContactMessageHandler(EmailService emailService, String adminEmail) {
    this.emailService = emailService;
    this.adminEmail = adminEmail;
  }

  // POST /api/contact/send-email
  public void sendContactEmail(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    // Validate required fields
    String name = body.getString("name");
    String email = body.getString("email");
    String message = body.getString("message");

    if (name == null || name.trim().isEmpty()) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Le nom est requis").encode());
      return;
    }

    if (email == null || !isValidEmail(email)) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Email invalide").encode());
      return;
    }

    if (message == null || message.trim().isEmpty()) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "Le message est requis").encode());
      return;
    }

    // Send email
    emailService.sendContactFormEmail(adminEmail, name.trim(), email.trim(), message.trim())
      .onSuccess(v -> {
        ctx.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(new JsonObject()
            .put("success", true)
            .put("message", "Email envoyé avec succès")
            .encode());
      })
      .onFailure(err -> {
        ctx.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end(new JsonObject()
            .put("error", "Erreur lors de l'envoi de l'email")
            .put("details", err.getMessage())
            .encode());
      });
  }

  private boolean isValidEmail(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
  }
}
