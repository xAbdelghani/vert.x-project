package com.example.starter.repository;

import com.example.starter.model.Pointvente;
import io.vertx.core.Future;

import java.util.List;

public interface PointventeRepository {
  Future<Long> save(Pointvente pointvente);
  Future<Pointvente> findById(Long id);
  Future<List<Pointvente>> findAll();
  Future<Void> update(Long id, Pointvente pointvente);
  Future<Void> delete(Long id);
  Future<Boolean> exists(Long id);
}
