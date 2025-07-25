package com.example.starter.repository.impl;

import com.example.starter.model.TypeAttestation;
import com.example.starter.repository.TypeAttestationRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class TypeAttestationRepositoryImpl implements TypeAttestationRepository {

  private final PgPool pgPool;

  public TypeAttestationRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(TypeAttestation typeAttestation) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM type_attestation";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String insertSql = """
                    INSERT INTO type_attestation (id, libelle, prix_unitaire, devise)
                    VALUES ($1, $2, $3, $4)
                    RETURNING id
                """;

        return pgPool.preparedQuery(insertSql)
          .execute(Tuple.of(
            nextId,
            typeAttestation.getLibelle(),
            typeAttestation.getPrixUnitaire(),
            typeAttestation.getDevise()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }


  @Override
  public Future<TypeAttestation> findById(Long id) {
    String sql = "SELECT * FROM type_attestation WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToTypeAttestation(rows.iterator().next());
      });
  }

  @Override
  public Future<TypeAttestation> findByLibelle(String libelle) {
    String sql = "SELECT * FROM type_attestation WHERE UPPER(libelle) = UPPER($1)";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(libelle))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToTypeAttestation(rows.iterator().next());
      });
  }

  @Override
  public Future<List<TypeAttestation>> findAll() {
    String sql = "SELECT * FROM type_attestation ORDER BY libelle";

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<TypeAttestation> types = new ArrayList<>();
        rows.forEach(row -> types.add(mapRowToTypeAttestation(row)));
        return types;
      });
  }

  @Override
  public Future<Void> update(Long id, TypeAttestation typeAttestation) {
    String sql = """
            UPDATE type_attestation
            SET libelle = $1, prix_unitaire = $2, devise = $3
            WHERE id = $4
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        typeAttestation.getLibelle(),
        typeAttestation.getPrixUnitaire(),
        typeAttestation.getDevise(),
        id
      ))
      .mapEmpty();
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = "DELETE FROM type_attestation WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .mapEmpty();
  }

  @Override
  public Future<Boolean> isUsedInAttestations(Long id) {
    String sql = """
            SELECT EXISTS (
                SELECT 1 FROM attestation
                WHERE typeattestation_id = $1
                LIMIT 1
            ) as is_used
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean("is_used"));
  }

  private TypeAttestation mapRowToTypeAttestation(Row row) {
    TypeAttestation type = new TypeAttestation();
    type.setId(row.getLong("id"));
    type.setLibelle(row.getString("libelle"));
    type.setPrixUnitaire(row.getBigDecimal("prix_unitaire"));
    type.setDevise(row.getString("devise"));
    return type;
  }


}
