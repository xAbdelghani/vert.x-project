package com.example.starter.repository.impl;

import com.example.starter.model.Pointvente;
import com.example.starter.repository.PointventeRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PointventeRepositoryImpl implements PointventeRepository {

  private final PgPool pgPool;

  public PointventeRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Pointvente pointvente) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM pointvente";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO pointvente (id, nomp, emailp, telephonep, date_debutp)
                    VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            pointvente.getNomp(),
            pointvente.getEmailp(),
            pointvente.getTelephonep(),
            LocalDate.now()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<Pointvente> findById(Long id) {
    String sql = """
            SELECT * FROM pointvente WHERE id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToPointvente(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Pointvente>> findAll() {
    String sql = """
            SELECT pv.*, COUNT(DISTINCT rpc.compagnie_id) as company_count
            FROM pointvente pv
            LEFT JOIN relationpointventecompagnie rpc ON pv.id = rpc.pointvente_id
            GROUP BY pv.id
            ORDER BY pv.nomp
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<Pointvente> pointventes = new ArrayList<>();
        rows.forEach(row -> pointventes.add(mapRowToPointvente(row)));
        return pointventes;
      });
  }

  @Override
  public Future<Void> update(Long id, Pointvente pointvente) {
    String sql = """
            UPDATE pointvente
            SET nomp = $1, emailp = $2, telephonep = $3
            WHERE id = $4
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        pointvente.getNomp(),
        pointvente.getEmailp(),
        pointvente.getTelephonep(),
        id
      ))
      .map(rows -> null);
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = """
            DELETE FROM pointvente WHERE id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  @Override
  public Future<Boolean> exists(Long id) {
    String sql = """
            SELECT EXISTS(SELECT 1 FROM pointvente WHERE id = $1)
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean(0));
  }

  private Pointvente mapRowToPointvente(Row row) {
    Pointvente pointvente = new Pointvente();
    pointvente.setId(row.getLong("id"));
    pointvente.setNomp(row.getString("nomp"));
    pointvente.setEmailp(row.getString("emailp"));
    pointvente.setTelephonep(row.getString("telephonep"));
    pointvente.setDateDebutp(row.getLocalDate("date_debutp"));
    pointvente.setDateFinp(row.getLocalDate("date_finp"));
    return pointvente;
  }
}
