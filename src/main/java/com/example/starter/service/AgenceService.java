package com.example.starter.service;

import com.example.starter.dto.CreateAgenceRequest;
import com.example.starter.dto.UpdateAgenceRequest;
import com.example.starter.model.Agence;
import io.vertx.core.Future;

import java.util.List;

public interface AgenceService {

  Future<Agence> create(CreateAgenceRequest request);
  Future<Agence> findById(Long id);
  Future<List<Agence>> findAll();
  Future<List<Agence>> findByCompagnie(Long compagnieId);
  Future<List<Agence>> findByStatus(String status);
  Future<Agence> update(Long id, UpdateAgenceRequest request);
  Future<Boolean> closeAgence(Long id);
  Future<Boolean> delete(Long id);

}
