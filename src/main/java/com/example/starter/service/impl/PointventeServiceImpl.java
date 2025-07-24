package com.example.starter.service.impl;

import com.example.starter.model.Pointvente;
import com.example.starter.model.RelationPointventeCompagnie;
import com.example.starter.repository.PointventeRepository;
import com.example.starter.repository.RelationPointventeCompagnieRepository;
import com.example.starter.repository.StatutCRepository;
import com.example.starter.repository.StatutHistoriqueCRepository;
import com.example.starter.service.PointventeService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class PointventeServiceImpl implements PointventeService {

  private final PointventeRepository pointventeRepository;
  private final RelationPointventeCompagnieRepository relationRepository;
  private final StatutCRepository statutCRepository;
  private final StatutHistoriqueCRepository historiqueRepository;

  public PointventeServiceImpl(
    PointventeRepository pointventeRepository,
    RelationPointventeCompagnieRepository relationRepository,
    StatutCRepository statutCRepository,
    StatutHistoriqueCRepository historiqueRepository
  ) {
    this.pointventeRepository = pointventeRepository;
    this.relationRepository = relationRepository;
    this.statutCRepository = statutCRepository;
    this.historiqueRepository = historiqueRepository;
  }

  @Override
  public Future<Long> createPointvente(JsonObject data) {
    Pointvente pointvente = new Pointvente();
    pointvente.setNomp(data.getString("nomp"));
    pointvente.setEmailp(data.getString("emailp"));
    pointvente.setTelephonep(data.getString("telephonep"));

    // Validate required fields
    if (pointvente.getNomp() == null || pointvente.getNomp().trim().isEmpty()) {
      return Future.failedFuture("Nom is required");
    }

    return pointventeRepository.save(pointvente);
  }

  @Override
  public Future<JsonObject> getPointvente(Long id) {
    return pointventeRepository.findById(id)
      .compose(pointvente -> {
        if (pointvente == null) {
          return Future.failedFuture("Pointvente not found");
        }

        // Get relations count
        return relationRepository.findByPointventeId(id)
          .map(relations -> {
            JsonObject result = pointventeToJson(pointvente);
            result.put("company_count", relations.size());
            result.put("active_company_count",
              relations.stream().filter(r -> r.getActive()).count());
            return result;
          });
      });
  }

  @Override
  public Future<JsonArray> getAllPointventes() {
    return pointventeRepository.findAll()
      .map(pointventes -> {
        JsonArray array = new JsonArray();
        pointventes.forEach(pv -> array.add(pointventeToJson(pv)));
        return array;
      });
  }

  @Override
  public Future<Void> updatePointvente(Long id, JsonObject data) {
    return pointventeRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Pointvente not found");
        }

        Pointvente pointvente = new Pointvente();
        pointvente.setNomp(data.getString("nomp", existing.getNomp()));
        pointvente.setEmailp(data.getString("emailp", existing.getEmailp()));
        pointvente.setTelephonep(data.getString("telephonep", existing.getTelephonep()));

        return pointventeRepository.update(id, pointvente);
      });
  }

  @Override
  public Future<Void> deletePointvente(Long id) {
    // Check if there are any relations
    return relationRepository.findByPointventeId(id)
      .compose(relations -> {
        if (!relations.isEmpty()) {
          return Future.failedFuture("Cannot delete pointvente with linked companies");
        }
        return pointventeRepository.delete(id);
      });
  }

  @Override
  public Future<JsonArray> getCompagniesForPointvente(Long pointventeId) {
    return relationRepository.findByPointventeId(pointventeId)
      .compose(relations -> {
        JsonArray array = new JsonArray();

        // Get current status for each relation
        List<Future> futures = new ArrayList<>();

        for (RelationPointventeCompagnie relation : relations) {
          Future<JsonObject> future = historiqueRepository.getCurrentStatus(relation.getId())
            .map(currentStatus -> {
              JsonObject json = relationToJson(relation);
              if (currentStatus != null && currentStatus.getStatutC() != null) {
                json.put("current_status_label", currentStatus.getStatutC().getLibelle());
              }
              return json;
            });
          futures.add(future);
        }

        return CompositeFuture.all(futures)
          .map(composite -> {
            for (int i = 0; i < composite.size(); i++) {
              array.add(composite.resultAt(i));
            }
            return array;
          });
      });
  }

  private JsonObject pointventeToJson(Pointvente pointvente) {
    JsonObject json = new JsonObject()
      .put("id", pointvente.getId())
      .put("nomp", pointvente.getNomp())
      .put("emailp", pointvente.getEmailp())
      .put("telephonep", pointvente.getTelephonep());

    if (pointvente.getDateDebutp() != null) {
      json.put("date_debutp", pointvente.getDateDebutp().toString());
    }
    if (pointvente.getDateFinp() != null) {
      json.put("date_finp", pointvente.getDateFinp().toString());
    }

    return json;
  }

  private JsonObject relationToJson(RelationPointventeCompagnie relation) {
    JsonObject json = new JsonObject()
      .put("id", relation.getId())
      .put("pointvente_id", relation.getPointventeId())
      .put("compagnie_id", relation.getCompagnieId())
      .put("date_debut", relation.getDateDebut().toString())
      .put("active", relation.getActive())
      .put("status", relation.getStatus());

    if (relation.getDateFin() != null) {
      json.put("date_fin", relation.getDateFin().toString());
    }
    if (relation.getSuspensionReason() != null) {
      json.put("suspension_reason", relation.getSuspensionReason());
    }

    // Add company info if loaded
    if (relation.getCompagnie() != null) {
      json.put("compagnie_name", relation.getCompagnie().getRaison_social());
      json.put("compagnie_email", relation.getCompagnie().getEmail());
      json.put("compagnie_telephone", relation.getCompagnie().getTelephone());
    }

    return json;
  }
}

