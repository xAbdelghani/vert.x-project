package com.example.starter.handler;

import com.example.starter.service.PointventeService;
import com.example.starter.service.RelationPointventeCompagnieService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class PointventeHandler {

  private final PointventeService pointventeService;
  private final RelationPointventeCompagnieService relationService;

  public PointventeHandler(
    PointventeService pointventeService,
    RelationPointventeCompagnieService relationService
  ) {
    this.pointventeService = pointventeService;
    this.relationService = relationService;
  }

  // POST /api/pointventes
  public void createPointvente(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    pointventeService.createPointvente(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/pointventes
  public void getAllPointventes(RoutingContext ctx) {
    pointventeService.getAllPointventes()
      .onSuccess(pointventes -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(pointventes.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/pointventes/:id
  public void getPointvente(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    pointventeService.getPointvente(id)
      .onSuccess(pointvente -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(pointvente.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/pointventes/:id
  public void updatePointvente(RoutingContext ctx) {

    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    pointventeService.updatePointvente(id, body)
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

  // DELETE /api/pointventes/:id
  public void deletePointvente(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    pointventeService.deletePointvente(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("linked companies") ? 409 : 404;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

  // GET /api/pointventes/:id/compagnies
  public void getCompagniesForPointvente(RoutingContext ctx) {
    Long pointventeId = Long.parseLong(ctx.pathParam("id"));

    pointventeService.getCompagniesForPointvente(pointventeId)
      .onSuccess(companies -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(companies.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/pointventes/:id/compagnies
  public void linkCompagnieToPointvente(RoutingContext ctx) {
    Long pointventeId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    Long compagnieId = body.getLong("compagnie_id");

    if (compagnieId == null) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "compagnie_id is required").encode());
      return;
    }

    relationService.linkCompagnieToPointvente(pointventeId, compagnieId)
      .onSuccess(relationId -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", relationId).encode()))
      .onFailure(err -> {
        int statusCode = 400;
        if (err.getMessage().contains("not found")) {
          statusCode = 404;
        } else if (err.getMessage().contains("already exists")) {
          statusCode = 409;
        }
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

  // PUT /api/relations/:relationId/status
  public void updateRelationStatus(RoutingContext ctx) {
    Long relationId = Long.parseLong(ctx.pathParam("relationId"));
    JsonObject body = ctx.body().asJsonObject();
    String status = body.getString("status");
    String reason = body.getString("reason");

    if (status == null || status.trim().isEmpty()) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "status is required").encode());
      return;
    }

    relationService.updateRelationStatus(relationId, status, reason)
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

  // GET /api/relations/:relationId/historique
  public void getHistoriqueForRelation(RoutingContext ctx) {
    Long relationId = Long.parseLong(ctx.pathParam("relationId"));

    relationService.getHistoriqueForRelation(relationId)
      .onSuccess(historique -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(historique.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/relations/:relationId
  public void unlinkCompagnie(RoutingContext ctx) {
    Long relationId = Long.parseLong(ctx.pathParam("relationId"));

    relationService.unlinkCompagnie(relationId)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }


}
