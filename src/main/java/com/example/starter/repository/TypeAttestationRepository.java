package com.example.starter.repository;

import com.example.starter.model.TypeAttestation;
import io.vertx.core.Future;

import java.util.List;

public interface TypeAttestationRepository {

  Future<Long> save(TypeAttestation typeAttestation);
  Future<TypeAttestation> findById(Long id);
  Future<TypeAttestation> findByLibelle(String libelle);
  Future<List<TypeAttestation>> findAll();
  Future<Void> update(Long id, TypeAttestation typeAttestation);
  Future<Void> delete(Long id);
  Future<Boolean> isUsedInAttestations(Long id);

}

