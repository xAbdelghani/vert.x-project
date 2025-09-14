package com.example.starter.service.impl;

import com.example.starter.service.DashboardService;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DashboardServiceImpl implements DashboardService {

  private final PgPool pgPool;

  public DashboardServiceImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<JsonObject> getDashboardStats() {
    return CompositeFuture.all(
      getCompanyStats(),
      getAttestationStats(),
      getFinancialStats(),
      getTodayActivity()
    ).map(result -> {
      JsonObject stats = new JsonObject();
      stats.put("companies", result.resultAt(0));
      stats.put("attestations", result.resultAt(1));
      stats.put("financial", result.resultAt(2));
      stats.put("todayActivity", result.resultAt(3));
      stats.put("timestamp", LocalDate.now().toString());
      return stats;
    });
  }

  @Override
  public Future<JsonObject> getCompanyStats() {
    String totalCompaniesSql = "SELECT COUNT(*) as total FROM compagnies ";

    String prepayeCompaniesSql = """
      SELECT COUNT(DISTINCT c.id) as total
      FROM compagnies c
      JOIN solde_prepaye sp ON c.id = sp.compagnie_id
      WHERE sp.type = 'PREPAYE' AND sp.statut = 'ACTIF'
    """;

    String avanceSubscriptionsSql = """
      SELECT COUNT(*) as total
      FROM abonnement a
      WHERE a.type = 'AVANCE' AND a.statut = 'ACTIF'
    """;

    String cautionSubscriptionsSql = """
      SELECT COUNT(*) as total
      FROM abonnement a
      WHERE a.type = 'CAUTION' AND a.statut = 'ACTIF'
    """;

    return CompositeFuture.all(
      pgPool.query(totalCompaniesSql).execute(),
      pgPool.query(prepayeCompaniesSql).execute(),
      pgPool.query(avanceSubscriptionsSql).execute(),
      pgPool.query(cautionSubscriptionsSql).execute()
    ).map(results -> {
      RowSet<Row> rs0 = (RowSet<Row>) results.resultAt(0);
      RowSet<Row> rs1 = (RowSet<Row>) results.resultAt(1);
      RowSet<Row> rs2 = (RowSet<Row>) results.resultAt(2);
      RowSet<Row> rs3 = (RowSet<Row>) results.resultAt(3);

      long totalCompanies = rs0.iterator().next().getLong("total");
      long prepayeCompanies = rs1.iterator().next().getLong("total");
      long avanceSubscriptions = rs2.iterator().next().getLong("total");
      long cautionSubscriptions = rs3.iterator().next().getLong("total");

      return new JsonObject()
        .put("totalCompanies", totalCompanies)
        .put("prepayeCompanies", prepayeCompanies)
        .put("avanceSubscriptions", avanceSubscriptions)
        .put("cautionSubscriptions", cautionSubscriptions)
        .put("totalSubscriptions", avanceSubscriptions + cautionSubscriptions);
    });
  }

  @Override
  public Future<JsonObject> getAttestationStats() {
    String totalTypesSql = "SELECT COUNT(*) as total FROM type_attestation";

    String totalAttestationsSql = """
      SELECT COUNT(*) as total
      FROM attestation a
      JOIN historique_statut_att h ON a.id = h.attestation_id
      WHERE h.statut = 'EN_COURS'
    """;

    String expiringThisWeekSql = """
      SELECT COUNT(*) as total
      FROM attestation a
      JOIN historique_statut_att h ON a.id = h.attestation_id
      WHERE h.statut = 'EN_COURS'
      AND a.date_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '7 days'
    """;

    return CompositeFuture.all(
      pgPool.query(totalTypesSql).execute(),
      pgPool.query(totalAttestationsSql).execute(),
      pgPool.query(expiringThisWeekSql).execute()
    ).map(results -> {
      RowSet<Row> rs0 = (RowSet<Row>) results.resultAt(0);
      RowSet<Row> rs1 = (RowSet<Row>) results.resultAt(1);
      RowSet<Row> rs2 = (RowSet<Row>) results.resultAt(2);

      long totalTypes = rs0.iterator().next().getLong("total");
      long totalAttestations = rs1.iterator().next().getLong("total");
      long expiringThisWeek = rs2.iterator().next().getLong("total");

      return new JsonObject()
        .put("totalTypes", totalTypes)
        .put("totalActiveAttestations", totalAttestations)
        .put("expiringThisWeek", expiringThisWeek);
    });
  }

  @Override
  public Future<JsonObject> getFinancialStats() {
    String totalPrepaidSql = """
      SELECT COALESCE(SUM(sp.solde), 0) as total, sp.devise
      FROM solde_prepaye sp
      WHERE sp.type = 'PREPAYE' AND sp.statut = 'ACTIF'
      GROUP BY sp.devise
    """;

    String monthlyRevenueSql = """
      SELECT COALESCE(SUM(t.montant), 0) as total,
             COUNT(*) as transaction_count
      FROM transaction t
      WHERE t.type IN ('ATTESTATION_GENERATION', 'CREDIT_USAGE', 'DEPOT_USAGE')
      AND date_trunc('month', t.date_transaction) = date_trunc('month', CURRENT_DATE)
    """;

    String lowBalanceSql = """
      SELECT COUNT(*) as total
      FROM solde_prepaye sp
      WHERE sp.type = 'PREPAYE'
      AND sp.statut = 'ACTIF'
      AND sp.solde < 100
    """;

    return CompositeFuture.all(
      pgPool.query(totalPrepaidSql).execute(),
      pgPool.query(monthlyRevenueSql).execute(),
      pgPool.query(lowBalanceSql).execute()
    ).map(results -> {
      JsonObject prepaidByCurrency = new JsonObject();
      BigDecimal totalPrepaidMAD = BigDecimal.ZERO;

      RowSet<Row> prepaidRows = (RowSet<Row>) results.resultAt(0);
      for (Row row : prepaidRows) {
        String devise = row.getString("devise");
        BigDecimal amount = row.getBigDecimal("total");
        prepaidByCurrency.put(devise, amount.doubleValue());

        if ("MAD".equals(devise)) {
          totalPrepaidMAD = amount;
        }
      }

      Row revenueRow = ((RowSet<Row>) results.resultAt(1)).iterator().next();
      BigDecimal monthlyRevenue = revenueRow.getBigDecimal("total");
      long transactionCount = revenueRow.getLong("transaction_count");

      long lowBalanceCompanies = ((RowSet<Row>) results.resultAt(2)).iterator().next().getLong("total");

      return new JsonObject()
        .put("totalPrepaidBalance", totalPrepaidMAD.doubleValue())
        .put("prepaidByCurrency", prepaidByCurrency)
        .put("monthlyRevenue", monthlyRevenue.doubleValue())
        .put("monthlyTransactions", transactionCount)
        .put("lowBalanceCompanies", lowBalanceCompanies);
    });
  }

  @Override
  public Future<JsonObject> getTodayActivity() {
    LocalDate today = LocalDate.now();

    String todayAttestationsSql = """
      SELECT COUNT(*) as count,
             COALESCE(SUM(ta.prix_unitaire), 0) as revenue
      FROM attestation a
      JOIN type_attestation ta ON a.typeattestation_id = ta.id
      WHERE DATE(a.date_generation) = $1
    """;

    String todayVehiclesSql = """
      SELECT COUNT(DISTINCT a.vehicule_id) as count
      FROM attestation a
      WHERE DATE(a.date_generation) = $1
    """;

    String todayTransactionsSql = """
      SELECT type, COUNT(*) as count, COALESCE(SUM(montant), 0) as total
      FROM transaction
      WHERE DATE(date_transaction) = $1
      GROUP BY type
    """;

    return CompositeFuture.all(
      pgPool.preparedQuery(todayAttestationsSql).execute(Tuple.of(today)),
      pgPool.preparedQuery(todayVehiclesSql).execute(Tuple.of(today)),
      pgPool.preparedQuery(todayTransactionsSql).execute(Tuple.of(today))
    ).map(results -> {
      Row attestationRow = ((RowSet<Row>) results.resultAt(0)).iterator().next();
      long todayAttestations = attestationRow.getLong("count");
      BigDecimal todayRevenue = attestationRow.getBigDecimal("revenue");

      long todayVehicles = ((RowSet<Row>) results.resultAt(1)).iterator().next().getLong("count");

      JsonArray transactionBreakdown = new JsonArray();
      RowSet<Row> transactionRows = (RowSet<Row>) results.resultAt(2);
      for (Row row : transactionRows) {
        transactionBreakdown.add(new JsonObject()
          .put("type", row.getString("type"))
          .put("count", row.getLong("count"))
          .put("total", row.getBigDecimal("total").doubleValue()));
      }

      return new JsonObject()
        .put("attestationsToday", todayAttestations)
        .put("revenueToday", todayRevenue.doubleValue())
        .put("vehiclesToday", todayVehicles)
        .put("transactionBreakdown", transactionBreakdown)
        .put("date", today.toString());
    });
  }


  @Override
  public Future<JsonObject> getRecentAttestations(int limit) {
    String sql = """
      SELECT a.id, a.référence_flotte as reference,
             a.date_generation, a.date_fin,
             c.raison_social as company_name,
             v.immatriculation,
             ta.libelle as type_name,
             ta.prix_unitaire as amount,
             ta.devise,
             h.statut,
             sp.solde as balance_after
      FROM attestation a
      JOIN compagnies c ON a.compagnie_id = c.id
      JOIN vehicule v ON a.vehicule_id = v.id
      JOIN type_attestation ta ON a.typeattestation_id = ta.id
      LEFT JOIN historique_statut_att h ON a.id = h.attestation_id
      LEFT JOIN solde_prepaye sp ON c.id = sp.compagnie_id AND sp.type = 'PREPAYE'
      WHERE h.id = (
        SELECT MAX(id) FROM historique_statut_att WHERE attestation_id = a.id
      )
      ORDER BY a.date_generation DESC, a.id DESC
      LIMIT $1
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(limit))
      .map(rows -> {
        JsonArray attestations = new JsonArray();
        for (Row row : rows) {
          attestations.add(new JsonObject()
            .put("id", row.getLong("id"))
            .put("reference", row.getString("reference"))
            .put("dateGeneration", row.getLocalDate("date_generation").toString())
            .put("dateFin", row.getLocalDate("date_fin").toString())
            .put("company", row.getString("company_name"))
            .put("vehicle", row.getString("immatriculation"))
            .put("type", row.getString("type_name"))
            .put("amount", row.getBigDecimal("amount").doubleValue())
            .put("devise", row.getString("devise"))
            .put("status", row.getString("statut"))
            .put("balanceAfter", row.getBigDecimal("balance_after") != null ?
              row.getBigDecimal("balance_after").doubleValue() : null));
        }

        return new JsonObject()
          .put("attestations", attestations)
          .put("count", attestations.size());
      });
  }

  @Override
  public Future<JsonObject> getLowBalanceCompanies(double threshold) {
    String sql = """
      SELECT c.id, c.raison_social, c.email, c.telephone,
             sp.solde, sp.devise,
             COUNT(DISTINCT a.id) as attestation_count
      FROM compagnies c
      JOIN solde_prepaye sp ON c.id = sp.compagnie_id
      LEFT JOIN attestation a ON c.id = a.compagnie_id
      WHERE sp.type = 'PREPAYE'
      AND sp.statut = 'ACTIF'
      AND sp.solde < $1
      GROUP BY c.id, c.raison_social, c.email, c.telephone, sp.solde, sp.devise
      ORDER BY sp.solde ASC
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(threshold))
      .map(rows -> {
        JsonArray companies = new JsonArray();
        for (Row row : rows) {
          companies.add(new JsonObject()
            .put("id", row.getLong("id"))
            .put("name", row.getString("raison_social"))
            .put("email", row.getString("email"))
            .put("phone", row.getString("telephone"))
            .put("balance", row.getBigDecimal("solde").doubleValue())
            .put("devise", row.getString("devise"))
            .put("totalAttestations", row.getLong("attestation_count")));
        }

        return new JsonObject()
          .put("companies", companies)
          .put("count", companies.size())
          .put("threshold", threshold);
      });
  }

  @Override
  public Future<JsonObject> getExpiringAttestations(int days) {
    String sql = """
      SELECT a.id, a.référence_flotte as reference,
             a.date_fin,
             c.raison_social as company_name,
             c.email as company_email,
             v.immatriculation,
             ta.libelle as type_name,
             EXTRACT(DAY FROM a.date_fin - CURRENT_DATE) as days_remaining
      FROM attestation a
      JOIN compagnies c ON a.compagnie_id = c.id
      JOIN vehicule v ON a.vehicule_id = v.id
      JOIN type_attestation ta ON a.typeattestation_id = ta.id
      JOIN historique_statut_att h ON a.id = h.attestation_id
      WHERE h.id = (
        SELECT MAX(id) FROM historique_statut_att WHERE attestation_id = a.id
      )
      AND h.statut = 'EN_COURS'
      AND a.date_fin BETWEEN CURRENT_DATE AND CURRENT_DATE + INTERVAL '%d days'
      ORDER BY a.date_fin ASC
    """.formatted(days);

    return pgPool.query(sql)
      .execute()
      .map(rows -> {
        JsonArray attestations = new JsonArray();
        for (Row row : rows) {
          attestations.add(new JsonObject()
            .put("id", row.getLong("id"))
            .put("reference", row.getString("reference"))
            .put("dateFin", row.getLocalDate("date_fin").toString())
            .put("company", row.getString("company_name"))
            .put("companyEmail", row.getString("company_email"))
            .put("vehicle", row.getString("immatriculation"))
            .put("type", row.getString("type_name"))
            .put("daysRemaining", row.getInteger("days_remaining")));
        }

        return new JsonObject()
          .put("attestations", attestations)
          .put("count", attestations.size())
          .put("daysAhead", days);
      });
  }
}
