package com.example.starter.router;

import com.example.starter.config.ServiceRegistry;
import com.example.starter.handler.RegistrationHandler;
import com.example.starter.handler.UserHandler;
import com.example.starter.security.PolicyEnforcerHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;


public class AppRouter {

  private final Vertx vertx;
  private final ServiceRegistry registry;

  public AppRouter(Vertx vertx, ServiceRegistry registry) {
    this.vertx = vertx;
    this.registry = registry;
  }

  public Router createRouter() {

    Router router = Router.router(vertx);
    // Global handlers
    router.route().handler(BodyHandler.create());
    // Public routes (no auth)
    router.get("/health").handler(ctx -> ctx.response().end("OK"));
    // Apply Policy Enforcer to ALL routes (just like Spring Boot)
    PolicyEnforcerHandler policyEnforcer = new PolicyEnforcerHandler(registry.getOAuth2Auth());
    router.route("/*").handler(policyEnforcer);
    // Mount all entity routes
    mountUserRoutes(router);
    mountCreateRegister(router);

    return router;
  }

  private void mountUserRoutes(Router router) {
    var handler = registry.getHandler("user", UserHandler.class);
    router.get("/users").handler(ctx -> handler.getAllUsers(ctx));
    router.get("/users/:id").handler(handler::getUser);
    router.post("/users").handler(handler::createUser);
    router.put("/users/:id").handler(handler::updateUser);
    router.delete("/users/:id").handler(handler::deleteUser);
  }

  private void mountCreateRegister(Router router) {

    RegistrationHandler registrationHandler =
      registry.getHandler("registrationHandler", RegistrationHandler.class);

    router.post("/api/auth/register")
      .handler(registrationHandler::register);
  }

}

