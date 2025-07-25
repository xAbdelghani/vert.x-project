package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TypeAttestationService {
  // Type Attestation CRUD
  Future<JsonObject> createType(JsonObject data);
  Future<JsonObject> getType(Long id);
  Future<JsonArray> getAllTypes();
  Future<JsonObject> updateType(Long id, JsonObject data);
  Future<Void> deleteType(Long id);

  // Authorization Management
  Future<Void> authorizeCompany(Long compagnieId, Long typeAttestationId);
  Future<Void> unauthorizeCompany(Long compagnieId, Long typeAttestationId);
  Future<JsonArray> getCompanyAuthorizations(Long compagnieId);
  Future<JsonArray> getTypeAuthorizations(Long typeAttestationId);
  Future<JsonObject> getAuthorizationMatrix();
  Future<Void> updateAuthorizationMatrix(JsonArray updates);

  // Check authorization
  Future<Boolean> isCompanyAuthorized(Long compagnieId, Long typeAttestationId);
  Future<JsonArray> getAuthorizedTypesForCompany(Long compagnieId);
}
