package com.example.starter.repository.impl;

import com.example.starter.model.Fonction;
import com.example.starter.repository.FonctionRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class FonctionRepositoryImpl implements FonctionRepository {

  private final PgPool pgPool;

  public FonctionRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(String qualite) {
    String sql = """
            INSERT INTO fonction (qualite)
            VALUES ($1)
            RETURNING id
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(qualite))
      .map(rows -> rows.iterator().next().getLong("id"));
  }

  @Override
  public Future<Fonction> findById(Long id) {
    String sql = """
            SELECT * FROM fonction WHERE id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToFonction(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Fonction>> findAll() {
    String sql = """
            SELECT * FROM fonction ORDER BY qualite
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<Fonction> fonctions = new ArrayList<>();
        rows.forEach(row -> fonctions.add(mapRowToFonction(row)));
        return fonctions;
      });
  }

  @Override
  public Future<Void> update(Long id, String qualite) {
    String sql = """
            UPDATE fonction SET qualite = $1
            WHERE id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(qualite, id))
      .map(rows -> null);
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = """
            DELETE FROM fonction WHERE id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  @Override
  public Future<Boolean> exists(Long id) {
    String sql = """
            SELECT EXISTS(SELECT 1 FROM fonction WHERE id = $1)
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean(0));
  }

  private Fonction mapRowToFonction(Row row) {
    Fonction fonction = new Fonction();
    fonction.setId(row.getLong("id"));
    fonction.setQualite(row.getString("qualite"));
    return fonction;
  }

}
