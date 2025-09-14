package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public interface DashboardService {

  // Get all dashboard statistics
  Future<JsonObject> getDashboardStats();

  // Get company statistics
  Future<JsonObject> getCompanyStats();

  // Get attestation statistics
  Future<JsonObject> getAttestationStats();

  // Get financial statistics
  Future<JsonObject> getFinancialStats();

  // Get today's activity
  Future<JsonObject> getTodayActivity();

  // Get recent attestations
  Future<JsonObject> getRecentAttestations(int limit);

  // Get low balance companies
  Future<JsonObject> getLowBalanceCompanies(double threshold);

  // Get expiring attestations
  Future<JsonObject> getExpiringAttestations(int days);
}
