package com.example.starter.handler;

import com.example.starter.dto.CreateAgenceRequest;
import com.example.starter.dto.UpdateAgenceRequest;
import com.example.starter.service.AgenceService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import jakarta.ws.rs.NotFoundException;


public class AgenceHandler {

  private final AgenceService agenceService;

  public AgenceHandler(AgenceService agenceService) {
    this.agenceService = agenceService;
  }

  public void create(RoutingContext ctx) {
    CreateAgenceRequest request = ctx.body().asPojo(CreateAgenceRequest.class);

    agenceService.create(request)
      .onSuccess(agence -> {
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(Json.encode(agence));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void getById(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    agenceService.findById(id)
      .onSuccess(agence -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(agence));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void getAll(RoutingContext ctx) {
    agenceService.findAll()
      .onSuccess(agences -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(agences));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void getByCompagnie(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("compagnieId"));

    agenceService.findByCompagnie(compagnieId)
      .onSuccess(agences -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(agences));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void getByStatus(RoutingContext ctx) {
    String status = ctx.queryParam("status").get(0);

    agenceService.findByStatus(status)
      .onSuccess(agences -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(agences));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void update(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    UpdateAgenceRequest request = ctx.body().asPojo(UpdateAgenceRequest.class);

    agenceService.update(id, request)
      .onSuccess(agence -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(agence));
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void closeAgence(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    agenceService.closeAgence(id)
      .onSuccess(result -> {
        if (result) {
          ctx.response()
            .putHeader("content-type", "application/json")
            .end(new JsonObject()
              .put("message", "Agence closed successfully")
              .encode());
        } else {
          ctx.response().setStatusCode(404).end();
        }
      })
      .onFailure(err -> handleError(ctx, err));
  }

  public void delete(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    agenceService.delete(id)
      .onSuccess(deleted -> {
        if (deleted) {
          ctx.response().setStatusCode(204).end();
        } else {
          ctx.response().setStatusCode(404).end();
        }
      })
      .onFailure(err -> handleError(ctx, err));
  }

  private void handleError(RoutingContext ctx, Throwable err) {
    if (err instanceof NotFoundException) {
      ctx.response()
        .setStatusCode(404)
        .end(new JsonObject().put("error", err.getMessage()).encode());
    } else if (err instanceof Exception) {
      ctx.response()
        .setStatusCode(400)
        .end(new JsonObject().put("error", err.getMessage()).encode());
    } else {
      ctx.response()
        .setStatusCode(500)
        .end(new JsonObject().put("error", "Internal server error").encode());
    }
  }



}
