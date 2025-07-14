package com.example.starter.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.StartTLSOptions;

public class MailClientConfig {

    // Renamed class to avoid conflict
  public static MailClient create(Vertx vertx, JsonObject config) {

    MailConfig mailConfig = new MailConfig()
      .setHostname(config.getString("host", "smtp.gmail.com"))
      .setPort(config.getInteger("port", 587))
      .setStarttls(StartTLSOptions.REQUIRED)
      .setUsername(config.getString("username"))
      .setPassword(config.getString("password"))
      .setAuthMethods("PLAIN LOGIN")
      .setSsl(config.getBoolean("ssl", false))
      .setTrustAll(config.getBoolean("trustAll", false));

    return MailClient.createShared(vertx, mailConfig);
  }
}
