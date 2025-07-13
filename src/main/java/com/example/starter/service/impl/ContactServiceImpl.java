package com.example.starter.service.impl;

import com.example.starter.model.Contact;
import com.example.starter.repository.ContactRepository;
import com.example.starter.repository.FonctionRepository;
import com.example.starter.repository.impl.CompagnieRepositoryImpl;
import com.example.starter.service.ContactService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.NoSuchElementException;

public class ContactServiceImpl implements ContactService {
  private final ContactRepository contactRepository;
  private final CompagnieRepositoryImpl compagnieRepository;
  private final FonctionRepository fonctionRepository;

  public ContactServiceImpl(
    ContactRepository contactRepository,
    CompagnieRepositoryImpl compagnieRepository,
    FonctionRepository fonctionRepository
  ) {
    this.contactRepository = contactRepository;
    this.compagnieRepository = compagnieRepository;
    this.fonctionRepository = fonctionRepository;
  }

  @Override
  public Future<Long> createContact(JsonObject data) {
    Contact contact = new Contact();
    contact.setNomc(data.getString("nomc"));
    contact.setPrenomc(data.getString("prenomc"));
    contact.setFax(data.getString("fax"));
    contact.setTelephonec(data.getString("telephonec"));
    contact.setEmailc(data.getString("emailc"));
    contact.setRemarquec(data.getString("remarquec"));
    contact.setCompagnieId(data.getLong("compagnie_id"));
    contact.setFonctionId(data.getLong("fonction_id"));

    // Validate required fields
    if (contact.getNomc() == null || contact.getNomc().trim().isEmpty()) {
      return Future.failedFuture("Contact name is required");
    }

    // Validate compagnie exists if provided
    Future<Boolean> compagnieCheck = contact.getCompagnieId() != null
      ? compagnieRepository.findById(contact.getCompagnieId())
      .map(compagnie -> compagnie != null)
      : Future.succeededFuture(true);

    // Validate fonction exists if provided
    Future<Boolean> fonctionCheck = contact.getFonctionId() != null
      ? fonctionRepository.exists(contact.getFonctionId())
      : Future.succeededFuture(true);

    return CompositeFuture.all(compagnieCheck, fonctionCheck)
      .compose(composite -> {
        if (contact.getCompagnieId() != null && !compagnieCheck.result()) {
          return Future.failedFuture("Compagnie not found");
        }
        if (contact.getFonctionId() != null && !fonctionCheck.result()) {
          return Future.failedFuture("Fonction not found");
        }
        return contactRepository.save(contact);
      });
  }

  @Override
  public Future<JsonObject> getContact(Long id) {
    return contactRepository.findById(id)
      .map(contact -> {
        if (contact == null) {
          throw new NoSuchElementException("Contact not found");
        }
        return contactToJson(contact);
      });
  }

  @Override
  public Future<JsonArray> getAllContacts() {
    return contactRepository.findAll()
      .map(contacts -> {
        JsonArray array = new JsonArray();
        contacts.forEach(contact -> array.add(contactToJson(contact)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getContactsByCompagnie(Long compagnieId) {
    return contactRepository.findByCompagnieId(compagnieId)
      .map(contacts -> {
        JsonArray array = new JsonArray();
        contacts.forEach(contact -> array.add(contactToJson(contact)));
        return array;
      });
  }

  @Override
  public Future<Void> updateContact(Long id, JsonObject data) {
    return contactRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Contact not found");
        }

        Contact contact = new Contact();
        contact.setNomc(data.getString("nomc", existing.getNomc()));
        contact.setPrenomc(data.getString("prenomc", existing.getPrenomc()));
        contact.setFax(data.getString("fax", existing.getFax()));
        contact.setTelephonec(data.getString("telephonec", existing.getTelephonec()));
        contact.setEmailc(data.getString("emailc", existing.getEmailc()));
        contact.setRemarquec(data.getString("remarquec", existing.getRemarquec()));
        contact.setCompagnieId(data.getLong("compagnie_id", existing.getCompagnieId()));
        contact.setFonctionId(data.getLong("fonction_id", existing.getFonctionId()));

        return contactRepository.update(id, contact);
      });
  }

  @Override
  public Future<Void> deleteContact(Long id) {
    return contactRepository.delete(id);
  }

  private JsonObject contactToJson(Contact contact) {
    JsonObject json = new JsonObject()
      .put("id", contact.getId())
      .put("nomc", contact.getNomc())
      .put("prenomc", contact.getPrenomc())
      .put("fax", contact.getFax())
      .put("telephonec", contact.getTelephonec())
      .put("emailc", contact.getEmailc())
      .put("remarquec", contact.getRemarquec())
      .put("compagnie_id", contact.getCompagnieId())
      .put("fonction_id", contact.getFonctionId());

    // Add related entity info if loaded
    if (contact.getFonction() != null) {
      json.put("fonction_qualite", contact.getFonction().getQualite());
    }

    if (contact.getCompagnie() != null) {
      json.put("compagnie_name", contact.getCompagnie().getRaison_social());
    }

    return json;
  }
}
