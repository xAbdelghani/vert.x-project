package com.example.starter.repository.impl;

import com.example.starter.model.Agence;
import com.example.starter.repository.AgenceRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AgenceRepositoryImpl implements AgenceRepository {

  private final PgPool pgPool;

  public AgenceRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Agence> save(Agence agence) {
    String sql = """
            INSERT INTO agence (noma, adressea, telephonea, date_debuta, date_fina, status, compagnie_id)
            VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *
            """;
    // Set default values
    agence.setStatus("OUVERT");
    agence.setDateFina(null);

    Tuple params = Tuple.of(
      agence.getNoma(),
      agence.getAdressea(),
      agence.getTelephonea(),
      agence.getDateDebuta(),
      agence.getDateFina(),
      agence.getStatus(),
      agence.getCompagnieId()
    );

    return pgPool.preparedQuery(sql)
      .execute(params)
      .compose(rows -> {
        Agence savedAgence = mapRow(rows.iterator().next());
        // Load company info after save
        return loadCompagnieInfo(savedAgence);
      });
  }

  @Override
  public Future<List<Agence>> findAll() {
    String sql = """
            SELECT a.*, c.nom as compagnie_nom, c.raison_social as compagnie_raison_social
            FROM agence a
            LEFT JOIN compagnies c ON a.compagnie_id = c.id
            ORDER BY a.id DESC
            """;

    return pgPool.query(sql)
      .execute()
      .map(this::mapRowsWithCompagnie);
  }

  @Override
  public Future<Agence> findById(Long id) {
    String sql = """
            SELECT a.*, c.nom as compagnie_nom, c.raison_social as compagnie_raison_social
            FROM agence a
            LEFT JOIN compagnies c ON a.compagnie_id = c.id
            WHERE a.id = $1
            """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowWithCompagnie(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Agence>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT a.*, c.nom as compagnie_nom, c.raison_social as compagnie_raison_social
            FROM agence a
            LEFT JOIN compagnies c ON a.compagnie_id = c.id
            WHERE a.compagnie_id = $1
            ORDER BY a.id DESC
            """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(this::mapRowsWithCompagnie);
  }

  @Override
  public Future<List<Agence>> findByStatus(String status) {
    String sql = """
            SELECT a.*, c.nom as compagnie_nom, c.raison_social as compagnie_raison_social
            FROM agence a
            LEFT JOIN compagnies c ON a.compagnie_id = c.id
            WHERE a.status = $1
            ORDER BY a.id DESC
            """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(status))
      .map(this::mapRowsWithCompagnie);
  }

  @Override
  public Future<Agence> update(Agence agence) {
    // If date_fina is set, automatically update status to CLOTURE
    if (agence.getDateFina() != null) {
      agence.setStatus("CLOTURE");
    }

    String sql = """
            UPDATE agence
            SET noma = $2, adressea = $3, telephonea = $4,
                date_debuta = $5, date_fina = $6, status = $7, compagnie_id = $8
            WHERE id = $1 RETURNING *
            """;

    Tuple params = Tuple.of(
      agence.getId(),
      agence.getNoma(),
      agence.getAdressea(),
      agence.getTelephonea(),
      agence.getDateDebuta(),
      agence.getDateFina(),
      agence.getStatus(),
      agence.getCompagnieId()
    );

    return pgPool.preparedQuery(sql)
      .execute(params)
      .compose(rows -> {
        if (rows.size() == 0) return Future.succeededFuture(null);
        Agence updatedAgence = mapRow(rows.iterator().next());
        return loadCompagnieInfo(updatedAgence);
      });
  }

  @Override
  public Future<Boolean> updateDateFinToToday(Long id) {
    String sql = """
            UPDATE agence
            SET date_fina = $2, status = 'CLOTURE'
            WHERE id = $1
            """;
    Tuple params = Tuple.of(id, LocalDate.now());
    return pgPool.preparedQuery(sql)
      .execute(params)
      .map(result -> result.rowCount() > 0);
  }

  @Override
  public Future<Boolean> deleteById(Long id) {
    String sql = "DELETE FROM agence WHERE id = $1";
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(result -> result.rowCount() > 0);
  }

  // Helper method to load company info separately
  private Future<Agence> loadCompagnieInfo(Agence agence) {
    if (agence.getCompagnieId() == null) {
      return Future.succeededFuture(agence);
    }

    String sql = "SELECT nom, raison_social FROM compagnies WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(agence.getCompagnieId()))
      .map(rows -> {
        if (rows.size() > 0) {
          Row row = rows.iterator().next();
          agence.setCompagnieNom(row.getString("nom"));
          agence.setCompagnieRaisonSocial(row.getString("raison_social"));
        }
        return agence;
      })
      .otherwise(err -> {
        System.err.println("Failed to load compagnie info: " + err.getMessage());
        return agence;
      });
  }


  protected Agence mapRow(Row row) {
    Agence agence = new Agence();
    agence.setId(row.getLong("id"));
    agence.setNoma(row.getString("noma"));
    agence.setAdressea(row.getString("adressea"));
    agence.setTelephonea(row.getString("telephonea"));
    agence.setDateDebuta(row.getLocalDate("date_debuta"));
    agence.setDateFina(row.getLocalDate("date_fina"));
    agence.setStatus(row.getString("status"));
    agence.setCompagnieId(row.getLong("compagnie_id"));
    return agence;
  }

  private Agence mapRowWithCompagnie(Row row) {
    Agence agence = mapRow(row);

    // Add company info from the join
    try {
      String compagnieNom = row.getString("compagnie_nom");
      String compagnieRaisonSocial = row.getString("compagnie_raison_social");

      if (compagnieNom != null) {
        agence.setCompagnieNom(compagnieNom);
      }
      if (compagnieRaisonSocial != null) {
        agence.setCompagnieRaisonSocial(compagnieRaisonSocial);
      }
    } catch (Exception e) {
      // Company info might not be available if LEFT JOIN returns null
      System.err.println("No company info available for agence: " + agence.getId());
    }

    return agence;
  }

  private List<Agence> mapRowsWithCompagnie(RowSet<Row> rows) {
    List<Agence> result = new ArrayList<>();
    for (Row row : rows) {
      result.add(mapRowWithCompagnie(row));
    }
    return result;
  }



}
