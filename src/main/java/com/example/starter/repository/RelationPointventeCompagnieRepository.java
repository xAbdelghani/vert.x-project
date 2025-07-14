package com.example.starter.repository;

import com.example.starter.model.RelationPointventeCompagnie;
import io.vertx.core.Future;

import java.util.List;

public interface RelationPointventeCompagnieRepository {
  Future<Long> save(RelationPointventeCompagnie relation);
  Future<RelationPointventeCompagnie> findById(Long id);
  Future<List<RelationPointventeCompagnie>> findByPointventeId(Long pointventeId);
  Future<List<RelationPointventeCompagnie>> findByCompagnieId(Long compagnieId);
  Future<RelationPointventeCompagnie> findByPointventeAndCompagnie(Long pointventeId, Long compagnieId);
  Future<Void> updateStatus(Long id, String status, String reason);
  Future<Boolean> exists(Long pointventeId, Long compagnieId);
}
