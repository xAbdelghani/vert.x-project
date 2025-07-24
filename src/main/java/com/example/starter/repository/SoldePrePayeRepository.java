package com.example.starter.repository;

import com.example.starter.model.SoldePrepaye;
import io.vertx.core.Future;

import java.math.BigDecimal;
import java.util.List;

public interface SoldePrePayeRepository {

  Future<Long> save(SoldePrepaye soldePrepaye);
  Future<SoldePrepaye> findByCompagnieId(Long compagnieId);
  Future<List<SoldePrepaye>> findAll();

  // NEW METHOD - Find all by type (PREPAYE only)
  Future<List<SoldePrepaye>> findAllByType(String type);

  Future<Void> updateSolde(Long compagnieId, BigDecimal newSolde);
  Future<Void> addToSolde(Long compagnieId, BigDecimal amount);
  Future<Void> deductFromSolde(Long compagnieId, BigDecimal amount);
  Future<List<SoldePrepaye>> findLowBalances(BigDecimal threshold);

  // NEW METHOD - Find low balances by type
  Future<List<SoldePrepaye>> findLowBalancesByType(String type, BigDecimal threshold);

  // NEW METHOD - Update date_abonnement when recharging
  Future<Void> updateDateAbonnement(Long compagnieId);

  // NEW METHOD - Check if company already has a balance record
  Future<Boolean> existsByCompagnieId(Long compagnieId);

}
