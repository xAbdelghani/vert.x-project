package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;

public interface PrepayeService {

  // Initialize a company with PREPAYE model


  Future<JsonObject> initializePrepaye(Long compagnieId, BigDecimal initialAmount, String devise);

  // Get all PREPAYE companies with their balances
  Future<JsonArray> getAllPrepayeBalances();

  // Get specific company's PREPAYE balance
  Future<JsonObject> getPrepayeBalance(Long compagnieId);

  // Add credit to PREPAYE account
  Future<JsonObject> addCredit(Long compagnieId, BigDecimal amount, String description);

  // Deduct from PREPAYE account
  Future<JsonObject> deductCredit(Long compagnieId, BigDecimal amount, String description);

  // Check if company has sufficient PREPAYE balance
  Future<Boolean> hasSufficientBalance(Long compagnieId, BigDecimal amount);

  // Get companies with low PREPAYE balance
  Future<JsonArray> getLowBalanceCompanies(BigDecimal threshold);

  // Get transaction history for PREPAYE company
  Future<JsonArray> getTransactionHistory(Long compagnieId);

  // Process attestation payment for PREPAYE
  Future<JsonObject> processAttestationPayment(Long compagnieId, String attestationRef);
}
