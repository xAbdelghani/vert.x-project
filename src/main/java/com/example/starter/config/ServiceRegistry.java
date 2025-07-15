package com.example.starter.config;

import com.example.starter.handler.*;
import com.example.starter.repository.*;
import com.example.starter.repository.impl.*;
import com.example.starter.service.*;
import com.example.starter.service.impl.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.mail.MailClient;
import io.vertx.pgclient.PgPool;
import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {

  private final Map<String, Object> handlers = new HashMap<>();
  private final Map<String, Object> services = new HashMap<>();
  private final Map<String, Object> repositories = new HashMap<>();

  private final PgPool pgPool;
  private final OAuth2Auth oauth2Auth;
  private final JsonObject config;
  private final Vertx vertx;

  public ServiceRegistry(PgPool pgPool, OAuth2Auth oauth2Auth, JsonObject config, Vertx vertx) {
    this.vertx = vertx;
    this.config = config;
    this.pgPool = pgPool;
    this.oauth2Auth = oauth2Auth;
    initializeServices();
  }

  private void initializeServices() {
    initCoreServices();
    initRepositories();
    initBusinessServices();
    initHandlers();
  }

  private void initCoreServices() {
    MailClient mailClient = MailClientConfig.create(vertx, config.getJsonObject("mail"));

    EmailService emailService = new EmailService(mailClient, config.getJsonObject("mail"));
    services.put("emailService", emailService);

    KeycloakAdminService keycloakAdmin = new KeycloakAdminService(config.getJsonObject("keycloak"));
    services.put("keycloakAdmin", keycloakAdmin);
  }



  private void initRepositories() {
    repositories.put("userRepository", new UserRepositoryImpl(pgPool));
    repositories.put("compagnieRepository", new CompagnieRepositoryImpl(pgPool));
    repositories.put("agenceRepository", new AgenceRepositoryImpl(pgPool));
    repositories.put("fonctionRepository", new FonctionRepositoryImpl(pgPool));
    repositories.put("contactRepository", new ContactRepositoryImpl(pgPool));
    repositories.put("pointventeRepository", new PointventeRepositoryImpl(pgPool));
    repositories.put("relationPointventeCompagnieRepository", new RelationPointventeCompagnieRepositoryImpl(pgPool));
    repositories.put("statutCRepository", new StatutCRepositoryImpl(pgPool));
    repositories.put("statutHistoriqueCRepository", new StatutHistoriqueCRepositoryImpl(pgPool));
  }


  private void initBusinessServices() {

    // User service
    UserService userService = new UserServiceImpl(
      getRepository("userRepository", UserRepository.class)
    );
    services.put("userService", userService);

    FonctionService fonctionService = new FonctionServiceImpl(
      getRepository("fonctionRepository", FonctionRepository.class),
      getRepository("contactRepository", ContactRepository.class)
    );
    services.put("fonctionService", fonctionService);

    ContactService contactService = new ContactServiceImpl(
      getRepository("contactRepository", ContactRepository.class),
      getRepository("compagnieRepository", CompagnieRepositoryImpl.class),
      getRepository("fonctionRepository", FonctionRepository.class)
    );
    services.put("contactService", contactService);



    // Registration service for regular users
    RegistrationService registrationService = new RegistrationService(
      getService("keycloakAdmin", KeycloakAdminService.class),
      getService("emailService", EmailService.class)
    );
    services.put("registrationService", registrationService);



    // Compagnie registration service
    CompagnieRegistrationService compagnieRegistrationService = new CompagnieRegistrationService(
      getRepository("compagnieRepository", CompagnieRepositoryImpl.class),
      getService("keycloakAdmin", KeycloakAdminService.class),
      getService("emailService", EmailService.class)
    );
    services.put("compagnieRegistrationService", compagnieRegistrationService);


    AgenceService agenceService = new AgenceServiceImpl(
      getRepository("agenceRepository", AgenceRepository.class),
      getRepository("compagnieRepository", CompagnieRepositoryImpl.class)
    );
    services.put("agenceService", agenceService);


    PointventeService pointventeService = new PointventeServiceImpl(
      getRepository("pointventeRepository", PointventeRepository.class),
      getRepository("relationPointventeCompagnieRepository", RelationPointventeCompagnieRepository.class),
      getRepository("statutCRepository", StatutCRepository.class),
      getRepository("statutHistoriqueCRepository", StatutHistoriqueCRepository.class)
    );
    services.put("pointventeService", pointventeService);


    RelationPointventeCompagnieService relationService = new RelationPointventeCompagnieServiceImpl(
      getRepository("relationPointventeCompagnieRepository", RelationPointventeCompagnieRepository.class),
      getRepository("pointventeRepository", PointventeRepository.class),
      getRepository("compagnieRepository", CompagnieRepositoryImpl.class),
      getRepository("statutCRepository", StatutCRepository.class),
      getRepository("statutHistoriqueCRepository", StatutHistoriqueCRepository.class),
      pgPool
    );
    services.put("relationPointventeCompagnieService", relationService);


  }


  private void initHandlers() {

    handlers.put("fonctionHandler", new FonctionHandler(
      getService("fonctionService", FonctionService.class)
    ));

    handlers.put("contactHandler", new ContactHandler(
      getService("contactService", ContactService.class)
    ));

    handlers.put("agenceHandler", new AgenceHandler(
      getService("agenceService", AgenceService.class)
    ));
    // Handler for regular users
    handlers.put("user", new UserHandler(
      getService("userService", UserService.class)
    ));
    // Handler for user registration
    // handlers.put("registrationHandler", new RegistrationHandler(
    // getService("registrationService", RegistrationService.class)
    // ));
    // Handler for compagnie registration
    handlers.put("compagnieRegistrationHandler", new CompagnieHandler(
      getService("compagnieRegistrationService", CompagnieRegistrationService.class)
    ));

    handlers.put("pointventeHandler", new PointventeHandler(
      getService("pointventeService", PointventeService.class),
      getService("relationPointventeCompagnieService", RelationPointventeCompagnieService.class)
    ));
  }

  // Type-safe getters
  @SuppressWarnings("unchecked")
  public <T> T getHandler(String name, Class<T> type) {
    return (T) handlers.get(name);
  }

  @SuppressWarnings("unchecked")
  private <T> T getService(String name, Class<T> type) {
    return (T) services.get(name);
  }

  @SuppressWarnings("unchecked")
  private <T> T getRepository(String name, Class<T> type) {
    return (T) repositories.get(name);
  }

  public OAuth2Auth getOAuth2Auth() {
    return oauth2Auth;
  }


}
