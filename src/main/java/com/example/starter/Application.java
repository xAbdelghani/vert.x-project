package com.example.starter;


import com.example.starter.verticle.MainVerticle;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

public class Application {

  public static void main(String[] args) {

    System.out.println(">>> Application started");
    Vertx vertx = Vertx.vertx();
    JsonObject config = new JsonObject()
      .put("http", new JsonObject().put("port", 8080))
      .put("database", new JsonObject()
        .put("host", "localhost")
        .put("port", 5432)
        .put("database", "vertxData")
        .put("user", "postgres")
        .put("password", "user"))
      .put("keycloak", new JsonObject()
        .put("realm", "Digitalisation")
        .put("authServerUrl", "http://localhost:8180")
        .put("clientId", "Digitalisation-attestation")
        .put("clientSecret", "qP3rNuWq6UXMHO1MDhQAPSiqedo5Exm8")
        .put("adminClientId", "admin-cli")
        .put("adminClientSecret", "qKkB4pflBaXmYb299In5vkjJlVp7XLRQ"))
        .put("mail", new JsonObject()
        .put("host", "smtp.gmail.com")
        .put("port", 587)
        .put("username", "rabom325@gmail.com")
        .put("password", "fnna sfca qwun wxbe")
        .put("ssl", false)
        .put("trustAll", false));

    DatabindCodec.mapper().registerModule(new JavaTimeModule());
    DatabindCodec.prettyMapper().registerModule(new JavaTimeModule());

    vertx.deployVerticle(new MainVerticle(), new DeploymentOptions().setConfig(config))
      .onSuccess(id -> System.out.println("MainVerticle deployed: " + id))
      .onFailure(err -> {
        System.err.println("Failed to deploy MainVerticle");
        err.printStackTrace();
      });
  }


}
