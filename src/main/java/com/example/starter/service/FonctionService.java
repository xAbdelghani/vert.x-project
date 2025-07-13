package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface FonctionService {
  Future<Long> createFonction(JsonObject data);
  Future<JsonObject> getFonction(Long id);
  Future<JsonArray> getAllFonctions();
  Future<JsonArray> getContactsByFonction(Long fonctionId);
  Future<Void> updateFonction(Long id, JsonObject data);
  Future<Void> deleteFonction(Long id);
}
