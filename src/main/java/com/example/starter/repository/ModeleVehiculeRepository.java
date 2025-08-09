package com.example.starter.repository;

import com.example.starter.model.ModeleVehicule;
import io.vertx.core.Future;

import java.util.List;

public interface ModeleVehiculeRepository {

  Future<Long> save(ModeleVehicule modele);
  Future<ModeleVehicule> findById(Long id);
  Future<List<ModeleVehicule>> findAll();
  Future<List<ModeleVehicule>> findByMarque(String marque);
  Future<List<ModeleVehicule>> findByType(String type);
  Future<List<String>> findAllMarques();
  Future<List<String>> findAllTypes();
  Future<Void> update(Long id, ModeleVehicule modele);
  Future<Void> delete(Long id);
  Future<Boolean> isUsedInVehicles(Long id);
  Future<List<ModeleVehicule>> search(String marque, String type, String carburant);

}
