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
        .allowedMethod(HttpMethod.HEAD)
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
    mountCompagnieRegister(router);
    mountAgenceRoutes(router);
    mountFonctionRoutes(router);
    mountContactRoutes(router);
    mountPointventeRoutes(router);
    mountBalanceRoutes(router);
    mountTypeAbonnementRoutes(router);
    mountSubscriptionRoutes(router);
    mountPrepayeRoutes(router);
    mountTypeAttestationRoutes(router);
    mountVehicleRoutes(router);
    mountAttestationRoutes(router);
    mountNotificationRoutes(router);
    mountContactRoutesM(router);
    mountDashboardRoutes(router);
    return router;

  }



  private void mountNotificationRoutes(Router router) {
    NotificationHandler handler = registry.getHandler("notificationHandler", NotificationHandler.class);
    // Notification routes
    router.get("/api/notifications").handler(handler::getAllNotifications);
    router.get("/api/notifications/unread-count").handler(handler::getUnreadCount);
    router.get("/api/notifications/:id").handler(handler::getNotification);
    router.get("/api/notifications/company/:companyId").handler(handler::getCompanyNotifications);
    router.post("/api/notifications").handler(handler::createNotification);
    router.post("/api/notifications/authorization-request").handler(handler::createAuthorizationRequest);
    router.put("/api/notifications/:id/read").handler(handler::markAsRead);
    router.put("/api/notifications/read-multiple").handler(handler::markMultipleAsRead);
    router.delete("/api/notifications/:id").handler(handler::deleteNotification);
  }

  private void mountContactRoutesM(Router router) {
    ContactMessageHandler contactMessageHandler = registry.
      getHandler("ContactMessageHandler", ContactMessageHandler.class);
    router.post("/api/contact/send-email")
      .handler(BodyHandler.create())
      .handler(contactMessageHandler::sendContactEmail);

  }


  private void mountTypeAttestationRoutes(Router router) {

    TypeAttestationHandler handler = registry.getHandler("typeAttestationHandler", TypeAttestationHandler.class);

    // Type Attestation CRUD
    router.post("/api/type-attestations").handler(ctx -> handler.createType(ctx));
    router.get("/api/type-attestations").handler(handler::getAllTypes);
    router.get("/api/type-attestations/:id").handler(handler::getType);
    router.put("/api/type-attestations/:id").handler(handler::updateType);
    router.delete("/api/type-attestations/:id").handler(handler::deleteType);

    // Authorization Management
    router.post("/api/companies/:companyId/authorize-attestation/:typeId")
      .handler(handler::authorizeCompany);

    router.delete("/api/companies/:companyId/authorize-attestation/:typeId")
      .handler(handler::unauthorizeCompany);

    router.get("/api/companies/:id/authorized-attestations")
      .handler(handler::getCompanyAuthorizations);
    // Authorization Matrix
    router.get("/api/authorization-matrix").handler(handler::getAuthorizationMatrix);
    router.put("/api/authorization-matrix").handler(handler::updateAuthorizationMatrix);

    // Check Authorization
    router.get("/api/companies/:id/can-generate/:typeId")
      .handler(handler::checkAuthorization);
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

    router.put("/api/compagnies/:id").handler(CompagnieHandler::updateCompagnie);

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

  private void mountPointventeRoutes(Router router) {
    PointventeHandler handler = registry.getHandler("pointventeHandler", PointventeHandler.class);
    // Pointvente CRUD
    router.get("/api/pointventes").handler(handler::getAllPointventes);
    router.get("/api/pointventes/:id").handler(handler::getPointvente);
    router.post("/api/pointventes").handler(handler::createPointvente);
    router.put("/api/pointventes/:id").handler(handler::updatePointvente);
    router.delete("/api/pointventes/:id").handler(handler::deletePointvente);

    // Company relations
    router.get("/api/pointventes/:id/compagnies").handler(handler::getCompagniesForPointvente);
    router.post("/api/pointventes/:id/compagnies").handler(handler::linkCompagnieToPointvente);

    // Relation management
    router.put("/api/relations/:relationId/status").handler(handler::updateRelationStatus);
    router.get("/api/relations/:relationId/historique").handler(handler::getHistoriqueForRelation);
    // router.get("/api/relations/:relationId").handler(handler::getRelationDetails);
    router.delete("/api/relations/:relationId").handler(handler::unlinkCompagnie);
  }

  private void mountBalanceRoutes(Router router) {
    BalanceHandler handler = registry.getHandler("balanceHandler", BalanceHandler.class);
    // Balance management
    router.get("/api/balances").handler(handler::getAllBalances);
    router.get("/api/companies/:id/balance").handler(handler::getCompanyBalance);
    router.post("/api/companies/:id/balance/credit").handler(handler::addCredit);
    router.post("/api/companies/:id/balance/debit").handler(handler::deductCredit);
    router.get("/api/companies/:id/transactions").handler(handler::getTransactionHistory);
    router.post("/api/companies/:id/balance/initialize").handler(handler::initializeBalance);
    router.get("/api/balances/low").handler(handler::getLowBalances);
  }

  private void mountPrepayeRoutes(Router router) {
    PrepayeHandler prepayeHandler = registry.getHandler("prepayeHandler", PrepayeHandler.class);

    router.get("/api/prepaye/balances").handler(prepayeHandler::getAllPrepayeBalances);
    router.get("/api/prepaye/companies/:id/balance").handler(prepayeHandler::getPrepayeBalance);
    router.post("/api/prepaye/companies/:id/initialize").handler(prepayeHandler::initializePrepaye);
    router.post("/api/prepaye/companies/:id/credit").handler(prepayeHandler::addCredit);
    router.post("/api/prepaye/companies/:id/debit").handler(prepayeHandler::deductCredit);
    router.get("/api/prepaye/companies/:id/transactions").handler(prepayeHandler::getTransactionHistory);
    router.get("/api/prepaye/balances/low").handler(prepayeHandler::getLowBalances);
    router.post("/api/prepaye/companies/:id/attestation-payment").handler(prepayeHandler::processAttestationPayment);

  }

  private void mountTypeAbonnementRoutes(Router router) {
    TypeAbonnementHandler handler = registry.getHandler("typeAbonnementHandler", TypeAbonnementHandler.class);
    //
    router.get("/api/abonnement-types").handler(handler::getAllTypes);
    router.get("/api/abonnement-types/active").handler(handler::getActiveTypes);
    router.get("/api/abonnement-types/:id").handler(handler::getType);
    router.get("/api/abonnement-types/categorie/:categorie").handler(handler::getTypesByCategorie);
    router.post("/api/abonnement-types").handler(handler::createType);
    router.put("/api/abonnement-types/:id").handler(handler::updateType);
    router.delete("/api/abonnement-types/:id").handler(handler::deleteType);
  }

  private void mountSubscriptionRoutes(Router router) {

    SubscriptionHandler handler = registry.getHandler("subscriptionHandler", SubscriptionHandler.class);
    // Existing routes
    router.post("/api/subscriptions").handler(handler::createSubscription);
    router.get("/api/companies/:id/subscription").handler(handler::getCompanySubscription);
    router.get("/api/subscriptions").handler(handler::getAllSubscriptions);
    router.get("/api/subscriptions/category/:category").handler(handler::getSubscriptionsByCategory);
    router.put("/api/subscriptions/:id").handler(handler::updateSubscription);
    // B) Status Management Routes
    router.get("/api/subscription-statuses").handler(handler::getAvailableStatuses);
    router.put("/api/subscriptions/:id/status").handler(handler::changeStatus);
    router.put("/api/subscriptions/:id/suspend").handler(handler::suspendSubscription);
    router.put("/api/subscriptions/:id/reactivate").handler(handler::reactivateSubscription);
    // C) Credit Management Routes (AVANCE)
    router.get("/api/companies/:id/credit-usage").handler(handler::getCreditUsage);
    router.post("/api/companies/:id/use-credit").handler(handler::useCredit);
    // D) Deposit Management Routes (CAUTION)
    router.get("/api/companies/:id/deposit-status").handler(handler::getDepositStatus);
    router.post("/api/companies/:id/use-deposit").handler(handler::useDeposit);
  }

  private void mountVehicleRoutes(Router router) {

    VehicleHandler handler = registry.getHandler("vehicleHandler", VehicleHandler.class);
    // Vehicle Model Routes
    // IMPORTANT: Static routes MUST come before dynamic routes with path parameters
    // Static routes first
    router.get("/api/vehicle-models/brands").handler(handler::getAllBrands);
    router.get("/api/vehicle-models/types").handler(handler::getAllTypes);
    router.get("/api/vehicle-models/search").handler(handler::searchModels);

    // Then routes without parameters
    router.post("/api/vehicle-models").handler(handler::createModel);
    router.get("/api/vehicle-models").handler(handler::getAllModels);

    // Finally, dynamic routes with path parameters
    router.get("/api/vehicle-models/:id").handler(handler::getModel);
    router.put("/api/vehicle-models/:id").handler(handler::updateModel);
    router.delete("/api/vehicle-models/:id").handler(handler::deleteModel);

    // Vehicle Routes
    // Same principle: static routes before dynamic ones

    // Static routes first
    router.get("/api/vehicles/search").handler(handler::searchVehicles);
    router.get("/api/vehicles/plate/:immatriculation").handler(handler::getVehicleByPlate);

    // Then routes without parameters
    router.post("/api/vehicles").handler(handler::registerVehicle);
    router.get("/api/vehicles").handler(handler::getAllVehicles);

    // Finally, dynamic routes with path parameters
    router.get("/api/vehicles/:id").handler(handler::getVehicle);
    router.put("/api/vehicles/:id").handler(handler::updateVehicle);
    router.delete("/api/vehicles/:id").handler(handler::deleteVehicle);
  }

  // Add this method to your main application class where routes are configured:

  private void mountAttestationRoutes(Router router) {
    AttestationHandler handler = registry.getHandler("attestationHandler", AttestationHandler.class);

    // Main attestation generation endpoint
    router.post("/api/attestations/generate")
      .handler(handler::generateAttestations);

    // Get single attestation
    router.get("/api/attestations/:id")
      .handler(handler::getAttestation);

    // Get attestation by reference
    router.get("/api/attestations/reference/:reference")
      .handler(handler::getAttestationByReference);

    // Get company attestations
    router.get("/api/companies/:id/attestations")
      .handler(handler::getCompanyAttestations);

    // Get vehicle attestations
    router.get("/api/vehicles/:id/attestations")
      .handler(handler::getVehicleAttestations);

    // Cancel attestation
    router.put("/api/attestations/:id/cancel")
      .handler(handler::cancelAttestation);

    // Get attestation PDF
    router.get("/api/attestations/:id/pdf")
      .handler(handler::getAttestationPDF);

    // Verify attestation (public endpoint)
    router.get("/api/attestations/verify/:reference")
      .handler(handler::verifyAttestation);

    // Expire attestations (for scheduled job or manual trigger)
    router.post("/api/attestations/expire")
      .handler(handler::expireAttestations);
  }

  private void mountDashboardRoutes(Router router) {
    DashboardHandler handler = registry.getHandler("dashboardHandler", DashboardHandler.class);

    // Main dashboard endpoint - returns all stats
    router.get("/api/dashboard/stats")
      .handler(handler::getDashboardStats);

    // Individual stat endpoints
    router.get("/api/dashboard/companies")
      .handler(handler::getCompanyStats);

    router.get("/api/dashboard/attestations")
      .handler(handler::getAttestationStats);

    router.get("/api/dashboard/financial")
      .handler(handler::getFinancialStats);

    router.get("/api/dashboard/today")
      .handler(handler::getTodayActivity);

    // List endpoints
    router.get("/api/dashboard/recent-attestations")
      .handler(handler::getRecentAttestations);

    router.get("/api/dashboard/low-balance")
      .handler(handler::getLowBalanceCompanies);

    router.get("/api/dashboard/expiring")
      .handler(handler::getExpiringAttestations);
  }


}

