package com.example.starter.handler;

import com.example.starter.service.TypeAbonnementService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TypeAbonnementHandler {

  private final TypeAbonnementService typeAbonnementService;

  public TypeAbonnementHandler(TypeAbonnementService typeAbonnementService) {
    this.typeAbonnementService = typeAbonnementService;
  }

  // POST /api/abonnement-types
  public void createType(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    typeAbonnementService.createType(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/abonnement-types
  public void getAllTypes(RoutingContext ctx) {
    typeAbonnementService.getAllTypes()
      .onSuccess(types -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(types.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/abonnement-types/:id
  public void getType(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    typeAbonnementService.getType(id)
      .onSuccess(type -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(type.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/abonnement-types/categorie/:categorie
  public void getTypesByCategorie(RoutingContext ctx) {
    String categorie = ctx.pathParam("categorie");

    typeAbonnementService.getTypesByCategorie(categorie)
      .onSuccess(types -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(types.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/abonnement-types/active
  public void getActiveTypes(RoutingContext ctx) {
    typeAbonnementService.getActiveTypes()
      .onSuccess(types -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(types.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/abonnement-types/:id
  public void updateType(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    typeAbonnementService.updateType(id, body)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("non trouvé") ? 404 : 400;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

 // DELETE /api/abonnement-types/:id
  public void deleteType(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    typeAbonnementService.deleteType(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("utilisé") ? 409 : 404;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }
}
