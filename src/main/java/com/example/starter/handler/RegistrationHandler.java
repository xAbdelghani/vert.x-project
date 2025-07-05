package com.example.starter.handler;

import com.example.starter.service.RegistrationService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.validation.ValidationException;

public class RegistrationHandler {

  private final RegistrationService registrationService;

  public RegistrationHandler(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  public void register(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    registrationService.registerUser(body)
      .onSuccess(result -> {
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(result.encode());
      })
      .onFailure(err -> {
        int statusCode = err instanceof ValidationException ? 400 : 500;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject()
            .put("error", err.getMessage())
            .encode());
      });
  }
}
