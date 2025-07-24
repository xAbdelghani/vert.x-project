package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public interface SubscriptionService {
  // Existing methods
  Future<JsonObject> createSubscription(JsonObject data);
  Future<JsonObject> getCompanySubscription(Long compagnieId);
  Future<JsonArray> getAllSubscriptions();
  Future<JsonArray> getSubscriptionsByCategory(String category);
  Future<JsonObject> updateSubscription(Long id, JsonObject data);
  Future<Boolean> canCreateSubscription(Long compagnieId);

  // B) Status Management
  Future<JsonObject> changeStatus(Long subscriptionId, String newStatus, String reason);
  Future<JsonObject> suspendSubscription(Long subscriptionId, String reason);
  Future<JsonObject> reactivateSubscription(Long subscriptionId);
  Future<JsonObject> expireSubscription(Long subscriptionId);
  Future<JsonObject> terminateSubscription(Long subscriptionId, String reason);

  // C) Credit Management for AVANCE
  Future<Boolean> canUseCredit(Long compagnieId, BigDecimal amount);
  Future<JsonObject> useCredit(Long compagnieId, BigDecimal amount, String description);
  Future<JsonObject> getCreditUsage(Long compagnieId);

  // D) Deposit Management for CAUTION
  Future<Boolean> canUseDeposit(Long compagnieId, BigDecimal amount);
  Future<JsonObject> useDeposit(Long compagnieId, BigDecimal amount, String description);
  Future<JsonObject> releaseDeposit(Long compagnieId, BigDecimal amount, String reason);
  Future<JsonObject> getDepositStatus(Long compagnieId);
}
