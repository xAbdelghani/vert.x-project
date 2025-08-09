package com.example.starter.handler;

import com.example.starter.service.NotificationService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

public class NotificationHandler {

  private final NotificationService notificationService;

  public NotificationHandler(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  // POST /api/notifications
  public void createNotification(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    notificationService.createNotification(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/notifications
  public void getAllNotifications(RoutingContext ctx) {
    String readParam = ctx.queryParam("read").stream().findFirst().orElse(null);
    String typeParam = ctx.queryParam("type").stream().findFirst().orElse(null);

    Future<JsonArray> future;

    if ("false".equals(readParam)) {
      future = notificationService.getUnreadNotifications();
    } else if (typeParam != null) {
      future = notificationService.getNotificationsByType(typeParam);
    } else {
      future = notificationService.getAllNotifications();
    }

    future
      .onSuccess(notifications -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(notifications.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/notifications/:id
  public void getNotification(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    notificationService.getNotification(id)
      .onSuccess(notification -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(notification.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/notifications/company/:companyId
  public void getCompanyNotifications(RoutingContext ctx) {
    Long companyId = Long.parseLong(ctx.pathParam("companyId"));

    notificationService.getCompanyNotifications(companyId)
      .onSuccess(notifications -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(notifications.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/notifications/unread-count
  public void getUnreadCount(RoutingContext ctx) {
    notificationService.getUnreadCount()
      .onSuccess(count -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(count.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/notifications/:id/read
  public void markAsRead(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    notificationService.markAsRead(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/notifications/read-multiple
  public void markMultipleAsRead(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    JsonArray idsArray = body.getJsonArray("ids");

    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < idsArray.size(); i++) {
      ids.add(idsArray.getLong(i));
    }

    notificationService.markMultipleAsRead(ids)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // DELETE /api/notifications/:id
  public void deleteNotification(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    notificationService.deleteNotification(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/notifications/authorization-request
  // POST /api/notifications/authorization-request
  public void createAuthorizationRequest(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    // Parse IDs handling both string and number types
    Long companyId = parseId(body.getValue("company_id"));
    Long typeAttestationId = parseId(body.getValue("type_attestation_id"));

    if (companyId == null || typeAttestationId == null) {
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", "company_id and type_attestation_id are required").encode());
      return;
    }

    notificationService.createAuthorizationRequest(companyId, typeAttestationId, body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // Helper method to parse ID from various types
  private Long parseId(Object value) {
    if (value == null) return null;
    if (value instanceof String) {
      try {
        return Long.parseLong((String) value);
      } catch (NumberFormatException e) {
        return null;
      }
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return null;
  }
}
