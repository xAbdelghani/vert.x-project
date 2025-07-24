package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public interface BalanceService {
  Future<JsonObject> getCompanyBalance(Long compagnieId);
  Future<JsonArray> getAllBalances();
  Future<JsonObject> addCredit(Long compagnieId, BigDecimal amount, String description);
  Future<JsonObject> deductCredit(Long compagnieId, BigDecimal amount, String description);
  Future<Boolean> hassufficientBalance(Long compagnieId, BigDecimal amount);
  Future<JsonArray> getLowBalanceCompanies(BigDecimal threshold);
  Future<JsonArray> getTransactionHistory(Long compagnieId);
  Future<JsonObject> initializeCompanyBalance(Long compagnieId, String paymentModel, BigDecimal initialAmount, String devise);
}
