package com.example.starter.repository.impl;

import com.example.starter.model.ModeleVehicule;
import com.example.starter.repository.ModeleVehiculeRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class ModeleVehiculeRepositoryImpl implements ModeleVehiculeRepository {

  private final PgPool pgPool;

  public ModeleVehiculeRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(ModeleVehicule modele) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM modele_vehicule";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String insertSql = """
                    INSERT INTO modele_vehicule (
                        id, designation, type, marque, annee,
                        puissance_fiscale, carburant
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                """;

        return pgPool.preparedQuery(insertSql)
          .execute(Tuple.of(
            nextId,
            modele.getDesignation(),
            modele.getType(),
            modele.getMarque(),
            modele.getAnnee(),
            modele.getPuissanceFiscale(),
            modele.getCarburant()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<ModeleVehicule> findById(Long id) {
    String sql = """
            SELECT m.*, COUNT(v.id) as vehicle_count
            FROM modele_vehicule m
            LEFT JOIN vehicule v ON v.model_id = m.id
            WHERE m.id = $1
            GROUP BY m.id
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToModeleVehicule(rows.iterator().next());
      });
  }

  @Override
  public Future<List<ModeleVehicule>> findAll() {
    String sql = """
            SELECT m.*, COUNT(v.id) as vehicle_count
            FROM modele_vehicule m
            LEFT JOIN vehicule v ON v.model_id = m.id
            GROUP BY m.id
            ORDER BY m.marque, m.designation
        """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<ModeleVehicule> modeles = new ArrayList<>();
        rows.forEach(row -> modeles.add(mapRowToModeleVehicule(row)));
        return modeles;
      });
  }

  @Override
  public Future<List<ModeleVehicule>> findByMarque(String marque) {
    String sql = """
            SELECT m.*, COUNT(v.id) as vehicle_count
            FROM modele_vehicule m
            LEFT JOIN vehicule v ON v.model_id = m.id
            WHERE UPPER(m.marque) = UPPER($1)
            GROUP BY m.id
            ORDER BY m.designation
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(marque))
      .map(rows -> {
        List<ModeleVehicule> modeles = new ArrayList<>();
        rows.forEach(row -> modeles.add(mapRowToModeleVehicule(row)));
        return modeles;
      });
  }

  @Override
  public Future<List<ModeleVehicule>> findByType(String type) {
    String sql = """
            SELECT m.*, COUNT(v.id) as vehicle_count
            FROM modele_vehicule m
            LEFT JOIN vehicule v ON v.model_id = m.id
            WHERE UPPER(m.type) = UPPER($1)
            GROUP BY m.id
            ORDER BY m.marque, m.designation
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(type))
      .map(rows -> {
        List<ModeleVehicule> modeles = new ArrayList<>();
        rows.forEach(row -> modeles.add(mapRowToModeleVehicule(row)));
        return modeles;
      });
  }

  @Override
  public Future<List<String>> findAllMarques() {
    String sql = """
            SELECT DISTINCT marque
            FROM modele_vehicule
            WHERE marque IS NOT NULL
            ORDER BY marque
        """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<String> marques = new ArrayList<>();
        rows.forEach(row -> marques.add(row.getString("marque")));
        return marques;
      });
  }

  @Override
  public Future<List<String>> findAllTypes() {
    String sql = """
            SELECT DISTINCT type
            FROM modele_vehicule
            WHERE type IS NOT NULL
            ORDER BY type
        """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<String> types = new ArrayList<>();
        rows.forEach(row -> types.add(row.getString("type")));
        return types;
      });
  }

  @Override
  public Future<Void> update(Long id, ModeleVehicule modele) {
    String sql = """
            UPDATE modele_vehicule SET
                designation = $1, type = $2, marque = $3,
                annee = $4, puissance_fiscale = $5, carburant = $6
            WHERE id = $7
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        modele.getDesignation(),
        modele.getType(),
        modele.getMarque(),
        modele.getAnnee(),
        modele.getPuissanceFiscale(),
        modele.getCarburant(),
        id
      ))
      .mapEmpty();
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = "DELETE FROM modele_vehicule WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .mapEmpty();
  }

  @Override
  public Future<Boolean> isUsedInVehicles(Long id) {
    String sql = """
            SELECT EXISTS (
                SELECT 1 FROM vehicule
                WHERE model_id = $1
                LIMIT 1
            ) as is_used
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean("is_used"));
  }

  @Override
  public Future<List<ModeleVehicule>> search(String marque, String type, String carburant) {
    List<Object> params = new ArrayList<>();
    StringBuilder sql = new StringBuilder("""
            SELECT m.*, COUNT(v.id) as vehicle_count
            FROM modele_vehicule m
            LEFT JOIN vehicule v ON v.model_id = m.id
            WHERE 1=1
        """);

    int paramIndex = 1;

    if (marque != null && !marque.isEmpty()) {
      sql.append(" AND UPPER(m.marque) = UPPER($").append(paramIndex++).append(")");
      params.add(marque);
    }

    if (type != null && !type.isEmpty()) {
      sql.append(" AND UPPER(m.type) = UPPER($").append(paramIndex++).append(")");
      params.add(type);
    }

    if (carburant != null && !carburant.isEmpty()) {
      sql.append(" AND UPPER(m.carburant) = UPPER($").append(paramIndex++).append(")");
      params.add(carburant);
    }

    sql.append(" GROUP BY m.id ORDER BY m.marque, m.designation");

    return pgPool.preparedQuery(sql.toString())
      .execute(Tuple.from(params))
      .map(rows -> {
        List<ModeleVehicule> modeles = new ArrayList<>();
        rows.forEach(row -> modeles.add(mapRowToModeleVehicule(row)));
        return modeles;
      });
  }

  private ModeleVehicule mapRowToModeleVehicule(Row row) {
    ModeleVehicule modele = new ModeleVehicule();
    modele.setId(row.getLong("id"));
    modele.setDesignation(row.getString("designation"));
    modele.setType(row.getString("type"));
    modele.setMarque(row.getString("marque"));
    modele.setAnnee(row.getInteger("annee"));
    modele.setPuissanceFiscale(row.getDouble("puissance_fiscale"));
    modele.setCarburant(row.getString("carburant"));

    // Try to get vehicle count if available
    try {
      modele.setVehicleCount(row.getInteger("vehicle_count"));
    } catch (Exception e) {
      // Column not in query
    }

    return modele;
  }
}
