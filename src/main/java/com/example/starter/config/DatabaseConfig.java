package com.example.starter.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class DatabaseConfig {

  private final JsonObject config;

  public DatabaseConfig(JsonObject config) {
    this.config = config;
  }

  public PgPool createPgPool(Vertx vertx) {
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setHost(config.getString("host"))
      .setPort(config.getInteger("port"))
      .setDatabase(config.getString("database"))
      .setUser(config.getString("user"))
      .setPassword(config.getString("password"));

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    return PgPool.pool(vertx, connectOptions, poolOptions);
  }
}
