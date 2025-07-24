package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface TypeAbonnementService {
  Future<Long> createType(JsonObject data);
  Future<JsonObject> getType(Long id);
  Future<JsonArray> getAllTypes();
  Future<JsonArray> getTypesByCategorie(String categorie);
  Future<JsonArray> getActiveTypes();
  Future<Void> updateType(Long id, JsonObject data);
  Future<Void> deleteType(Long id);
}
