package com.example.starter.config;

import com.example.starter.handler.RegistrationHandler;
import com.example.starter.handler.UserHandler;
import com.example.starter.repository.UserRepository;
import com.example.starter.repository.impl.UserRepositoryImpl;
import com.example.starter.service.EmailService;
import com.example.starter.service.KeycloakAdminService;
import com.example.starter.service.RegistrationService;
import com.example.starter.service.UserService;
import com.example.starter.service.impl.UserServiceImpl;
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
  }

  private void initBusinessServices() {
    // Business logic layer
    UserService userService = new UserServiceImpl(
      getRepository("userRepository", UserRepository.class)
    );
    services.put("userService", userService);

    RegistrationService registrationService = new RegistrationService(
      getService("keycloakAdmin", KeycloakAdminService.class),
      getService("emailService", EmailService.class)
    );
    services.put("registrationService", registrationService);
  }

  private void initHandlers() {
    // Presentation layer
    handlers.put("user", new UserHandler(
      getService("userService", UserService.class)
    ));

    handlers.put("registrationHandler", new RegistrationHandler(
      getService("registrationService", RegistrationService.class)
    ));
  }

  // Type-safe getters to avoid casting everywhere
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
