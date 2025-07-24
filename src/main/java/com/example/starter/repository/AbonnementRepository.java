package com.example.starter.repository;

import com.example.starter.model.Abonnement;
import io.vertx.core.Future;

import java.util.List;

public interface AbonnementRepository {

  Future<Long> save(Abonnement abonnement);

  Future<Abonnement> findById(Long id);

  Future<Abonnement> findActiveByCompagnieId(Long compagnieId);

  Future<List<Abonnement>> findByCompagnieId(Long compagnieId);

  Future<Void> update(Long id, Abonnement abonnement);

  Future<List<Abonnement>> findAll();  // Changed from Future<Object>
  Future<List<Abonnement>> findByType(String category);

}



