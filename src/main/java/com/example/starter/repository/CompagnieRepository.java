package com.example.starter.repository;

import com.example.starter.model.Compagnie;
import io.vertx.core.Future;
import java.util.List;

public interface CompagnieRepository {

  Future<Long> save(String raisonSocial, String email, String telephone, String adresse);

  Future<Void> createAccountForCompagnie(Long id, String nom, String password);

  Future<Compagnie> findById(Long id);

  Future<Compagnie> findByEmail(String email);

  Future<List<Compagnie>> findAll();
}
