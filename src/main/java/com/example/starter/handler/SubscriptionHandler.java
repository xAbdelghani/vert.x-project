package com.example.starter.handler;

import com.example.starter.model.constant.AbonnementStatus;
import com.example.starter.service.SubscriptionService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.math.BigDecimal;

public class SubscriptionHandler {

  private final SubscriptionService subscriptionService;

  public SubscriptionHandler(SubscriptionService subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  // POST /api/subscriptions
  public void createSubscription(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    subscriptionService.createSubscription(body)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/subscription
  public void getCompanySubscription(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    subscriptionService.getCompanySubscription(compagnieId)
      .onSuccess(subscription -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(subscription.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/subscriptions
  public void getAllSubscriptions(RoutingContext ctx) {
    subscriptionService.getAllSubscriptions()
      .onSuccess(subscriptions -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(subscriptions.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/subscriptions/category/:category
  public void getSubscriptionsByCategory(RoutingContext ctx) {
    String category = ctx.pathParam("category");

    subscriptionService.getSubscriptionsByCategory(category)
      .onSuccess(subscriptions -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(subscriptions.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // PUT /api/subscriptions/:id
  public void updateSubscription(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    subscriptionService.updateSubscription(id, body)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/can-create-subscription
  public void canCreateSubscription(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    subscriptionService.canCreateSubscription(compagnieId)
      .onSuccess(canCreate -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject()
          .put("can_create", canCreate)
          .encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void changeStatus(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    String newStatus = body.getString("status");
    String reason = body.getString("reason", "");

    subscriptionService.changeStatus(id, newStatus, reason)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void suspendSubscription(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    String reason = body.getString("reason", "Manual suspension");

    subscriptionService.suspendSubscription(id, reason)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void reactivateSubscription(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    subscriptionService.reactivateSubscription(id)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // Credit Management Endpoints
  public void getCreditUsage(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    subscriptionService.getCreditUsage(compagnieId)
      .onSuccess(usage -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(usage.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }


  public void useCredit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Credit usage");

    subscriptionService.useCredit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // Deposit Management Endpoints
  public void getDepositStatus(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    subscriptionService.getDepositStatus(compagnieId)
      .onSuccess(status -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(status.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void useDeposit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();
    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Deposit usage");

    subscriptionService.useDeposit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // Get available statuses
  public void getAvailableStatuses(RoutingContext ctx) {
    ctx.response()
      .putHeader("content-type", "application/json")
      .end(AbonnementStatus.getAllStatuses().encode());
  }



}
