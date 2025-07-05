package com.example.starter.verticle;

import com.example.starter.config.DatabaseConfig;
import com.example.starter.config.KeycloakConfig;
import com.example.starter.config.ServiceRegistry;
import com.example.starter.router.AppRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.auth.oauth2.OAuth2Auth;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    // Database setup
    var pgPool = new DatabaseConfig(config().getJsonObject("database"))
      .createPgPool(vertx);

    // Initialize all services with registry
    OAuth2Auth oauth2Auth = KeycloakConfig.create(vertx, config().getJsonObject("keycloak"));

    var serviceRegistry = new ServiceRegistry(pgPool,oauth2Auth,config(),vertx);

    // Create router with registry
    var router = new AppRouter(vertx, serviceRegistry).createRouter();

    // Start server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(server -> {
        System.out.println("Server started on port 8080");
        startPromise.complete();
      })
      .onFailure(startPromise::fail);
  }


}
