package com.example.starter.handler;

import com.example.starter.service.CompagnieRegistrationService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.stream.Collectors;

public class CompagnieHandler {

  private final CompagnieRegistrationService registrationService;

  public CompagnieHandler(CompagnieRegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  // POST /api/compagnies
  public void createCompagnie(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    registrationService.createCompagnie(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/compagnies/:id/account
  public void createAccount(RoutingContext ctx) {

    Long compagnieId = Long.valueOf(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    String nom = body.getString("nom");

    registrationService.createCompagnieAccount(compagnieId, nom)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/compagnies
  public void getAllCompagnies(RoutingContext ctx) {
    registrationService.findAll()
      .onSuccess(list -> {
        JsonArray jsonArray = new JsonArray(
          list.stream()
            .map(JsonObject::mapFrom)
            .collect(Collectors.toList())
        );
        ctx.response()
          .setStatusCode(200)
          .putHeader("content-type", "application/json")
          .end(jsonArray.encode());
      })
      .onFailure(err -> {
        ctx.response()
          .setStatusCode(500)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

  // DELETE /api/compagnies/:id
  public void deleteCompagnie(RoutingContext ctx) {
    Long compagnieId = Long.valueOf(ctx.pathParam("id"));

    registrationService.deleteCompagnie(compagnieId)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204) // No Content
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }


  // DELETE /api/compagnies/:id/account
  public void deleteOnlyAccount(RoutingContext ctx) {
    Long compagnieId = Long.valueOf(ctx.pathParam("id"));

    registrationService.deleteCompagnieAccountOnly(compagnieId)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }




}
