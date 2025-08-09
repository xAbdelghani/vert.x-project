package com.example.starter.handler;

import com.example.starter.service.PrepayeService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.math.BigDecimal;

public class PrepayeHandler {

  private final PrepayeService prepayeService;

  public PrepayeHandler(PrepayeService prepayeService) {
    this.prepayeService = prepayeService;
  }

  // GET /api/prepaye/balances
  public void getAllPrepayeBalances(RoutingContext ctx) {
    prepayeService.getAllPrepayeBalances()
      .onSuccess(balances -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(balances.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/prepaye/companies/:id/balance
  public void getPrepayeBalance(RoutingContext ctx)   {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    prepayeService.getPrepayeBalance(compagnieId)
      .onSuccess(balance -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(balance.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }


  // POST /api/prepaye/companies/:id/initialize
  public void initializePrepaye(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    BigDecimal initialAmount = new BigDecimal(body.getString("initial_amount", "0"));
    String devise =body.getString("devise", "MAD");

    prepayeService.initializePrepaye(compagnieId, initialAmount,devise)
      .onSuccess(result -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/prepaye/companies/:id/credit
  public void addCredit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Manual credit");

    prepayeService.addCredit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // POST /api/prepaye/companies/:id/debit
  public void deductCredit(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    BigDecimal amount = new BigDecimal(body.getString("amount"));
    String description = body.getString("description", "Manual debit");

    prepayeService.deductCredit(compagnieId, amount, description)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  // GET /api/prepaye/companies/:id/transactions
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

  // GET /api/prepaye/balances/low?threshold=1000
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

  // POST /api/prepaye/companies/:id/attestation-payment
  public void processAttestationPayment(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    String attestationRef = body.getString("attestation_ref", "");

    prepayeService.processAttestationPayment(compagnieId, attestationRef)
      .onSuccess(result -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(result.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

}
