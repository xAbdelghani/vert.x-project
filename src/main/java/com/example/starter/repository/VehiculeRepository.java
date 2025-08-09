package com.example.starter.repository;

import com.example.starter.model.Vehicule;
import io.vertx.core.Future;

import java.util.List;

public interface VehiculeRepository {

  Future<Long> save(Vehicule vehicule);
  Future<Vehicule> findById(Long id);
  Future<Vehicule> findByImmatriculation(String immatriculation);
  Future<List<Vehicule>> findAll();
  Future<List<Vehicule>> findByCompagnieId(Long compagnieId);
  Future<List<Vehicule>> findByModelId(Long modelId);
  Future<Void> update(Long id, Vehicule vehicule);
  Future<Void> delete(Long id);
  Future<Boolean> hasActiveAttestations(Long id);
  Future<List<Vehicule>> search(String immatriculation, Long modelId, Long compagnieId);

}
