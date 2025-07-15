package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface RelationPointventeCompagnieService {
  Future<Long> linkCompagnieToPointvente(Long pointventeId, Long compagnieId);
  Future<Void> updateRelationStatus(Long relationId, String status, String reason);
  Future<JsonArray> getHistoriqueForRelation(Long relationId);
  Future<JsonObject> getRelationDetails(Long relationId);
  Future<Void> unlinkCompagnie(Long relationId);
}
