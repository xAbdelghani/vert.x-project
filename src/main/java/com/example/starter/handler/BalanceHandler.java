package com.example.starter.handler;

import com.example.starter.service.BalanceService;
import com.example.starter.service.PrepayeService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.math.BigDecimal;

public class BalanceHandler {

  private final PrepayeService prepayeService;

  private final BalanceService balanceService; // For AVANCE/CAUTION operations

  public BalanceHandler(PrepayeService prepayeService, BalanceService balanceService) {
    this.prepayeService = prepayeService;
    this.balanceService = balanceService;
  }

  // GET /api/balances - Returns only PREPAYE balances now
  public void getAllBalances(RoutingContext ctx) {
    prepayeService.getAllPrepayeBalances()
      .onSuccess(balances -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(balances.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/balance
  public void getCompanyBalance(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    // Check if it's a PREPAYE company first
    prepayeService.getPrepayeBalance(compagnieId)
      .onSuccess(balance -> {
        if (!"NO_PREPAYE_BALANCE".equals(balance.getString("status"))) {
          ctx.response()
            .putHeader("content-type", "application/json")
            .end(balance.encode());
        } else {
          // Fall back to subscription-based balance (AVANCE/CAUTION)
          balanceService.getCompanyBalance(compagnieId)
            .onSuccess(subBalance -> ctx.response()
              .putHeader("content-type", "application/json")
              .end(subBalance.encode()))
            .onFailure(err -> ctx.response()
              .setStatusCode(404)
              .putHeader("content-type", "application/json")
              .end(new JsonObject().put("error", err.getMessage()).encode()));
        }
      })
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/companies/:id/balance/credit
  public void addCredit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Manual credit");

    // For PREPAYE companies only
    prepayeService.addCredit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/companies/:id/balance/debit
  public void deductCredit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Manual debit");

    // For PREPAYE companies only
    prepayeService.deductCredit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/companies/:id/transactions
  public void getTransactionHistory(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));

    prepayeService.getTransactionHistory(compagnieId)
      .onSuccess(transactions -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(transactions.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/companies/:id/balance/initialize
  public void initializeBalance(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    String paymentModel = body.getString("payment_model");
    BigDecimal initialAmount = new BigDecimal(body.getString("initial_amount", "0"));
    String devise = body.getString("devise", "MAD"); // Get devise from request, default to MAD

    if ("PREPAYE".equals(paymentModel)) {
      // Handle PREPAYE initialization with devise
      prepayeService.initializePrepaye(compagnieId, initialAmount, devise)
        .onSuccess(result -> ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(result.encode()))
        .onFailure(err -> ctx.response()
          .setStatusCode(400)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode()));
    } else {
      // Handle AVANCE/CAUTION through subscription system with devise
      balanceService.initializeCompanyBalance(compagnieId, paymentModel, initialAmount, devise)
        .onSuccess(result -> ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(result.encode()))
        .onFailure(err -> ctx.response()
          .setStatusCode(400)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode()));
    }
  }

  // GET /api/balances/low?threshold=1000
  public void getLowBalances(RoutingContext ctx) {
    String thresholdStr = ctx.queryParam("threshold").get(0);
    BigDecimal threshold = new BigDecimal(thresholdStr != null ? thresholdStr : "1000");

    prepayeService.getLowBalanceCompanies(threshold)
      .onSuccess(companies -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(companies.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }
}
