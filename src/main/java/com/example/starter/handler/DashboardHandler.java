package com.example.starter.handler;

import com.example.starter.service.DashboardService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class DashboardHandler {

  private final DashboardService dashboardService;

  public DashboardHandler(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  // GET /api/dashboard/stats
  public void getDashboardStats(RoutingContext ctx) {
    dashboardService.getDashboardStats()
      .onSuccess(stats -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(stats.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/companies
  public void getCompanyStats(RoutingContext ctx) {
    dashboardService.getCompanyStats()
      .onSuccess(stats -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(stats.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/attestations
  public void getAttestationStats(RoutingContext ctx) {
    dashboardService.getAttestationStats()
      .onSuccess(stats -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(stats.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/financial
  public void getFinancialStats(RoutingContext ctx) {
    dashboardService.getFinancialStats()
      .onSuccess(stats -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(stats.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/today
  public void getTodayActivity(RoutingContext ctx) {
    dashboardService.getTodayActivity()
      .onSuccess(activity -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(activity.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/recent-attestations
  public void getRecentAttestations(RoutingContext ctx) {
    String limitStr = ctx.queryParam("limit").isEmpty() ? "20" : ctx.queryParam("limit").get(0);
    int limit = Integer.parseInt(limitStr);

    dashboardService.getRecentAttestations(limit)
      .onSuccess(attestations -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestations.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/low-balance
  public void getLowBalanceCompanies(RoutingContext ctx) {
    String thresholdStr = ctx.queryParam("threshold").isEmpty() ? "100" : ctx.queryParam("threshold").get(0);
    double threshold = Double.parseDouble(thresholdStr);

    dashboardService.getLowBalanceCompanies(threshold)
      .onSuccess(companies -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(companies.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/dashboard/expiring
  public void getExpiringAttestations(RoutingContext ctx) {
    String daysStr = ctx.queryParam("days").isEmpty() ? "7" : ctx.queryParam("days").get(0);
    int days = Integer.parseInt(daysStr);

    dashboardService.getExpiringAttestations(days)
      .onSuccess(attestations -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(attestations.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }
}
