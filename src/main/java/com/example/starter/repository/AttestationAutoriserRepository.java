package com.example.starter.repository;

import com.example.starter.model.AttestationAutoriser;
import io.vertx.core.Future;

import java.util.List;

public interface AttestationAutoriserRepository {

  Future<Void> save(Long compagnieId, Long typeAttestationId, Boolean flag);
  Future<Void> update(Long compagnieId, Long typeAttestationId, Boolean flag);
  Future<Void> delete(Long compagnieId, Long typeAttestationId);
  Future<AttestationAutoriser> find(Long compagnieId, Long typeAttestationId);
  Future<List<AttestationAutoriser>> findByCompagnieId(Long compagnieId);
  Future<List<AttestationAutoriser>> findByTypeAttestationId(Long typeAttestationId);
  Future<List<AttestationAutoriser>> findAll();
  Future<Boolean> isAuthorized(Long compagnieId, Long typeAttestationId);

}
