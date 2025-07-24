package com.example.starter.handler;

import com.example.starter.model.constant.AbonnementStatus;
import io.vertx.ext.web.RoutingContext;

public class AbonnementStatusHandler {

  // GET /api/abonnement-statuses
  public void getAvailableStatuses(RoutingContext ctx) {
    ctx.response()
      .putHeader("content-type", "application/json")
      .end(AbonnementStatus.getAllStatuses().encode());
  }


}
