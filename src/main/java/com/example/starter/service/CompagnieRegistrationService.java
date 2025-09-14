package com.example.starter.service;

import com.example.starter.model.Compagnie;
import com.example.starter.repository.impl.CompagnieRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class CompagnieRegistrationService {

  private final CompagnieRepositoryImpl compagnieRepository;
  private final KeycloakAdminService keycloakAdminService;
  private final EmailService emailService;

  public CompagnieRegistrationService(
    CompagnieRepositoryImpl compagnieRepository,
    KeycloakAdminService keycloakAdminService,
    EmailService emailService)
  {
    this.compagnieRepository = compagnieRepository;
    this.keycloakAdminService = keycloakAdminService;
    this.emailService = emailService;
  }
  // Step 1: Save basic compagnie data
  public Future<Long> createCompagnie(JsonObject data) {
    return compagnieRepository.save(
      data.getString("raison_social"),
      data.getString("email"),
      data.getString("telephone"),
      data.getString("adresse")
    );
  }
  // Update compagnie information
  public Future<JsonObject> updateCompagnie(Long compagnieId, JsonObject updateData) {
    Promise<JsonObject> promise = Promise.promise();

    compagnieRepository.findById(compagnieId).compose(compagnie -> {
      if (compagnie == null) {
        return Future.failedFuture("Compagnie not found");
      }

      String nom = updateData.getString("nom");
      String raisonSocial = updateData.getString("raison_social");
      String email = updateData.getString("email");
      String telephone = updateData.getString("telephone");
      String adresse = updateData.getString("adresse");

      return compagnieRepository.update(compagnieId, nom, raisonSocial, email, telephone, adresse)
        .compose(v -> {
          return Future.succeededFuture(
            new JsonObject()
              .put("compagnieId", compagnieId)
              .put("message", "Compagnie updated successfully")
          );
        });
    }).onComplete(promise);

    return promise.future();
  }


  // Step 2: Create Keycloak account & update compagnie record
  public Future<JsonObject> createCompagnieAccount(Long compagnieId, String nom) {
    Promise<JsonObject> promise = Promise.promise();

    compagnieRepository.findById(compagnieId).compose(compagnie -> {
      if (compagnie == null) {
        return Future.failedFuture("Compagnie not found");
      }
      String email = compagnie.getEmail();
      String generatedPassword = "Kx9#mL2pQw8@vN5r";

      JsonObject userData = new JsonObject()
        .put("username", nom)
        .put("email", email)
        .put("firstName", compagnie.getRaison_social())
        .put("lastName", "Company")  // Simple fix
        .put("password", generatedPassword)
        .put("role", "client-abonnement")
        .put("compagnieId", compagnieId)
        .put("requiredActions", new JsonArray());



      return keycloakAdminService.createUser(userData)
        .compose(keycloakId ->
          compagnieRepository.createAccountForCompagnie(compagnieId, nom, generatedPassword)
            .compose(v -> {
              // Send welcome email asynchronously (fire-and-forget)
              emailService.sendWelcomeEmail(email, nom, generatedPassword)
                .onFailure(err -> System.err.println("Failed to send email: " + err.getMessage()));

              return Future.succeededFuture(
                new JsonObject()
                  .put("compagnieId", compagnieId)
                  .put("keycloakUserId", keycloakId)
                  .put("message", "Compagnie account created successfully"));
            })
        );
    }).onComplete(promise);

    return promise.future();
  }


  public Future<List<Compagnie>> findAll() {
    return compagnieRepository.findAll();
  }

  // Delete a compagnie by ID
  public Future<Void> deleteCompagnie(Long id) {
    Promise<Void> promise = Promise.promise();

    compagnieRepository.findById(id).compose(compagnie -> {
      if (compagnie == null) {
        return Future.failedFuture("Compagnie not found");
      }

      String username = compagnie.getNom();
      if (username == null || username.isEmpty()) {
        // No Keycloak account created yet
        return compagnieRepository.deleteById(id);
      }

      return keycloakAdminService.findUserIdByUsername(username)
        .compose(userId -> keycloakAdminService.deleteUser(userId))
        .compose(v -> compagnieRepository.deleteById(id));
    }).onComplete(promise);

    return promise.future();
  }

  // Delete only the Keycloak account of a compagnie
  public Future<Void> deleteCompagnieAccountOnly(Long compagnieId) {
    Promise<Void> promise = Promise.promise();

    compagnieRepository.findById(compagnieId).compose(compagnie -> {
      if (compagnie == null) {
        return Future.failedFuture("Compagnie not found");
      }

      String username = compagnie.getNom();
      if (username == null || username.isEmpty()) {
        return Future.failedFuture("No Keycloak account associated with this compagnie");
      }

      return keycloakAdminService.findUserIdByUsername(username)
        .compose(userId -> keycloakAdminService.deleteUser(userId))
        .compose(v -> {
          // Optionally, clear the `nom` and `password` fields in DB
          return compagnieRepository.createAccountForCompagnie(compagnieId, null, null);
        });
    }).onComplete(promise);

    return promise.future();
  }





}
