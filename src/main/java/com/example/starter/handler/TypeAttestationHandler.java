package com.example.starter.handler;

import com.example.starter.service.TypeAttestationService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class TypeAttestationHandler {

  private final TypeAttestationService typeAttestationService;

  public TypeAttestationHandler(TypeAttestationService typeAttestationService) {
    this.typeAttestationService = typeAttestationService;
  }

  // POST /api/type-attestations
  public void createType(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    typeAttestationService.createType(body)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/type-attestations
  public void getAllTypes(RoutingContext ctx) {
    typeAttestationService.getAllTypes()
      .onSuccess(types -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(types.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/type-attestations/:id
  public void getType(RoutingContext ctx) {

    Long id = Long.parseLong(ctx.pathParam("id"));
    typeAttestationService.getType(id)
      .onSuccess(type -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(type.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/type-attestations/:id
  public void updateType(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    typeAttestationService.updateType(id, body)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/type-attestations/:id
  public void deleteType(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    typeAttestationService.deleteType(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/companies/:companyId/authorize-attestation/:typeId
  public void authorizeCompany(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("companyId"));
    Long typeId = Long.parseLong(ctx.pathParam("typeId"));

    typeAttestationService.authorizeCompany(compagnieId, typeId)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/companies/:companyId/authorize-attestation/:typeId
  public void unauthorizeCompany(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("companyId"));
    Long typeId = Long.parseLong(ctx.pathParam("typeId"));

    typeAttestationService.unauthorizeCompany(compagnieId, typeId)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/authorized-attestations
  public void getCompanyAuthorizations(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    typeAttestationService.getCompanyAuthorizations(compagnieId)
      .onSuccess(authorizations -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(authorizations.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/authorization-matrix
  public void getAuthorizationMatrix(RoutingContext ctx) {
    typeAttestationService.getAuthorizationMatrix()
      .onSuccess(matrix -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(matrix.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/authorization-matrix
  public void updateAuthorizationMatrix(RoutingContext ctx) {
    JsonArray updates = ctx.body().asJsonArray();

    typeAttestationService.updateAuthorizationMatrix(updates)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/can-generate/:typeId
  public void checkAuthorization(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    Long typeId = Long.parseLong(ctx.pathParam("typeId"));

    typeAttestationService.isCompanyAuthorized(compagnieId, typeId)
      .onSuccess(authorized -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
          .put("authorized", authorized)
          .encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }
}
