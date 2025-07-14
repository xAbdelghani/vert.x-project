package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PointventeService {
  Future<Long> createPointvente(JsonObject data);
  Future<JsonObject> getPointvente(Long id);
  Future<JsonArray> getAllPointventes();
  Future<Void> updatePointvente(Long id, JsonObject data);
  Future<Void> deletePointvente(Long id);
  Future<JsonArray> getCompagniesForPointvente(Long pointventeId);
}
