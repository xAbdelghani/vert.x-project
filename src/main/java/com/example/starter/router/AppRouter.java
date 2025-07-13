package com.example.starter.router;

import com.example.starter.config.ServiceRegistry;
import com.example.starter.handler.*;
import com.example.starter.security.PolicyEnforcerHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;


public class AppRouter {

  private final Vertx vertx;
  private final ServiceRegistry registry;

  public AppRouter(Vertx vertx, ServiceRegistry registry) {
    this.vertx = vertx;
    this.registry = registry;
  }

  public Router createRouter() {

    Router router = Router.router(vertx);
    router.route().handler(
      CorsHandler.create("*") // Allow all origins; change "*" to a specific origin if needed
        .allowedMethod(HttpMethod.GET)
        .allowedMethod(HttpMethod.POST)
        .allowedMethod(HttpMethod.PUT)
        .allowedMethod(HttpMethod.DELETE)
        .allowedMethod(HttpMethod.OPTIONS)
        .allowedMethod(HttpMethod.PATCH)
        .allowedHeader("Authorization")
        .allowedHeader("Content-Type")
        .allowedHeader("Access-Control-Allow-Origin")
        .allowedHeader("X-Requested-With")
        .allowCredentials(true) // Optional if you want cookies or auth headers
    );
    // Global handlers
    router.route().handler(BodyHandler.create());
    // Public routes (no auth)
    router.get("/health").handler(ctx -> ctx.response().end("OK"));
    // Apply Policy Enforcer to ALL routes (just like Spring Boot)
    PolicyEnforcerHandler policyEnforcer = new PolicyEnforcerHandler(registry.getOAuth2Auth());
    router.route("/*").handler(policyEnforcer);
    // Mount all entity routes
    mountUserRoutes(router);
    //mountCreateRegister(router);
    mountCompagnieRegister(router);
    mountAgenceRoutes(router);

    mountFonctionRoutes(router);
    mountContactRoutes(router);

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

//  private void mountCreateRegister(Router router) {
//
//    //RegistrationHandler registrationHandler =
//    //registry.getHandler("registrationHandler", RegistrationHandler.class);
//
//       router.post("/api/auth/register")
//      .handler(ctx -> registrationHandler.register(ctx));
//  }

  private void mountCompagnieRegister(Router router) {

    CompagnieHandler CompagnieHandler = registry.getHandler("compagnieRegistrationHandler", CompagnieHandler.class);
    router.post("/api/compagnies")
      .handler(ctx -> CompagnieHandler.createCompagnie(ctx));

    router.post("/api/v1/compagnie/save")
      .handler(ctx -> CompagnieHandler.createCompagnie(ctx));

    router.get("/api/v1/compagnies")
      .handler(ctx -> CompagnieHandler.getAllCompagnies(ctx));


    router.post("/api/compagnies/:id/account")
      .handler(ctx -> CompagnieHandler.createAccount(ctx));

    router.delete("/api/compagnies/:id")
      .handler(ctx -> CompagnieHandler.deleteCompagnie(ctx));

    router.delete("/api/compagnies/:id/account")
      .handler(ctx->CompagnieHandler.deleteOnlyAccount(ctx));



  }

  private void mountAgenceRoutes(Router router) {

    AgenceHandler handler = registry.getHandler("agenceHandler", AgenceHandler.class);
    // CRUD routes
    router.get("/api/agences").handler(handler::getAll);
    router.get("/api/agences/:id").handler(handler::getById);
    router.post("/api/agences").handler(handler::create);
    router.put("/api/agences/:id").handler(handler::update);
    router.delete("/api/agences/:id").handler(handler::delete);

    // Special routes
    router.get("/api/agences/compagnie/:compagnieId").handler(handler::getByCompagnie);
    router.get("/api/agences/status").handler(handler::getByStatus);
    router.patch("/api/agences/:id/close").handler(handler::closeAgence);

  }


  private void mountFonctionRoutes(Router router) {
    FonctionHandler handler = registry.getHandler("fonctionHandler", FonctionHandler.class);

    router.get("/api/fonctions").handler(handler::getAllFonctions);
    router.get("/api/fonctions/:id").handler(handler::getFonction);
    router.post("/api/fonctions").handler(handler::createFonction);
    router.put("/api/fonctions/:id").handler(handler::updateFonction);
    router.delete("/api/fonctions/:id").handler(handler::deleteFonction);
    router.get("/api/fonctions/:id/contacts").handler(handler::getContactsByFonction);
  }

  private void mountContactRoutes(Router router) {
    ContactHandler handler = registry.getHandler("contactHandler", ContactHandler.class);

    router.get("/api/contacts").handler(handler::getAllContacts);
    router.get("/api/contacts/:id").handler(handler::getContact);
    router.post("/api/contacts").handler(handler::createContact);
    router.put("/api/contacts/:id").handler(handler::updateContact);
    router.delete("/api/contacts/:id").handler(handler::deleteContact);
    router.get("/api/compagnies/:compagnieId/contacts").handler(handler::getContactsByCompagnie);
  }


}

