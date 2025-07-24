package com.example.starter.repository;

import com.example.starter.model.TypeAbonnement;
import io.vertx.core.Future;
import java.util.List;

public interface TypeAbonnementRepository {
  Future<Long> save(TypeAbonnement typeAbonnement);
  Future<TypeAbonnement> findById(Long id);
  Future<TypeAbonnement> findByLibelle(String libelle);
  Future<List<TypeAbonnement>> findAll();
  Future<List<TypeAbonnement>> findByCategorie(String categorie);
  Future<List<TypeAbonnement>> findActifs();
  Future<Void> update(Long id, TypeAbonnement typeAbonnement);
  Future<Void> delete(Long id);
  Future<Boolean> isUsedInAbonnements(Long id);
}
