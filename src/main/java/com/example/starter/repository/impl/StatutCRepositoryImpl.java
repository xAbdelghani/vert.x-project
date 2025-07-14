package com.example.starter.repository.impl;

import com.example.starter.model.StatutC;
import com.example.starter.repository.StatutCRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class StatutCRepositoryImpl implements StatutCRepository {
  private final PgPool pgPool;

  public StatutCRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(String libelle) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM statutc";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO statutc (id, libelle)
                    VALUES ($1, $2)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(nextId, libelle))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<StatutC> findById(Long id) {
    String sql = "SELECT * FROM statutc WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToStatutC(rows.iterator().next());
      });
  }

  @Override
  public Future<StatutC> findByLibelle(String libelle) {
    String sql = "SELECT * FROM statutc WHERE libelle = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(libelle))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToStatutC(rows.iterator().next());
      });
  }

  @Override
  public Future<List<StatutC>> findAll() {
    String sql = "SELECT * FROM statutc ORDER BY libelle";

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<StatutC> statuts = new ArrayList<>();
        rows.forEach(row -> statuts.add(mapRowToStatutC(row)));
        return statuts;
      });
  }

  private StatutC mapRowToStatutC(Row row) {
    StatutC statut = new StatutC();
    statut.setId(row.getLong("id"));
    statut.setLibelle(row.getString("libelle"));
    return statut;
  }
}
