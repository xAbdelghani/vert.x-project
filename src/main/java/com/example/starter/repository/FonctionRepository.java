package com.example.starter.repository;

import com.example.starter.model.Fonction;
import io.vertx.core.Future;

import java.util.List;

public interface FonctionRepository {

  Future<Long> save(String qualite);
  Future<Fonction> findById(Long id);
  Future<List<Fonction>> findAll();
  Future<Void> update(Long id, String qualite);
  Future<Void> delete(Long id);
  Future<Boolean> exists(Long id);

}

