package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface NotificationService {

  Future<Long> createNotification(JsonObject notificationData);

  Future<JsonObject> getNotification(Long id);

  Future<JsonArray> getAllNotifications();

  Future<JsonArray> getCompanyNotifications(Long companyId);

  Future<JsonArray> getUnreadNotifications();

  Future<JsonArray> getNotificationsByType(String type);

  Future<JsonObject> getUnreadCount();

  Future<Void> markAsRead(Long id);

  Future<Void> markMultipleAsRead(List<Long> ids);

  Future<Void> deleteNotification(Long id);

  Future<Long> createAuthorizationRequest(Long companyId, Long typeAttestationId, JsonObject requestData);
}
