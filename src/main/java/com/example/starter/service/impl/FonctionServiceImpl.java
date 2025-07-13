package com.example.starter.service.impl;

import com.example.starter.model.Contact;
import com.example.starter.model.Fonction;
import com.example.starter.repository.ContactRepository;
import com.example.starter.repository.FonctionRepository;
import com.example.starter.service.FonctionService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class FonctionServiceImpl implements FonctionService {

  private final FonctionRepository fonctionRepository;
  private final ContactRepository contactRepository;

  public FonctionServiceImpl(
    FonctionRepository fonctionRepository,
    ContactRepository contactRepository
  ) {
    this.fonctionRepository = fonctionRepository;
    this.contactRepository = contactRepository;
  }

  @Override
  public Future<Long> createFonction(JsonObject data) {
    String qualite = data.getString("qualite");

    if (qualite == null || qualite.trim().isEmpty()) {
      return Future.failedFuture("Qualite is required");
    }

    return fonctionRepository.save(qualite.trim());
  }

  @Override
  public Future<JsonObject> getFonction(Long id) {
    return fonctionRepository.findById(id)
      .compose(fonction -> {
        if (fonction == null) {
          return Future.failedFuture("Fonction not found");
        }

        // Get contacts for this fonction
        return contactRepository.findByFonctionId(id)
          .map(contacts -> {
            JsonObject result = fonctionToJson(fonction);
            JsonArray contactsArray = new JsonArray();
            contacts.forEach(contact -> contactsArray.add(contactToJson(contact)));
            result.put("contacts", contactsArray);
            return result;
          });
      });
  }

  @Override
  public Future<JsonArray> getAllFonctions() {
    return fonctionRepository.findAll()
      .map(fonctions -> {
        JsonArray array = new JsonArray();
        fonctions.forEach(fonction -> array.add(fonctionToJson(fonction)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getContactsByFonction(Long fonctionId) {
    return contactRepository.findByFonctionId(fonctionId)
      .map(contacts -> {
        JsonArray array = new JsonArray();
        contacts.forEach(contact -> array.add(contactToJson(contact)));
        return array;
      });
  }

  @Override
  public Future<Void> updateFonction(Long id, JsonObject data) {
    String qualite = data.getString("qualite");

    if (qualite == null || qualite.trim().isEmpty()) {
      return Future.failedFuture("Qualite is required");
    }

    return fonctionRepository.exists(id)
      .compose(exists -> {
        if (!exists) {
          return Future.failedFuture("Fonction not found");
        }
        return fonctionRepository.update(id, qualite.trim());
      });
  }

  @Override
  public Future<Void> deleteFonction(Long id) {
    // Check if there are contacts using this fonction
    return contactRepository.findByFonctionId(id)
      .compose(contacts -> {
        if (!contacts.isEmpty()) {
          return Future.failedFuture("Cannot delete fonction with associated contacts");
        }
        return fonctionRepository.delete(id);
      });
  }

  private JsonObject fonctionToJson(Fonction fonction) {
    return new JsonObject()
      .put("id", fonction.getId())
      .put("qualite", fonction.getQualite());
  }

  private JsonObject contactToJson(Contact contact) {
    JsonObject json = new JsonObject()
      .put("id", contact.getId())
      .put("nomc", contact.getNomc())
      .put("prenomc", contact.getPrenomc())
      .put("emailc", contact.getEmailc())
      .put("telephonec", contact.getTelephonec());

    if (contact.getCompagnie() != null) {
      json.put("compagnie_name", contact.getCompagnie().getRaison_social());
    }

    return json;
  }
}
