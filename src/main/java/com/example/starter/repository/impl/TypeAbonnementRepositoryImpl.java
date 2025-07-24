package com.example.starter.repository.impl;

import com.example.starter.model.TypeAbonnement;
import com.example.starter.repository.TypeAbonnementRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class TypeAbonnementRepositoryImpl implements TypeAbonnementRepository {


  private final PgPool pgPool;

  public TypeAbonnementRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(TypeAbonnement typeAbonnement) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM type_abonnement";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO type_abonnement (
                        id, libelle, categorie, duree, unite, description, actif
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            typeAbonnement.getLibelle(),
            typeAbonnement.getCategorie(),
            typeAbonnement.getDuree(),
            typeAbonnement.getUnite(),
            typeAbonnement.getDescription(),
            typeAbonnement.getActif() != null ? typeAbonnement.getActif() : true
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<TypeAbonnement> findById(Long id) {
    String sql = "SELECT * FROM type_abonnement WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToTypeAbonnement(rows.iterator().next());
      });
  }

  @Override
  public Future<TypeAbonnement> findByLibelle(String libelle) {
    String sql = "SELECT * FROM type_abonnement WHERE libelle = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(libelle))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToTypeAbonnement(rows.iterator().next());
      });
  }

  @Override
  public Future<List<TypeAbonnement>> findAll() {
    String sql = """
            SELECT * FROM type_abonnement
            ORDER BY categorie, libelle
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<TypeAbonnement> types = new ArrayList<>();
        rows.forEach(row -> types.add(mapRowToTypeAbonnement(row)));
        return types;
      });
  }

  @Override
  public Future<List<TypeAbonnement>> findByCategorie(String categorie) {
    String sql = """
            SELECT * FROM type_abonnement
            WHERE categorie = $1 AND actif = true
            ORDER BY libelle
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(categorie))
      .map(rows -> {
        List<TypeAbonnement> types = new ArrayList<>();
        rows.forEach(row -> types.add(mapRowToTypeAbonnement(row)));
        return types;
      });
  }

  @Override
  public Future<List<TypeAbonnement>> findActifs() {
    String sql = """
            SELECT * FROM type_abonnement
            WHERE actif = true
            ORDER BY categorie, libelle
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<TypeAbonnement> types = new ArrayList<>();
        rows.forEach(row -> types.add(mapRowToTypeAbonnement(row)));
        return types;
      });
  }

  @Override
  public Future<Void> update(Long id, TypeAbonnement typeAbonnement) {
    String sql = """
            UPDATE type_abonnement SET
                libelle = $1, categorie = $2, duree = $3,
                unite = $4, description = $5, actif = $6
            WHERE id = $7
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        typeAbonnement.getLibelle(),
        typeAbonnement.getCategorie(),
        typeAbonnement.getDuree(),
        typeAbonnement.getUnite(),
        typeAbonnement.getDescription(),
        typeAbonnement.getActif(),
        id
      ))
      .map(rows -> null);
  }

  @Override
  public Future<Void> delete(Long id) {
    // Soft delete by setting actif = false
    String sql = "UPDATE type_abonnement SET actif = false WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  @Override
  public Future<Boolean> isUsedInAbonnements(Long id) {
    String sql = """
            SELECT EXISTS(
                SELECT 1 FROM abonnement
                WHERE typeabonnement_id = $1
            )
        """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().next().getBoolean(0));
  }

  private TypeAbonnement mapRowToTypeAbonnement(Row row) {
    TypeAbonnement type = new TypeAbonnement();
    type.setId(row.getLong("id"));
    type.setLibelle(row.getString("libelle"));
    type.setCategorie(row.getString("categorie"));
    type.setDuree(row.getDouble("duree"));
    type.setUnite(row.getString("unite"));
    type.setDescription(row.getString("description"));
    type.setActif(row.getBoolean("actif"));
    return type;
  }



}
