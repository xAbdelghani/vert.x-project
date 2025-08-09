package com.example.starter.service.impl;

import com.example.starter.model.Notification;
import com.example.starter.repository.NotificationRepository;
import com.example.starter.service.NotificationService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  public NotificationServiceImpl(NotificationRepository notificationRepository) {
    this.notificationRepository = notificationRepository;
  }

  @Override
  public Future<Long> createNotification(JsonObject notificationData) {
    // Validate required fields
    if (!notificationData.containsKey("message")) {
      return Future.failedFuture("Le message est requis");
    }

    Notification notification = new Notification();

    // Handle company_id - it might come as string or number
    Object companyIdObj = notificationData.getValue("company_id");
    if (companyIdObj != null) {
      if (companyIdObj instanceof String) {
        notification.setCompanyId(Long.parseLong((String) companyIdObj));
      } else if (companyIdObj instanceof Number) {
        notification.setCompanyId(((Number) companyIdObj).longValue());
      }
    }

    notification.setMessage(notificationData.getString("message"));
    notification.setDescription(notificationData.getString("description"));
    notification.setRead(false);
    notification.setType(notificationData.getString("type", "INFO"));
    notification.setMetadata(notificationData.getJsonObject("metadata") != null ?
      notificationData.getJsonObject("metadata").encode() : null);

    // Handle timestamp - parse ISO 8601 with timezone
    String timestampStr = notificationData.getString("timestamp");
    if (timestampStr != null) {
      try {
        // Parse ISO 8601 instant and convert to LocalDateTime
        Instant instant = Instant.parse(timestampStr);
        notification.setTimestamp(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
      } catch (Exception e) {
        // Fallback to try parsing without timezone
        try {
          notification.setTimestamp(LocalDateTime.parse(timestampStr, DATE_FORMATTER));
        } catch (Exception e2) {
          // If all parsing fails, use current time
          notification.setTimestamp(LocalDateTime.now());
        }
      }
    } else {
      notification.setTimestamp(LocalDateTime.now());
    }

    return notificationRepository.save(notification);
  }

  @Override
  public Future<JsonObject> getNotification(Long id) {
    return notificationRepository.findById(id)
      .map(notification -> {
        if (notification == null) {
          throw new RuntimeException("Notification non trouvée");
        }
        return notificationToJson(notification);
      });
  }

  @Override
  public Future<JsonArray> getAllNotifications() {
    return notificationRepository.findAll()
      .map(notifications -> {
        JsonArray array = new JsonArray();
        notifications.forEach(notification -> array.add(notificationToJson(notification)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getCompanyNotifications(Long companyId) {
    return notificationRepository.findByCompanyId(companyId)
      .map(notifications -> {
        JsonArray array = new JsonArray();
        notifications.forEach(notification -> array.add(notificationToJson(notification)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getUnreadNotifications() {
    return notificationRepository.findUnread()
      .map(notifications -> {
        JsonArray array = new JsonArray();
        notifications.forEach(notification -> array.add(notificationToJson(notification)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getNotificationsByType(String type) {
    return notificationRepository.findByType(type)
      .map(notifications -> {
        JsonArray array = new JsonArray();
        notifications.forEach(notification -> array.add(notificationToJson(notification)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> getUnreadCount() {
    return notificationRepository.countUnread()
      .map(count -> new JsonObject().put("count", count));
  }

  @Override
  public Future<Void> markAsRead(Long id) {
    return notificationRepository.markAsRead(id);
  }

  @Override
  public Future<Void> markMultipleAsRead(List<Long> ids) {
    return notificationRepository.markMultipleAsRead(ids);
  }

  @Override
  public Future<Void> deleteNotification(Long id) {
    return notificationRepository.delete(id);
  }

  @Override
  public Future<Long> createAuthorizationRequest(Long companyId, Long typeAttestationId, JsonObject requestData) {
    JsonObject metadata = new JsonObject()
      .put("type_attestation_id", typeAttestationId)
      .put("urgency", requestData.getString("urgency"))
      .put("usage", requestData.getString("usage"))
      .put("reason", requestData.getString("reason"));

    JsonObject notificationData = new JsonObject()
      .put("company_id", companyId) // This is already a Long
      .put("message", "Demande d'autorisation - " + requestData.getString("company_name"))
      .put("description", "Demande d'autorisation pour: " + requestData.getString("attestation_type") +
        ". Urgence: " + requestData.getString("urgency") +
        ". Raison: " + requestData.getString("reason"))
      .put("type", "AUTHORIZATION_REQUEST")
      .put("metadata", metadata);

    return createNotification(notificationData);
  }

  private JsonObject notificationToJson(Notification notification) {
    JsonObject json = new JsonObject()
      .put("id", notification.getId())
      .put("company_id", notification.getCompanyId())
      .put("message", notification.getMessage())
      .put("read", notification.getRead())
      // Convert back to ISO 8601 with Z
      .put("timestamp", notification.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toString());

    if (notification.getDescription() != null) {
      json.put("description", notification.getDescription());
    }

    if (notification.getType() != null) {
      json.put("type", notification.getType());
    }

    if (notification.getMetadata() != null) {
      try {
        json.put("metadata", new JsonObject(notification.getMetadata()));
      } catch (Exception e) {
        json.put("metadata", notification.getMetadata());
      }
    }

    return json;
  }
}
