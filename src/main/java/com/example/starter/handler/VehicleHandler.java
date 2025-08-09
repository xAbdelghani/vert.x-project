package com.example.starter.handler;

import com.example.starter.service.VehicleService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


public class VehicleHandler {

  private final VehicleService vehicleService;

  public VehicleHandler(VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  // VEHICLE MODEL ENDPOINTS

  // POST /api/vehicle-models
  public void createModel(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    vehicleService.createModel(body)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicle-models
  public void getAllModels(RoutingContext ctx) {
    vehicleService.getAllModels()
      .onSuccess(models -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(models.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicle-models/:id
  public void getModel(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    vehicleService.getModel(id)
      .onSuccess(model -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(model.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/vehicle-models/:id
  public void updateModel(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    vehicleService.updateModel(id, body)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/vehicle-models/:id
  public void deleteModel(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    vehicleService.deleteModel(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicle-models/brands
  public void getAllBrands(RoutingContext ctx) {

    vehicleService.getAllBrands()
      .onSuccess(brands -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(brands.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));

  }

  // GET /api/vehicle-models/types
  public void getAllTypes(RoutingContext ctx) {
    vehicleService.getAllTypes()
      .onSuccess(types -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(types.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicle-models/search
  public void searchModels(RoutingContext ctx) {
    String marque = ctx.queryParam("marque").isEmpty() ? null : ctx.queryParam("marque").get(0);
    String type = ctx.queryParam("type").isEmpty() ? null : ctx.queryParam("type").get(0);
    String carburant = ctx.queryParam("carburant").isEmpty() ? null : ctx.queryParam("carburant").get(0);

    vehicleService.searchModels(marque, type, carburant)
      .onSuccess(models -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(models.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // VEHICLE ENDPOINTS

  // POST /api/vehicles
  public void registerVehicle(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    vehicleService.registerVehicle(body)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicles
  public void getAllVehicles(RoutingContext ctx) {
    vehicleService.getAllVehicles()
      .onSuccess(vehicles -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(vehicles.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicles/:id
  public void getVehicle(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    vehicleService.getVehicle(id)
      .onSuccess(vehicle -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(vehicle.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicles/plate/:immatriculation
  public void getVehicleByPlate(RoutingContext ctx) {
    String immatriculation = ctx.pathParam("immatriculation");

    vehicleService.getVehicleByPlate(immatriculation)
      .onSuccess(vehicle -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(vehicle.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/vehicles/:id
  public void updateVehicle(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    vehicleService.updateVehicle(id, body)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/vehicles/:id
  public void deleteVehicle(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    vehicleService.deleteVehicle(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/vehicles
  public void getCompanyVehicles(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    vehicleService.getCompanyVehicles(compagnieId)
      .onSuccess(vehicles -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(vehicles.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/vehicles/search
  public void searchVehicles(RoutingContext ctx) {
    String immatriculation = ctx.queryParam("immatriculation").isEmpty() ? null : ctx.queryParam("immatriculation").get(0);
    Long modelId = ctx.queryParam("model_id").isEmpty() ? null : Long.parseLong(ctx.queryParam("model_id").get(0));
    Long compagnieId = ctx.queryParam("compagnie_id").isEmpty() ? null : Long.parseLong(ctx.queryParam("compagnie_id").get(0));

    vehicleService.searchVehicles(immatriculation, modelId, compagnieId)
      .onSuccess(vehicles -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(vehicles.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }


}
