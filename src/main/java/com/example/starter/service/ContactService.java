package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ContactService {

  Future<Long> createContact(JsonObject data);
  Future<JsonObject> getContact(Long id);
  Future<JsonArray> getAllContacts();
  Future<JsonArray> getContactsByCompagnie(Long compagnieId);
  Future<Void> updateContact(Long id, JsonObject data);
  Future<Void> deleteContact(Long id);

}
