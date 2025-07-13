package com.example.starter.handler;

import com.example.starter.service.UserService;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserHandler {

  private final UserService userService;

  public UserHandler(UserService userService) {
    this.userService = userService;
  }

  public void getUser(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    userService.getUser(id)
      .onSuccess(user -> {
        if (user == null) {
          ctx.response().setStatusCode(404).end();
        } else {
          ctx.response()
            .putHeader("content-type", "application/json")
            .end(JsonObject.mapFrom(user).encode());
        }
      })
      .onFailure(err -> ctx.response().setStatusCode(500).end());
  }

  public void getAllUsers(RoutingContext ctx) {
    userService.getAllUsers()
      .onSuccess(users -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(Json.encode(users));
      })
      .onFailure(err -> ctx.response().setStatusCode(500).end());
  }


  public void createUser(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    String name = body.getString("name");
    userService.createUser(name)
      .onSuccess(user -> {
        ctx.response()
          .setStatusCode(201)
          .putHeader("content-type", "application/json")
          .end(JsonObject.mapFrom(user).encode());
      })
      .onFailure(err -> ctx.response().setStatusCode(500).end());
  }

  public void updateUser(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    String name = ctx.body().asJsonObject().getString("name");

    userService.updateUser(id, name)
      .onSuccess(user -> {
        ctx.response()
          .putHeader("content-type", "application/json")
          .end(JsonObject.mapFrom(user).encode());
      })
      .onFailure(err -> ctx.response().setStatusCode(500).end());
  }

  public void deleteUser(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    userService.deleteUser(id)
      .onSuccess(deleted -> {
        if (deleted) {
          ctx.response().setStatusCode(204).end();
        } else {
          ctx.response().setStatusCode(404).end();
        }
      })
      .onFailure(err -> ctx.response().setStatusCode(500).end());
  }


}
