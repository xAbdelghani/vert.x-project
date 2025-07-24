package com.example.starter.repository;


import com.example.starter.model.Transaction;
import io.vertx.core.Future;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository {
  Future<Long> save(Transaction transaction);
  Future<List<Transaction>> findByCompagnieId(Long compagnieId);
  Future<List<Transaction>> findByCompagnieIdWithLimit(Long compagnieId, int limit);
  Future<List<Transaction>> findByDateRange(LocalDateTime start, LocalDateTime end);
}
