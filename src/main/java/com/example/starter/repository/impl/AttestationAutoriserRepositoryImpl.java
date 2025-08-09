package com.example.starter.repository.impl;

import com.example.starter.model.AttestationAutoriser;
import com.example.starter.model.Compagnie;
import com.example.starter.model.TypeAttestation;
import com.example.starter.repository.AttestationAutoriserRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class AttestationAutoriserRepositoryImpl implements AttestationAutoriserRepository {

  private final PgPool pgPool;

  public AttestationAutoriserRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Void> save(Long compagnieId, Long typeAttestationId, Boolean flag) {
    String sql = """
            INSERT INTO attestationautoriser (compagniea_id, typeattestationa_id, flag)
            VALUES ($1, $2, $3)
            ON CONFLICT (compagniea_id, typeattestationa_id)
            DO UPDATE SET flag = $3
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId, typeAttestationId, flag))
      .mapEmpty();
  }

  @Override
  public Future<Void> update(Long compagnieId, Long typeAttestationId, Boolean flag) {
    return save(compagnieId, typeAttestationId, flag); // Same as save with ON CONFLICT
  }

  @Override
  public Future<Void> delete(Long compagnieId, Long typeAttestationId) {
    String sql = """
            DELETE FROM attestationautoriser
            WHERE compagniea_id = $1 AND typeattestationa_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId, typeAttestationId))
      .mapEmpty();
  }

  @Override
  public Future<AttestationAutoriser> find(Long compagnieId, Long typeAttestationId) {
    String sql = """
            SELECT aa.*, c.raison_social, t.libelle, t.prix_unitaire
            FROM attestationautoriser aa
            LEFT JOIN compagnies c ON aa.compagniea_id = c.id
            LEFT JOIN type_attestation t ON aa.typeattestationa_id = t.id
            WHERE aa.compagniea_id = $1 AND aa.typeattestationa_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId, typeAttestationId))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToAttestationAutoriser(rows.iterator().next());
      });
  }

  @Override
  public Future<List<AttestationAutoriser>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT aa.*, t.libelle, t.prix_unitaire, t.devise
            FROM attestationautoriser aa
            LEFT JOIN type_attestation t ON aa.typeattestationa_id = t.id
            WHERE aa.compagniea_id = $1 AND aa.flag = true
            ORDER BY t.libelle
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<AttestationAutoriser> list = new ArrayList<>();
        rows.forEach(row -> list.add(mapRowToAttestationAutoriser(row)));
        return list;
      });
  }

  @Override
  public Future<List<AttestationAutoriser>> findByTypeAttestationId(Long typeAttestationId) {
    String sql = """
            SELECT aa.*, c.raison_social
            FROM attestationautoriser aa
            LEFT JOIN compagnies c ON aa.compagniea_id = c.id
            WHERE aa.typeattestationa_id = $1
            ORDER BY c.raison_social
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(typeAttestationId))
      .map(rows -> {
        List<AttestationAutoriser> list = new ArrayList<>();
        rows.forEach(row -> list.add(mapRowToAttestationAutoriser(row)));
        return list;
      });
  }

  @Override
  public Future<List<AttestationAutoriser>> findAll() {
    String sql = """
            SELECT aa.*, c.raison_social, t.libelle, t.prix_unitaire
            FROM attestationautoriser aa
            LEFT JOIN compagnies c ON aa.compagniea_id = c.id
            LEFT JOIN type_attestation t ON aa.typeattestationa_id = t.id
            ORDER BY c.raison_social, t.libelle
        """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<AttestationAutoriser> list = new ArrayList<>();
        rows.forEach(row -> list.add(mapRowToAttestationAutoriser(row)));
        return list;
      });
  }

  @Override
  public Future<Boolean> isAuthorized(Long compagnieId, Long typeAttestationId) {
    String sql = """
            SELECT flag FROM attestationautoriser
            WHERE compagniea_id = $1 AND typeattestationa_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId, typeAttestationId))
      .map(rows -> {
        if (rows.size() == 0) return false;
        return rows.iterator().next().getBoolean("flag");
      });
  }

  private AttestationAutoriser mapRowToAttestationAutoriser(Row row) {
    AttestationAutoriser aa = new AttestationAutoriser();
    aa.setCompagnieId(row.getLong("compagniea_id"));
    aa.setTypeAttestationId(row.getLong("typeattestationa_id"));
    aa.setFlag(row.getBoolean("flag"));

    // Map type attestation if available
    try {
      String libelle = row.getString("libelle");
      if (libelle != null) {
        TypeAttestation type = new TypeAttestation();
        type.setId(aa.getTypeAttestationId());
        type.setLibelle(libelle);
        type.setPrixUnitaire(row.getBigDecimal("prix_unitaire"));
        type.setDevise(row.getString("devise"));
        aa.setTypeAttestation(type);
      }
    } catch (Exception e) {
      // Columns not in query
    }

    // Map company if available
    try {
      String raisonSocial = row.getString("raison_social");
      if (raisonSocial != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(aa.getCompagnieId());
        compagnie.setRaison_social(raisonSocial);
        aa.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column not in query
    }

    return aa;
  }
}
