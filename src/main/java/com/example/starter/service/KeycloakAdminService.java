package com.example.starter.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.*;

public class KeycloakAdminService {

  private final Keycloak keycloak;

  private final String realm;

  public KeycloakAdminService(JsonObject config) {
    this.realm = config.getString("realm");
    // Initialize Keycloak Admin Client
    this.keycloak = KeycloakBuilder.builder()
      .serverUrl(config.getString("authServerUrl"))
      .realm(config.getString("realm"))
      .clientId(config.getString("adminClientId", "admin-cli"))
      .clientSecret(config.getString("adminClientSecret"))
      .grantType("client_credentials")
      .build();
  }

  public Future<String> createUser(JsonObject userData) {
    Promise<String> promise = Promise.promise();
    try {
      RealmResource realmResource = keycloak.realm(realm);
      UsersResource usersResource = realmResource.users();

      UserRepresentation user = new UserRepresentation();
      user.setUsername(userData.getString("username"));
      user.setEmail(userData.getString("email"));
      user.setFirstName(userData.getString("firstName"));
      user.setLastName("company");
      user.setEnabled(true);
      user.setEmailVerified(userData.getBoolean("emailVerified", false));
      Map<String, List<String>> attributes = new HashMap<>();
      attributes.put("compagnieId", Collections.singletonList(String.valueOf(userData.getLong("compagnieId"))));
      user.setAttributes(attributes);



      // Set required actions if present
      if (userData.containsKey("requiredActions")) {
        List<String> requiredActions = userData.getJsonArray("requiredActions").getList();
        user.setRequiredActions(requiredActions);
      }

      // Set password
      CredentialRepresentation credential = new CredentialRepresentation();
      credential.setType(CredentialRepresentation.PASSWORD);
      credential.setValue(userData.getString("password"));
      credential.setTemporary(false);
      user.setCredentials(Arrays.asList(credential));

      // Create user
      Response response = usersResource.create(user);

      if (response.getStatus() == 201) {
        String locationHeader = response.getHeaderString("Location");
        String userId = locationHeader.substring(locationHeader.lastIndexOf("/") + 1);
        assignDefaultRoles(userId, userData.getString("role", "client-abonnement"));
        promise.complete(userId);
      } else if (response.getStatus() == 409) {
        promise.fail("User already exists");
      } else {
        String error = response.readEntity(String.class); // ✅ get full error
        promise.fail("Failed to create user: " + response.getStatus() + " - " + error);
      }

      response.close();

    } catch (Exception e) {
      promise.fail(e);
    }

    return promise.future();
  }


  private void assignDefaultRoles(String userId, String roleName) {

    try {
      RealmResource realmResource = keycloak.realm(realm);

      // Get the role
      RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();

      // Assign role to user
      realmResource.users().get(userId).roles().realmLevel()
        .add(Arrays.asList(role));

    } catch (Exception e) {
      System.err.println("Failed to assign role: " + e.getMessage());
    }

  }

  public Future<Void> sendVerificationEmail(String userId) {
    Promise<Void> promise = Promise.promise();
    try {
      keycloak.realm(realm).users().get(userId).sendVerifyEmail();
      promise.complete();
    } catch (Exception e) {
      promise.fail(e);
    }

    return promise.future();
  }



  public Future<Boolean> deleteUser(String userId) {
    Promise<Boolean> promise = Promise.promise();
    try {
      keycloak.realm(realm).users().delete(userId);
      promise.complete(true);
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }


  public void close() {
    if (keycloak != null) {
      keycloak.close();
    }
  }

  public Future<String> findUserIdByUsername(String username) {
    Promise<String> promise = Promise.promise();
    try {
      List<UserRepresentation> users = keycloak.realm(realm).users().search(username);
      if (users != null && !users.isEmpty()) {
        promise.complete(users.get(0).getId());
      } else {
        promise.fail("User not found in Keycloak for username: " + username);
      }
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }


}
