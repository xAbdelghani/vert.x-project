package com.example.starter.repository.impl;

import com.example.starter.model.Compagnie;
import com.example.starter.model.Pointvente;
import com.example.starter.model.RelationPointventeCompagnie;
import com.example.starter.repository.RelationPointventeCompagnieRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RelationPointventeCompagnieRepositoryImpl implements RelationPointventeCompagnieRepository {

  private final PgPool pgPool;

  public RelationPointventeCompagnieRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(RelationPointventeCompagnie relation) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM relationpointventecompagnie";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO relationpointventecompagnie
                    (id, pointvente_id, compagnie_id, date_debut, active, status)
                    VALUES ($1, $2, $3, $4, $5, $6)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            relation.getPointventeId(),
            relation.getCompagnieId(),
            LocalDate.now(),
            true,
            "ACTIF"
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<RelationPointventeCompagnie> findById(Long id) {
    String sql = """
            SELECT rpc.*, c.raison_social as compagnie_name, p.nomp as pointvente_name
            FROM relationpointventecompagnie rpc
            LEFT JOIN compagnies c ON rpc.compagnie_id = c.id
            LEFT JOIN pointvente p ON rpc.pointvente_id = p.id
            WHERE rpc.id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToRelation(rows.iterator().next());
      });
  }

  @Override
  public Future<List<RelationPointventeCompagnie>> findByPointventeId(Long pointventeId) {
    String sql = """
            SELECT rpc.*, c.raison_social as compagnie_name, c.email as compagnie_email,
                   c.telephone as compagnie_telephone
            FROM relationpointventecompagnie rpc
            LEFT JOIN compagnies c ON rpc.compagnie_id = c.id
            WHERE rpc.pointvente_id = $1
            ORDER BY rpc.date_debut DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(pointventeId))
      .map(rows -> {
        List<RelationPointventeCompagnie> relations = new ArrayList<>();
        rows.forEach(row -> relations.add(mapRowToRelation(row)));
        return relations;
      });
  }

  @Override
  public Future<List<RelationPointventeCompagnie>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT rpc.*, p.nomp as pointvente_name
            FROM relationpointventecompagnie rpc
            LEFT JOIN pointvente p ON rpc.pointvente_id = p.id
            WHERE rpc.compagnie_id = $1
            ORDER BY rpc.date_debut DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<RelationPointventeCompagnie> relations = new ArrayList<>();
        rows.forEach(row -> relations.add(mapRowToRelation(row)));
        return relations;
      });
  }

  @Override
  public Future<RelationPointventeCompagnie> findByPointventeAndCompagnie(Long pointventeId, Long compagnieId) {
    String sql = """
            SELECT * FROM relationpointventecompagnie
            WHERE pointvente_id = $1 AND compagnie_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(pointventeId, compagnieId))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToRelation(rows.iterator().next());
      });
  }

  @Override
  public Future<Void> updateStatus(Long id, String status, String reason) {
    String sql = """
            UPDATE relationpointventecompagnie
            SET status = $1, suspension_reason = $2,
                active = $3, date_fin = $4
            WHERE id = $5
        """;

    boolean isActive = "ACTIF".equals(status);
    LocalDate dateFin = isActive ? null : LocalDate.now();

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(status, reason, isActive, dateFin, id))
      .map(rows -> null);
  }

  @Override
  public Future<Boolean> exists(Long pointventeId, Long compagnieId) {
    String sql = """
            SELECT EXISTS(
                SELECT 1 FROM relationpointventecompagnie
                WHERE pointvente_id = $1 AND compagnie_id = $2
            )
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(pointventeId, compagnieId))
      .map(rows -> rows.iterator().next().getBoolean(0));
  }

  private RelationPointventeCompagnie mapRowToRelation(Row row) {
    RelationPointventeCompagnie relation = new RelationPointventeCompagnie();
    relation.setId(row.getLong("id"));
    relation.setPointventeId(row.getLong("pointvente_id"));
    relation.setCompagnieId(row.getLong("compagnie_id"));
    relation.setDateDebut(row.getLocalDate("date_debut"));
    relation.setDateFin(row.getLocalDate("date_fin"));
    relation.setActive(row.getBoolean("active"));
    relation.setStatus(row.getString("status"));
    relation.setSuspensionReason(row.getString("suspension_reason"));

    // Map related entities if available - check column names carefully
    try {
      String compagnieName = row.getString("compagnie_name");
      if (compagnieName != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(relation.getCompagnieId());
        compagnie.setRaison_social(compagnieName);

        // Check if additional company fields exist
        try {
          compagnie.setEmail(row.getString("compagnie_email"));
          compagnie.setTelephone(row.getString("compagnie_telephone"));
        } catch (Exception e) {
          // These columns might not be in all queries
        }
        relation.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // compagnie_name column doesn't exist in this query
    }

    try {
      String pointventeName = row.getString("pointvente_name");
      if (pointventeName != null) {
        Pointvente pointvente = new Pointvente();
        pointvente.setId(relation.getPointventeId());
        pointvente.setNomp(pointventeName);
        relation.setPointvente(pointvente);
      }
    } catch (Exception e) {
      // pointvente_name column doesn't exist in this query
    }

    return relation;
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = """
            DELETE FROM relationpointventecompagnie WHERE id = $1
        """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }




}
