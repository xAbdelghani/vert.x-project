package com.example.starter.service.impl;

import com.example.starter.dto.CreateAgenceRequest;
import com.example.starter.dto.UpdateAgenceRequest;
import com.example.starter.model.Agence;
import com.example.starter.repository.AgenceRepository;
import com.example.starter.repository.CompagnieRepository;
import com.example.starter.service.AgenceService;
import io.vertx.core.Future;
import jakarta.ws.rs.NotFoundException;
import java.util.List;

public class AgenceServiceImpl implements AgenceService {

  private final AgenceRepository agenceRepository;

  private final CompagnieRepository compagnieRepository;

  public AgenceServiceImpl(AgenceRepository agenceRepository,
                           CompagnieRepository compagnieRepository) {
    this.agenceRepository = agenceRepository;
    this.compagnieRepository = compagnieRepository;
  }

  @Override
  public Future<Agence> create(CreateAgenceRequest request) {
    // Validate compagnie exists
    return compagnieRepository.findById(request.getCompagnieId())
      .compose(compagnie -> {
        System.out.println("hello world");
        if (compagnie == null) {
          System.out.println(compagnie.getRaison_social());
          return Future.failedFuture(
            new Exception("Compagnie not found with id: " + request.getCompagnieId())
          );
        }
        Agence agence = new Agence();
        agence.setNoma(request.getNoma());
        agence.setAdressea(request.getAdressea());
        agence.setTelephonea(request.getTelephonea());
        agence.setDateDebuta(request.getDateDebuta());
        agence.setCompagnieId(request.getCompagnieId());

        return agenceRepository.save(agence);
      });
  }

  @Override
  public Future<Agence> findById(Long id) {
    return agenceRepository.findById(id)
      .map(agence -> {
        if (agence == null) {
          throw new NotFoundException("Agence not found with id: " + id);
        }
        return agence;
      });
  }

  @Override
  public Future<List<Agence>> findAll() {
    return agenceRepository.findAll();
  }

  @Override
  public Future<List<Agence>> findByCompagnie(Long compagnieId) {
    return agenceRepository.findByCompagnieId(compagnieId);
  }

  @Override
  public Future<List<Agence>> findByStatus(String status) {
    return agenceRepository.findByStatus(status);
  }

  @Override
  public Future<Agence> update(Long id, UpdateAgenceRequest request) {
    return agenceRepository.findById(id)
      .compose(agence -> {
        if (agence == null) {
          return Future.failedFuture(
            new NotFoundException("Agence not found with id: " + id)
          );
        }
        // Update fields
        agence.setNoma(request.getNoma());
        agence.setAdressea(request.getAdressea());
        agence.setTelephonea(request.getTelephonea());
        agence.setDateDebuta(request.getDateDebuta());
        agence.setDateFina(request.getDateFina());
        agence.setCompagnieId(request.getCompagnieId());

        return agenceRepository.update(agence);
      });
  }

  @Override
  public Future<Boolean> closeAgence(Long id) {
    return agenceRepository.updateDateFinToToday(id);
  }

  @Override
  public Future<Boolean> delete(Long id) {
    return agenceRepository.deleteById(id);
  }
}
