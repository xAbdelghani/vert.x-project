package com.example.starter.repository.impl;

import com.example.starter.model.Compagnie;
import com.example.starter.model.SoldePrepaye;
import com.example.starter.repository.SoldePrePayeRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SoldePrePayeRepositoryImpl implements SoldePrePayeRepository {

  private final PgPool pgPool;

  public SoldePrePayeRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(SoldePrepaye soldePrepaye) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM solde_prepaye";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
                    INSERT INTO solde_prepaye (
                        id, compagnie_id, date_abonnement, solde,
                        solde_attestation, type, statut, devise
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
                    RETURNING id
                """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(
            nextId,
            soldePrepaye.getCompagnieId(),
            LocalDate.now(),
            soldePrepaye.getSolde(),
            soldePrepaye.getSoldeAttestation(),
            soldePrepaye.getType(),
            soldePrepaye.getStatut(),
            soldePrepaye.getDevise()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<SoldePrepaye> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT sp.*, c.raison_social
            FROM solde_prepaye sp
            LEFT JOIN compagnies c ON sp.compagnie_id = c.id
            WHERE sp.compagnie_id = $1
            ORDER BY sp.id DESC
            LIMIT 1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToSoldePrepaye(rows.iterator().next());
      });
  }

  @Override
  public Future<List<SoldePrepaye>> findAll() {
    String sql = """
            SELECT sp.*, c.raison_social
            FROM solde_prepaye sp
            LEFT JOIN compagnies c ON sp.compagnie_id = c.id
            ORDER BY c.raison_social
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<SoldePrepaye> soldes = new ArrayList<>();
        rows.forEach(row -> soldes.add(mapRowToSoldePrepaye(row)));
        return soldes;
      });
  }

  // NEW METHOD - Find all by type (PREPAYE only)
  @Override
  public Future<List<SoldePrepaye>> findAllByType(String type) {
    String sql = """
            SELECT sp.*, c.raison_social
            FROM solde_prepaye sp
            LEFT JOIN compagnies c ON sp.compagnie_id = c.id
            WHERE sp.type = $1
            ORDER BY c.raison_social
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(type))
      .map(rows -> {
        List<SoldePrepaye> soldes = new ArrayList<>();
        rows.forEach(row -> soldes.add(mapRowToSoldePrepaye(row)));
        return soldes;
      });
  }

  @Override
  public Future<Void> updateSolde(Long compagnieId, BigDecimal newSolde) {
    String sql = """
            UPDATE solde_prepaye
            SET solde = $1
            WHERE compagnie_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(newSolde, compagnieId))
      .map(rows -> null);
  }

  @Override
  public Future<Void> addToSolde(Long compagnieId, BigDecimal amount) {
    String sql = """
            UPDATE solde_prepaye
            SET solde = solde + $1
            WHERE compagnie_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(amount, compagnieId))
      .map(rows -> null);
  }

  @Override
  public Future<Void> deductFromSolde(Long compagnieId, BigDecimal amount) {
    String sql = """
            UPDATE solde_prepaye
            SET solde = solde - $1
            WHERE compagnie_id = $2 AND solde >= $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(amount, compagnieId))
      .compose(result -> {
        if (result.rowCount() == 0) {
          return Future.failedFuture("Insufficient balance");
        }
        return Future.succeededFuture();
      });
  }

  @Override
  public Future<List<SoldePrepaye>> findLowBalances(BigDecimal threshold) {
    String sql = """
            SELECT sp.*, c.raison_social
            FROM solde_prepaye sp
            LEFT JOIN compagnies c ON sp.compagnie_id = c.id
            WHERE sp.solde <= $1
            ORDER BY sp.solde ASC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(threshold))
      .map(rows -> {
        List<SoldePrepaye> soldes = new ArrayList<>();
        rows.forEach(row -> soldes.add(mapRowToSoldePrepaye(row)));
        return soldes;
      });
  }

  // NEW METHOD - Find low balances by type
  @Override
  public Future<List<SoldePrepaye>> findLowBalancesByType(String type, BigDecimal threshold) {
    String sql = """
            SELECT sp.*, c.raison_social
            FROM solde_prepaye sp
            LEFT JOIN compagnies c ON sp.compagnie_id = c.id
            WHERE sp.type = $1 AND sp.solde <= $2
            ORDER BY sp.solde ASC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(type, threshold))
      .map(rows -> {
        List<SoldePrepaye> soldes = new ArrayList<>();
        rows.forEach(row -> soldes.add(mapRowToSoldePrepaye(row)));
        return soldes;
      });
  }

  // NEW METHOD - Update date_abonnement when recharging
  @Override
  public Future<Void> updateDateAbonnement(Long compagnieId) {
    String sql = """
            UPDATE solde_prepaye
            SET date_abonnement = $1
            WHERE compagnie_id = $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(LocalDate.now(), compagnieId))
      .map(rows -> null);
  }

  // NEW METHOD - Check if company already has a balance record
  @Override
  public Future<Boolean> existsByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT EXISTS(
                SELECT 1 FROM solde_prepaye WHERE compagnie_id = $1
            ) as exists
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> rows.iterator().next().getBoolean("exists"));
  }

  private SoldePrepaye mapRowToSoldePrepaye(Row row) {
    SoldePrepaye solde = new SoldePrepaye();
    solde.setId(row.getLong("id"));
    solde.setCompagnieId(row.getLong("compagnie_id"));

    // Handle nullable fields
    try {
      Long factureId = row.getLong("facture_id");
      if (factureId != null && factureId > 0) {
        solde.setFactureId(factureId);
      }
    } catch (Exception e) {
      // Column might not exist or be null
    }

    try {
      Long offreId = row.getLong("offre_id");
      if (offreId != null && offreId > 0) {
        solde.setOffreId(offreId);
      }
    } catch (Exception e) {
      // Column might not exist or be null
    }

    solde.setDateAbonnement(row.getLocalDate("date_abonnement"));
    solde.setSolde(row.getBigDecimal("solde"));

    // Handle nullable Integer
    try {
      Integer soldeAttestation = row.getInteger("solde_attestation");
      solde.setSoldeAttestation(soldeAttestation);
    } catch (Exception e) {
      solde.setSoldeAttestation(0);
    }

    solde.setType(row.getString("type"));
    solde.setStatut(row.getString("statut"));
    solde.setDevise(row.getString("devise"));

    // Map company name if available
    try {
      String raisonSocial = row.getString("raison_social");
      if (raisonSocial != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(solde.getCompagnieId());
        compagnie.setRaison_social(raisonSocial);
        solde.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column doesn't exist in this query
    }

    return solde;
  }
}
