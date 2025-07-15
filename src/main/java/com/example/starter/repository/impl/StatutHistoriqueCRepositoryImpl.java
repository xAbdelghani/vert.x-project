package com.example.starter.repository.impl;

import com.example.starter.model.StatutC;
import com.example.starter.model.StatutHistoriqueC;
import com.example.starter.repository.StatutHistoriqueCRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StatutHistoriqueCRepositoryImpl implements StatutHistoriqueCRepository {

  private final PgPool pgPool;

  public StatutHistoriqueCRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(StatutHistoriqueC historique) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM statut_historiquec";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO statut_historiquec
                    (id, relation_id, id_statutc, date_debutc, date_changement, raison, statut)
                    VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            historique.getRelationId(),
            historique.getIdStatutc(),
            LocalDate.now(),
            LocalDateTime.now().toString(),
            historique.getRaison(),
            historique.getStatut()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<List<StatutHistoriqueC>> findByRelationId(Long relationId) {
    String sql = """
            SELECT sh.*, s.libelle as statut_libelle
            FROM statut_historiquec sh
            LEFT JOIN statutc s ON sh.id_statutc = s.id
            WHERE sh.relation_id = $1
            ORDER BY sh.date_changement DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(relationId))
      .map(rows -> {
        List<StatutHistoriqueC> historiques = new ArrayList<>();
        rows.forEach(row -> historiques.add(mapRowToHistorique(row)));
        return historiques;
      });
  }

  @Override
  public Future<StatutHistoriqueC> getCurrentStatus(Long relationId) {
    String sql = """
            SELECT sh.*, s.libelle as statut_libelle
            FROM statut_historiquec sh
            LEFT JOIN statutc s ON sh.id_statutc = s.id
            WHERE sh.relation_id = $1
            ORDER BY sh.date_changement DESC
            LIMIT 1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(relationId))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToHistorique(rows.iterator().next());
      });
  }

  private StatutHistoriqueC mapRowToHistorique(Row row) {
    StatutHistoriqueC historique = new StatutHistoriqueC();
    historique.setId(row.getLong("id"));
    historique.setRelationId(row.getLong("relation_id"));
    historique.setIdStatutc(row.getLong("id_statutc"));
    historique.setDateDebutc(row.getLocalDate("date_debutc"));
    historique.setDateFinc(row.getLocalDate("date_finc"));
    historique.setDateChangement(row.getString("date_changement"));
    historique.setRaison(row.getString("raison"));
    historique.setStatut(row.getString("statut"));

    if (row.getString("statut_libelle") != null) {
      StatutC statutC = new StatutC();
      statutC.setId(historique.getIdStatutc());
      statutC.setLibelle(row.getString("statut_libelle"));
      historique.setStatutC(statutC);
    }

    return historique;
  }


  @Override
  public Future<Void> deleteByRelationId(Long relationId) {
    String sql = "DELETE FROM statut_historiquec WHERE relation_id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(relationId))
      .map(rows -> null);
  }
}
