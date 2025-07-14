package com.example.starter.repository;

import com.example.starter.model.StatutC;
import io.vertx.core.Future;

import java.util.List;

public interface StatutCRepository {
  Future<Long> save(String libelle);
  Future<StatutC> findById(Long id);
  Future<StatutC> findByLibelle(String libelle);
  Future<List<StatutC>> findAll();
}
