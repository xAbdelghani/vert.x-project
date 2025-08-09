package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface VehicleService {
  // Vehicle Model Management
  Future<JsonObject> createModel(JsonObject data);
  Future<JsonObject> getModel(Long id);
  Future<JsonArray> getAllModels();
  Future<JsonArray> getModelsByBrand(String marque);
  Future<JsonArray> getModelsByType(String type);
  Future<JsonArray> getAllBrands();
  Future<JsonArray> getAllTypes();
  Future<JsonObject> updateModel(Long id, JsonObject data);
  Future<Void> deleteModel(Long id);
  Future<JsonArray> searchModels(String marque, String type, String carburant);

  // Vehicle Management
  Future<JsonObject> registerVehicle(JsonObject data);
  Future<JsonObject> getVehicle(Long id);
  Future<JsonObject> getVehicleByPlate(String immatriculation);
  Future<JsonArray> getAllVehicles();
  Future<JsonArray> getCompanyVehicles(Long compagnieId);
  Future<JsonArray> getVehiclesByModel(Long modelId);
  Future<JsonObject> updateVehicle(Long id, JsonObject data);
  Future<Void> deleteVehicle(Long id);
  Future<JsonArray> searchVehicles(String immatriculation, Long modelId, Long compagnieId);

  // Validation
  Future<Boolean> isValidMoroccanPlate(String immatriculation);
}
