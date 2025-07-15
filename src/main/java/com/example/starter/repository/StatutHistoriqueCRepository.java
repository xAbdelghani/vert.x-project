package com.example.starter.repository;

import com.example.starter.model.StatutHistoriqueC;
import io.vertx.core.Future;

import java.util.List;

public interface StatutHistoriqueCRepository {
  Future<Long> save(StatutHistoriqueC historique);
  Future<List<StatutHistoriqueC>> findByRelationId(Long relationId);
  Future<StatutHistoriqueC> getCurrentStatus(Long relationId);
  Future<Void> deleteByRelationId(Long relationId);

}
