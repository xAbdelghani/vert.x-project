package com.example.starter.handler;

import com.example.starter.service.FonctionService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class FonctionHandler {

  private final FonctionService fonctionService;

  public FonctionHandler(FonctionService fonctionService) {
    this.fonctionService = fonctionService;
  }

  public void createFonction(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    fonctionService.createFonction(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getFonction(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    fonctionService.getFonction(id)
      .onSuccess(fonction -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(fonction.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getAllFonctions(RoutingContext ctx) {
    fonctionService.getAllFonctions()
      .onSuccess(fonctions -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(fonctions.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getContactsByFonction(RoutingContext ctx) {
    Long fonctionId = Long.parseLong(ctx.pathParam("id"));

    fonctionService.getContactsByFonction(fonctionId)
      .onSuccess(contacts -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(contacts.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void updateFonction(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    fonctionService.updateFonction(id, body)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("not found") ? 404 : 400;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

  public void deleteFonction(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    fonctionService.deleteFonction(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("associated contacts") ? 409 : 404;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }
}
