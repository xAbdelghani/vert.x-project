package com.example.starter.repository.impl;

import com.example.starter.model.Abonnement;
import com.example.starter.model.Compagnie;
import com.example.starter.model.TypeAbonnement;
import com.example.starter.repository.AbonnementRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.util.ArrayList;
import java.util.List;

public class AbonnementRepositoryImpl implements AbonnementRepository {

  private final PgPool pgPool;

  public AbonnementRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<List<Abonnement>> findAll() {
    String sql = """
        SELECT a.*, t.libelle as type_libelle, t.categorie,
               c.raison_social
        FROM abonnement a
        LEFT JOIN type_abonnement t ON a.typeabonnement_id = t.id
        LEFT JOIN compagnies c ON a.compagnie_id = c.id
        ORDER BY a.date_abonnement DESC
    """;

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        List<Abonnement> abonnements = new ArrayList<>();
        rows.forEach(row -> abonnements.add(mapRowToAbonnement(row)));
        return abonnements;
      });
  }

  @Override
  public Future<List<Abonnement>> findByType(String category) {
    String sql = """
        SELECT a.*, t.libelle as type_libelle, t.categorie,
               c.raison_social
        FROM abonnement a
        LEFT JOIN type_abonnement t ON a.typeabonnement_id = t.id
        LEFT JOIN compagnies c ON a.compagnie_id = c.id
        WHERE a.type = $1
        ORDER BY a.date_abonnement DESC
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(category))
      .map(rows -> {
        List<Abonnement> abonnements = new ArrayList<>();
        rows.forEach(row -> abonnements.add(mapRowToAbonnement(row)));
        return abonnements;
      });
  }

  @Override
  public Future<Long> save(Abonnement abonnement) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM abonnement";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO abonnement (
                        id, compagnie_id, typeabonnement_id, date_abonnement,
                        date_fin, montant, type, statut, devise
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            abonnement.getCompagnieId(),
            abonnement.getTypeabonnementId(),
            abonnement.getDateAbonnement(),
            abonnement.getDateFin(),
            abonnement.getMontant(),
            abonnement.getType(),
            abonnement.getStatut(),
            abonnement.getDevise()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<Abonnement> findById(Long id) {
    String sql = """
            SELECT a.*, c.raison_social, t.libelle as type_libelle
            FROM abonnement a
            LEFT JOIN compagnies c ON a.compagnie_id = c.id
            LEFT JOIN type_abonnement t ON a.typeabonnement_id = t.id
            WHERE a.id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToAbonnement(rows.iterator().next());
      });
  }

  @Override
  public Future<Abonnement> findActiveByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT a.*, t.libelle as type_libelle
            FROM abonnement a
            LEFT JOIN type_abonnement t ON a.typeabonnement_id = t.id
            WHERE a.compagnie_id = $1
            AND a.statut = 'ACTIF'
            ORDER BY a.date_abonnement DESC
            LIMIT 1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToAbonnement(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Abonnement>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT a.*, t.libelle as type_libelle
            FROM abonnement a
            LEFT JOIN type_abonnement t ON a.typeabonnement_id = t.id
            WHERE a.compagnie_id = $1
            ORDER BY a.date_abonnement DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<Abonnement> abonnements = new ArrayList<>();
        rows.forEach(row -> abonnements.add(mapRowToAbonnement(row)));
        return abonnements;
      });
  }

  @Override
  public Future<Void> update(Long id, Abonnement abonnement) {
    String sql = """
        UPDATE abonnement SET
            montant = $1,
            statut = $2,
            date_fin = $3
        WHERE id = $4
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        abonnement.getMontant(),
        abonnement.getStatut(),
        abonnement.getDateFin(),
        id
      ))
      .map(rows -> null);
  }

  private Abonnement mapRowToAbonnement(Row row) {
    Abonnement abonnement = new Abonnement();
    abonnement.setId(row.getLong("id"));
    abonnement.setCompagnieId(row.getLong("compagnie_id"));
    abonnement.setTypeabonnementId(row.getLong("typeabonnement_id"));

    // Handle nullable fields
    Long factureId = row.getLong("facture_id");
    if (factureId != null) {
      abonnement.setFactureId(factureId);
    }

    abonnement.setDateAbonnement(row.getLocalDate("date_abonnement"));
    abonnement.setDateFin(row.getLocalDate("date_fin"));
    abonnement.setMontant(row.getBigDecimal("montant"));
    abonnement.setType(row.getString("type"));
    abonnement.setStatut(row.getString("statut"));
    abonnement.setDevise(row.getString("devise"));

    // Map related type if available
    try {
      String typeLibelle = row.getString("type_libelle");
      if (typeLibelle != null) {
        TypeAbonnement type = new TypeAbonnement();
        type.setId(abonnement.getTypeabonnementId());
        type.setLibelle(typeLibelle);
        abonnement.setTypeAbonnement(type);
      }
    } catch (Exception e) {
      // Column doesn't exist in this query
    }

    // Map company name if available
    try {
      String raisonSocial = row.getString("raison_social");
      if (raisonSocial != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(abonnement.getCompagnieId());
        compagnie.setRaison_social(raisonSocial);
        abonnement.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column doesn't exist in this query
    }

    return abonnement;
  }
}
