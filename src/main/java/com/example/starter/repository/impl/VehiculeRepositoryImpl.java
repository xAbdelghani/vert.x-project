package com.example.starter.repository.impl;

import com.example.starter.model.Compagnie;
import com.example.starter.model.ModeleVehicule;
import com.example.starter.model.Vehicule;
import com.example.starter.repository.VehiculeRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class VehiculeRepositoryImpl implements VehiculeRepository {

  private final PgPool pgPool;

  public VehiculeRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Vehicule vehicule) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM vehicule";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String insertSql = """
                    INSERT INTO vehicule (
                        id, immatriculation, date_immatriculation, model_id, compagnie_id
                    ) VALUES ($1, $2, $3, $4, $5)
                    RETURNING id
                """;

        return pgPool.preparedQuery(insertSql)
          .execute(Tuple.of(
            nextId,
            vehicule.getImmatriculation(),
            vehicule.getDateImmatriculation(),
            vehicule.getModelId(),
            vehicule.getCompagnieId()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<Vehicule> findById(Long id) {
    String sql = """
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            WHERE v.id = $1
            GROUP BY v.id, m.id, c.id
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToVehicule(rows.iterator().next());
      });
  }

  @Override
  public Future<Vehicule> findByImmatriculation(String immatriculation) {
    String sql = """
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            WHERE UPPER(v.immatriculation) = UPPER($1)
            GROUP BY v.id, m.id, c.id
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(immatriculation))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToVehicule(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Vehicule>> findAll() {
    String sql = """
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            GROUP BY v.id, m.id, c.id
            ORDER BY v.immatriculation
        """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<Vehicule> vehicules = new ArrayList<>();
        rows.forEach(row -> vehicules.add(mapRowToVehicule(row)));
        return vehicules;
      });
  }

  @Override
  public Future<List<Vehicule>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            WHERE v.compagnie_id = $1
            GROUP BY v.id, m.id, c.id
            ORDER BY v.immatriculation
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<Vehicule> vehicules = new ArrayList<>();
        rows.forEach(row -> vehicules.add(mapRowToVehicule(row)));
        return vehicules;
      });
  }

  @Override
  public Future<List<Vehicule>> findByModelId(Long modelId) {
    String sql = """
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            WHERE v.model_id = $1
            GROUP BY v.id, m.id, c.id
            ORDER BY v.immatriculation
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(modelId))
      .map(rows -> {
        List<Vehicule> vehicules = new ArrayList<>();
        rows.forEach(row -> vehicules.add(mapRowToVehicule(row)));
        return vehicules;
      });
  }

  @Override
  public Future<Void> update(Long id, Vehicule vehicule) {
    String sql = """
            UPDATE vehicule SET
                immatriculation = $1,
                date_immatriculation = $2,
                model_id = $3,
                compagnie_id = $4
            WHERE id = $5
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        vehicule.getImmatriculation(),
        vehicule.getDateImmatriculation(),
        vehicule.getModelId(),
        vehicule.getCompagnieId(),
        id
      ))
      .mapEmpty();
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = "DELETE FROM vehicule WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .mapEmpty();
  }

  @Override
  public Future<Boolean> hasActiveAttestations(Long id) {
    String sql = """
            SELECT EXISTS (
                SELECT 1 FROM attestation
                WHERE vehicule_id = $1
                AND (date_fin >= CURRENT_DATE OR date_fin IS NULL)
                LIMIT 1
            ) as has_active
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean("has_active"));
  }

  @Override
  public Future<List<Vehicule>> search(String immatriculation, Long modelId, Long compagnieId) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql = new StringBuilder("""
            SELECT v.*, m.designation, m.type, m.marque,
                   m.annee, m.puissance_fiscale, m.carburant,
                   c.raison_social as compagnie_name,
                   COUNT(a.id) as attestation_count
            FROM vehicule v
            LEFT JOIN modele_vehicule m ON v.model_id = m.id
            LEFT JOIN compagnies c ON v.compagnie_id = c.id
            LEFT JOIN attestation a ON a.vehicule_id = v.id
            WHERE 1=1
        """);

    int paramIndex = 1;

    if (immatriculation != null && !immatriculation.isEmpty()) {
      sql.append(" AND UPPER(v.immatriculation) LIKE UPPER($").append(paramIndex++).append(")");
      params.add("%" + immatriculation + "%");
    }

    if (modelId != null) {
      sql.append(" AND v.model_id = $").append(paramIndex++);
      params.add(modelId);
    }

    if (compagnieId != null) {
      sql.append(" AND v.compagnie_id = $").append(paramIndex++);
      params.add(compagnieId);
    }

    sql.append(" GROUP BY v.id, m.id, c.id ORDER BY v.immatriculation");

    return pgPool.preparedQuery(sql.toString())
      .execute(Tuple.from(params))
      .map(rows -> {
        List<Vehicule> vehicules = new ArrayList<>();
        rows.forEach(row -> vehicules.add(mapRowToVehicule(row)));
        return vehicules;
      });
  }

  private Vehicule mapRowToVehicule(Row row) {
    Vehicule vehicule = new Vehicule();
    vehicule.setId(row.getLong("id"));
    vehicule.setImmatriculation(row.getString("immatriculation"));
    vehicule.setDateImmatriculation(row.getLocalDate("date_immatriculation"));
    vehicule.setModelId(row.getLong("model_id"));

    // FIXED: Always set compagnie_id without conditions
    vehicule.setCompagnieId(row.getLong("compagnie_id"));

    // Map model if available
    try {
      String designation = row.getString("designation");
      if (designation != null) {
        ModeleVehicule modele = new ModeleVehicule();
        modele.setId(vehicule.getModelId());
        modele.setDesignation(designation);
        modele.setType(row.getString("type"));
        modele.setMarque(row.getString("marque"));
        modele.setAnnee(row.getInteger("annee"));
        modele.setPuissanceFiscale(row.getDouble("puissance_fiscale"));
        modele.setCarburant(row.getString("carburant"));
        vehicule.setModeleVehicule(modele);
      }
    } catch (Exception e) {
      // Columns not in query
    }

    // Map company if available
    try {
      String compagnieName = row.getString("compagnie_name");
      if (compagnieName != null && vehicule.getCompagnieId() != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(vehicule.getCompagnieId());
        compagnie.setRaison_social(compagnieName);
        vehicule.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column not in query
    }

    // Try to get attestation count
    try {
      vehicule.setAttestationCount(row.getInteger("attestation_count"));
    } catch (Exception e) {
      // Column not in query
    }

    return vehicule;
  }
}
