package com.example.starter.handler;

import com.example.starter.service.ContactService;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class ContactHandler {

  private final ContactService contactService;

  public ContactHandler(ContactService contactService) {
    this.contactService = contactService;
  }

  public void createContact(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();

    contactService.createContact(body)
      .onSuccess(id -> ctx.response()
        .setStatusCode(201)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("id", id).encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getContact(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    contactService.getContact(id)
      .onSuccess(contact -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(contact.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getAllContacts(RoutingContext ctx) {
    contactService.getAllContacts()
      .onSuccess(contacts -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(contacts.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void getContactsByCompagnie(RoutingContext ctx) {
    Long compagnieId = Long.parseLong(ctx.pathParam("compagnieId"));
    contactService.getContactsByCompagnie(compagnieId)
      .onSuccess(contacts -> ctx.response()
        .putHeader("content-type", "application/json")
        .end(contacts.encode()))
      .onFailure(err -> ctx.response()
        .setStatusCode(500)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }

  public void updateContact(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));
    JsonObject body = ctx.body().asJsonObject();

    contactService.updateContact(id, body)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> {
        int statusCode = err.getMessage().contains("not found") ? 404 : 400;
        ctx.response()
          .setStatusCode(statusCode)
          .putHeader("content-type", "application/json")
          .end(new JsonObject().put("error", err.getMessage()).encode());
      });
  }

  public void deleteContact(RoutingContext ctx) {
    Long id = Long.parseLong(ctx.pathParam("id"));

    contactService.deleteContact(id)
      .onSuccess(v -> ctx.response()
        .setStatusCode(204)
        .end())
      .onFailure(err -> ctx.response()
        .setStatusCode(404)
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("error", err.getMessage()).encode()));
  }



}
