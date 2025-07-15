package com.example.starter.service.impl;

import com.example.starter.model.RelationPointventeCompagnie;
import com.example.starter.model.StatutC;
import com.example.starter.model.StatutHistoriqueC;
import com.example.starter.repository.PointventeRepository;
import com.example.starter.repository.RelationPointventeCompagnieRepository;
import com.example.starter.repository.StatutCRepository;
import com.example.starter.repository.StatutHistoriqueCRepository;
import com.example.starter.repository.impl.CompagnieRepositoryImpl;
import com.example.starter.service.RelationPointventeCompagnieService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class RelationPointventeCompagnieServiceImpl implements RelationPointventeCompagnieService {

  private final RelationPointventeCompagnieRepository relationRepository;
  private final PointventeRepository pointventeRepository;
  private final CompagnieRepositoryImpl compagnieRepository;
  private final StatutCRepository statutCRepository;
  private final StatutHistoriqueCRepository historiqueRepository;
  private final PgPool pgPool;

  public RelationPointventeCompagnieServiceImpl(
    RelationPointventeCompagnieRepository relationRepository,
    PointventeRepository pointventeRepository,
    CompagnieRepositoryImpl compagnieRepository,
    StatutCRepository statutCRepository,
    StatutHistoriqueCRepository historiqueRepository,
    PgPool pgPool
  ) {
    this.relationRepository = relationRepository;
    this.pointventeRepository = pointventeRepository;
    this.compagnieRepository = compagnieRepository;
    this.statutCRepository = statutCRepository;
    this.historiqueRepository = historiqueRepository;
    this.pgPool=pgPool;
  }

  @Override
  public Future<Long> linkCompagnieToPointvente(Long pointventeId, Long compagnieId) {
    // Validate pointvente exists
    return pointventeRepository.exists(pointventeId)
      .compose(pvExists -> {
        if (!pvExists) {
          return Future.failedFuture("Pointvente not found");
        }

        // Validate compagnie exists
        return compagnieRepository.findById(compagnieId)
          .map(compagnie -> compagnie != null);
      })
      .compose(compExists -> {
        if (!compExists) {
          return Future.failedFuture("Compagnie not found");
        }

        // Check if relation already exists
        return relationRepository.exists(pointventeId, compagnieId);
      })
      .compose(relationExists -> {
        if (relationExists) {
          return Future.failedFuture("Relation already exists");
        }

        // Create the relation
        RelationPointventeCompagnie relation = new RelationPointventeCompagnie();
        relation.setPointventeId(pointventeId);
        relation.setCompagnieId(compagnieId);

        return relationRepository.save(relation);
      })
      .compose(relationId -> {
        // Get or create ACTIF status
        return statutCRepository.findByLibelle("ACTIF")
          .compose(statut -> {
            if (statut == null) {
              return statutCRepository.save("ACTIF")
                .map(statutId -> {
                  StatutC newStatut = new StatutC();
                  newStatut.setId(statutId);
                  newStatut.setLibelle("ACTIF");
                  return newStatut;
                });
            }
            return Future.succeededFuture(statut);
          })
          .compose(statut -> {
            // Create initial status history entry
            StatutHistoriqueC historique = new StatutHistoriqueC();
            historique.setRelationId(relationId);
            historique.setIdStatutc(statut.getId());
            historique.setStatut("ACTIF");
            historique.setRaison("Initial creation");

            return historiqueRepository.save(historique)
              .map(v -> relationId);
          });
      });
  }

  @Override
  public Future<Void> updateRelationStatus(Long relationId, String status, String reason) {
    return relationRepository.findById(relationId)
      .compose(relation -> {
        if (relation == null) {
          return Future.failedFuture("Relation not found");
        }

        // Get or create the status
        return statutCRepository.findByLibelle(status)
          .compose(statut -> {
            if (statut == null) {
              return statutCRepository.save(status)
                .map(statutId -> {
                  StatutC newStatut = new StatutC();
                  newStatut.setId(statutId);
                  newStatut.setLibelle(status);
                  return newStatut;
                });
            }
            return Future.succeededFuture(statut);
          });
      })
      .compose(statut -> {
        // Update the relation
        return relationRepository.updateStatus(relationId, status, reason)
          .map(v -> statut);
      })
      .compose(statut -> {
        // Add history entry
        StatutHistoriqueC historique = new StatutHistoriqueC();
        historique.setRelationId(relationId);
        historique.setIdStatutc(statut.getId());
        historique.setStatut(status);
        historique.setRaison(reason);

        return historiqueRepository.save(historique)
          .map(v -> null);
      });
  }

  @Override
  public Future<JsonArray> getHistoriqueForRelation(Long relationId) {
    return historiqueRepository.findByRelationId(relationId)
      .map(historiques -> {
        JsonArray array = new JsonArray();
        historiques.forEach(h -> array.add(historiqueToJson(h)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> getRelationDetails(Long relationId) {
    return relationRepository.findById(relationId)
      .compose(relation -> {
        if (relation == null) {
          return Future.failedFuture("Relation not found");
        }

        JsonObject result = relationToJson(relation);

        // Get current status
        return historiqueRepository.getCurrentStatus(relationId)
          .map(currentStatus -> {
            if (currentStatus != null && currentStatus.getStatutC() != null) {
              result.put("current_status_label", currentStatus.getStatutC().getLibelle());
            }
            return result;
          });
      });
  }

  @Override
  public Future<Void> unlinkCompagnie(Long relationId) {
    // First get all history IDs for this relation
    String getHistoryIdsSql = """
            SELECT id FROM statut_historiquec WHERE relation_id = $1
        """;

    return pgPool.preparedQuery(getHistoryIdsSql)
      .execute(Tuple.of(relationId))
      .compose(rows -> {
        // Delete history entries one by one (to avoid foreign key issues)
        List<Future> deleteFutures = new ArrayList<>();
        rows.forEach(row -> {
          String deleteHistorySql = """
                        DELETE FROM statut_historiquec WHERE id = $1
                    """;
          deleteFutures.add(
            pgPool.preparedQuery(deleteHistorySql)
              .execute(Tuple.of(row.getLong("id")))
          );
        });

        if (deleteFutures.isEmpty()) {
          return Future.succeededFuture();
        }
        return CompositeFuture.all(deleteFutures);
      })
      .compose(v -> {
        // Now delete the relation itself
        return relationRepository.delete(relationId);
      });
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

    // Add related entity info if loaded
    if (relation.getCompagnie() != null) {
      json.put("compagnie_name", relation.getCompagnie().getRaison_social());
    }
    if (relation.getPointvente() != null) {
      json.put("pointvente_name", relation.getPointvente().getNomp());
    }

    return json;
  }

  private JsonObject historiqueToJson(StatutHistoriqueC historique) {
    JsonObject json = new JsonObject()
      .put("id", historique.getId())
      .put("relation_id", historique.getRelationId())
      .put("statut", historique.getStatut())
      .put("date_changement", historique.getDateChangement())
      .put("raison", historique.getRaison());

    if (historique.getDateDebutc() != null) {
      json.put("date_debutc", historique.getDateDebutc().toString());
    }
    if (historique.getDateFinc() != null) {
      json.put("date_finc", historique.getDateFinc().toString());
    }

    // Add status label if loaded
    if (historique.getStatutC() != null) {
      json.put("statut_label", historique.getStatutC().getLibelle());
    }

    return json;
  }
}
