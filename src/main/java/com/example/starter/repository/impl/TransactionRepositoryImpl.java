package com.example.starter.repository.impl;

import com.example.starter.model.Transaction;
import com.example.starter.repository.TransactionRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepositoryImpl implements TransactionRepository {

  private final PgPool pgPool;

  public TransactionRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Transaction transaction) {
    String sql = """
            INSERT INTO transaction (
                compagnie_id, type, montant, solde_avant, solde_apres,
                description, date_transaction, user_id, reference
            ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
            RETURNING id
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        transaction.getCompagnieId(),
        transaction.getType(),
        transaction.getMontant(),
        transaction.getSoldeAvant(),
        transaction.getSoldeApres(),
        transaction.getDescription(),
        LocalDateTime.now(),
        transaction.getUserId(),
        transaction.getReference()
      ))
      .map(rows -> rows.iterator().next().getLong("id"));
  }

  @Override
  public Future<List<Transaction>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT * FROM transaction
            WHERE compagnie_id = $1
            ORDER BY date_transaction DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<Transaction> transactions = new ArrayList<>();
        rows.forEach(row -> transactions.add(mapRowToTransaction(row)));
        return transactions;
      });
  }

  @Override
  public Future<List<Transaction>> findByCompagnieIdWithLimit(Long compagnieId, int limit) {
    String sql = """
            SELECT * FROM transaction
            WHERE compagnie_id = $1
            ORDER BY date_transaction DESC
            LIMIT $2
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId, limit))
      .map(rows -> {
        List<Transaction> transactions = new ArrayList<>();
        rows.forEach(row -> transactions.add(mapRowToTransaction(row)));
        return transactions;
      });
  }

  @Override
  public Future<List<Transaction>> findByDateRange(LocalDateTime start, LocalDateTime end) {
    String sql = """
            SELECT t.*, c.raison_social
            FROM transaction t
            LEFT JOIN compagnies c ON t.compagnie_id = c.id
            WHERE t.date_transaction BETWEEN $1 AND $2
            ORDER BY t.date_transaction DESC
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(start, end))
      .map(rows -> {
        List<Transaction> transactions = new ArrayList<>();
        rows.forEach(row -> transactions.add(mapRowToTransaction(row)));
        return transactions;
      });
  }

  private Transaction mapRowToTransaction(Row row) {
    Transaction transaction = new Transaction();
    transaction.setId(row.getLong("id"));
    transaction.setCompagnieId(row.getLong("compagnie_id"));
    transaction.setType(row.getString("type"));
    transaction.setMontant(row.getBigDecimal("montant"));
    transaction.setSoldeAvant(row.getBigDecimal("solde_avant"));
    transaction.setSoldeApres(row.getBigDecimal("solde_apres"));
    transaction.setDescription(row.getString("description"));
    transaction.setDateTransaction(row.getLocalDateTime("date_transaction"));
    transaction.setUserId(row.getLong("user_id"));
    transaction.setReference(row.getString("reference"));
    return transaction;
  }
}
