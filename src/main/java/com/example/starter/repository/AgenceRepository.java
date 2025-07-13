package com.example.starter.repository;

import com.example.starter.model.Agence;
import io.vertx.core.Future;

import java.util.List;

public interface AgenceRepository {

  Future<Agence> save(Agence agence);
  Future<Agence> findById(Long id);
  Future<List<Agence>> findAll();
  Future<List<Agence>> findByCompagnieId(Long compagnieId);
  Future<List<Agence>> findByStatus(String status);
  Future<Agence> update(Agence agence);
  Future<Boolean> updateDateFinToToday(Long id);
  Future<Boolean> deleteById(Long id);

}
