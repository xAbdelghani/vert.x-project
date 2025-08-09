package com.example.starter.service.impl;

import com.example.starter.model.ModeleVehicule;
import com.example.starter.model.Vehicule;
import com.example.starter.repository.ModeleVehiculeRepository;
import com.example.starter.repository.VehiculeRepository;
import com.example.starter.service.VehicleService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.time.LocalDate;
import java.util.regex.Pattern;

public class VehicleServiceImpl implements VehicleService {

  private final ModeleVehiculeRepository modelRepository;
  private final VehiculeRepository vehiculeRepository;

  // Moroccan plate patterns
  private static final Pattern MOROCCAN_PLATE_PATTERN = Pattern.compile(
    "^\\d{1,5}-[A-Z]-\\d{1,2}$|^\\d{1,5}-[\\u0600-\\u06FF]-\\d{1,2}$"
  );

  public VehicleServiceImpl(
    ModeleVehiculeRepository modelRepository,
    VehiculeRepository vehiculeRepository
  ) {
    this.modelRepository = modelRepository;
    this.vehiculeRepository = vehiculeRepository;
  }

  // VEHICLE MODEL METHODS

  @Override
  public Future<JsonObject> createModel(JsonObject data) {
    // Validate required fields
    String designation = data.getString("designation");
    String type = data.getString("type");
    String marque = data.getString("marque");

    if (designation == null || designation.trim().isEmpty()) {
      return Future.failedFuture("La désignation est requise");
    }

    if (type == null || type.trim().isEmpty()) {
      return Future.failedFuture("Le type est requis");
    }

    if (marque == null || marque.trim().isEmpty()) {
      return Future.failedFuture("La marque est requise");
    }

    ModeleVehicule modele = new ModeleVehicule();
    modele.setDesignation(designation);
    modele.setType(type.toUpperCase());
    modele.setMarque(marque.toUpperCase());
    modele.setAnnee(data.getInteger("annee"));
    modele.setPuissanceFiscale(data.getDouble("puissance_fiscale"));
    modele.setCarburant(data.getString("carburant"));

    return modelRepository.save(modele)
      .map(id -> new JsonObject()
        .put("id", id)
        .put("message", "Modèle de véhicule créé avec succès"));
  }

  @Override
  public Future<JsonObject> getModel(Long id) {
    return modelRepository.findById(id)
      .map(modele -> {
        if (modele == null) {
          throw new RuntimeException("Modèle non trouvé");
        }
        return modelToJson(modele);
      });
  }

  @Override
  public Future<JsonArray> getAllModels() {
    return modelRepository.findAll()
      .map(models -> {
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(modelToJson(model)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getModelsByBrand(String marque) {
    return modelRepository.findByMarque(marque)
      .map(models -> {
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(modelToJson(model)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getModelsByType(String type) {
    return modelRepository.findByType(type)
      .map(models -> {
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(modelToJson(model)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getAllBrands() {
    return modelRepository.findAllMarques()
      .map(marques -> {
        JsonArray array = new JsonArray();
        marques.forEach(array::add);
        return array;
      });
  }

  @Override
  public Future<JsonArray> getAllTypes() {
    return modelRepository.findAllTypes()
      .map(types -> {
        JsonArray array = new JsonArray();
        types.forEach(array::add);
        return array;
      });
  }

  @Override
  public Future<JsonObject> updateModel(Long id, JsonObject data) {
    return modelRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Modèle non trouvé");
        }

        String designation = data.getString("designation");
        String type = data.getString("type");
        String marque = data.getString("marque");

        if (designation == null || designation.trim().isEmpty()) {
          return Future.failedFuture("La désignation est requise");
        }

        ModeleVehicule updated = new ModeleVehicule();
        updated.setDesignation(designation);
        updated.setType(type != null ? type.toUpperCase() : existing.getType());
        updated.setMarque(marque != null ? marque.toUpperCase() : existing.getMarque());
        updated.setAnnee(data.getInteger("annee", existing.getAnnee()));
        updated.setPuissanceFiscale(data.getDouble("puissance_fiscale", existing.getPuissanceFiscale()));
        updated.setCarburant(data.getString("carburant", existing.getCarburant()));

        return modelRepository.update(id, updated)
          .map(v -> new JsonObject()
            .put("message", "Modèle mis à jour avec succès"));
      });
  }

  @Override
  public Future<Void> deleteModel(Long id) {
    return modelRepository.isUsedInVehicles(id)
      .compose(isUsed -> {
        if (isUsed) {
          return Future.failedFuture("Ce modèle est utilisé par des véhicules");
        }
        return modelRepository.delete(id);
      });
  }

  @Override
  public Future<JsonArray> searchModels(String marque, String type, String carburant) {
    return modelRepository.search(marque, type, carburant)
      .map(models -> {
        JsonArray array = new JsonArray();
        models.forEach(model -> array.add(modelToJson(model)));
        return array;
      });
  }

  // VEHICLE METHODS

  @Override
  public Future<JsonObject> registerVehicle(JsonObject data) {
    String immatriculation = data.getString("immatriculation");
    Long modelId = data.getLong("model_id");
    Long compagnieId = data.getLong("compagnie_id");
    LocalDate dateImmatriculation = data.getString("date_immatriculation") != null
      ? LocalDate.parse(data.getString("date_immatriculation"))
      : LocalDate.now();

    if (immatriculation == null || immatriculation.trim().isEmpty()) {
      return Future.failedFuture("L'immatriculation est requise");
    }

    if (modelId == null) {
      return Future.failedFuture("Le modèle est requis");
    }

    if (compagnieId == null) {
      return Future.failedFuture("La compagnie est requise");
    }

    // Normalize and validate plate
    immatriculation = immatriculation.toUpperCase().trim();

    final String finalImmatriculation = immatriculation;

    return isValidMoroccanPlate(finalImmatriculation)
      .compose(isValid -> {
        if (!isValid) {
          return Future.failedFuture("Format d'immatriculation invalide (ex: 12345-A-11)");
        }

        // Check if plate already exists
        return vehiculeRepository.findByImmatriculation(finalImmatriculation);
      })
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture("Cette immatriculation existe déjà");
        }

        // Check if model exists
        return modelRepository.findById(modelId);
      })
      .compose(model -> {
        if (model == null) {
          return Future.failedFuture("Modèle non trouvé");
        }

        Vehicule vehicule = new Vehicule();
        vehicule.setImmatriculation(finalImmatriculation);
        vehicule.setDateImmatriculation(dateImmatriculation);
        vehicule.setModelId(modelId);
        vehicule.setCompagnieId(compagnieId);

        return vehiculeRepository.save(vehicule)
          .map(id -> new JsonObject()
            .put("id", id)
            .put("message", "Véhicule enregistré avec succès"));
      });
  }

  @Override
  public Future<JsonObject> getVehicle(Long id) {
    return vehiculeRepository.findById(id)
      .map(vehicule -> {
        if (vehicule == null) {
          throw new RuntimeException("Véhicule non trouvé");
        }
        return vehiculeToJson(vehicule);
      });
  }

  @Override
  public Future<JsonObject> getVehicleByPlate(String immatriculation) {
    return vehiculeRepository.findByImmatriculation(immatriculation)
      .map(vehicule -> {
        if (vehicule == null) {
          throw new RuntimeException("Véhicule non trouvé");
        }
        return vehiculeToJson(vehicule);
      });
  }

  @Override
  public Future<JsonArray> getAllVehicles() {
    return vehiculeRepository.findAll()
      .map(vehicles -> {
        JsonArray array = new JsonArray();
        vehicles.forEach(vehicle -> array.add(vehiculeToJson(vehicle)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getCompanyVehicles(Long compagnieId) {
    return vehiculeRepository.findByCompagnieId(compagnieId)
      .map(vehicles -> {
        JsonArray array = new JsonArray();
        vehicles.forEach(vehicle -> array.add(vehiculeToJson(vehicle)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getVehiclesByModel(Long modelId) {
    return vehiculeRepository.findByModelId(modelId)
      .map(vehicles -> {
        JsonArray array = new JsonArray();
        vehicles.forEach(vehicle -> array.add(vehiculeToJson(vehicle)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> updateVehicle(Long id, JsonObject data) {
    return vehiculeRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Véhicule non trouvé");
        }

        String immatriculation = data.getString("immatriculation");
        Long modelId = data.getLong("model_id");

        if (immatriculation != null) {
          immatriculation = immatriculation.toUpperCase().trim();
          final String finalImmatriculation = immatriculation;

          return isValidMoroccanPlate(finalImmatriculation)
            .compose(isValid -> {
              if (!isValid) {
                return Future.failedFuture("Format d'immatriculation invalide");
              }

              // Check if new plate conflicts
              if (!finalImmatriculation.equals(existing.getImmatriculation())) {
                return vehiculeRepository.findByImmatriculation(finalImmatriculation)
                  .compose(conflict -> {
                    if (conflict != null) {
                      return Future.failedFuture("Cette immatriculation existe déjà");
                    }

                    return updateVehicleData(id, existing, data);
                  });
              }

              return updateVehicleData(id, existing, data);
            });
        } else {
          return updateVehicleData(id, existing, data);
        }
      });
  }

  private Future<JsonObject> updateVehicleData(Long id, Vehicule existing, JsonObject data) {
    Vehicule updated = new Vehicule();
    updated.setImmatriculation(
      data.getString("immatriculation") != null
        ? data.getString("immatriculation").toUpperCase().trim()
        : existing.getImmatriculation()
    );
    updated.setModelId(data.getLong("model_id", existing.getModelId()));
    updated.setDateImmatriculation(
      data.getString("date_immatriculation") != null
        ? LocalDate.parse(data.getString("date_immatriculation"))
        : existing.getDateImmatriculation()
    );

    return vehiculeRepository.update(id, updated)
      .map(v -> new JsonObject()
        .put("message", "Véhicule mis à jour avec succès"));
  }

  @Override
  public Future<Void> deleteVehicle(Long id) {
    return vehiculeRepository.hasActiveAttestations(id)
      .compose(hasActive -> {
        if (hasActive) {
          return Future.failedFuture("Ce véhicule a des attestations actives");
        }
        return vehiculeRepository.delete(id);
      });
  }

  @Override
  public Future<JsonArray> searchVehicles(String immatriculation, Long modelId, Long compagnieId) {
    return vehiculeRepository.search(immatriculation, modelId, compagnieId)
      .map(vehicles -> {
        JsonArray array = new JsonArray();
        vehicles.forEach(vehicle -> array.add(vehiculeToJson(vehicle)));
        return array;
      });
  }

  @Override
  public Future<Boolean> isValidMoroccanPlate(String immatriculation) {
    if (immatriculation == null) {
      return Future.succeededFuture(false);
    }

    // Remove spaces and normalize
    String normalized = immatriculation.toUpperCase().trim();

    // Check pattern: 12345-A-11 or with Arabic letter
    boolean isValid = MOROCCAN_PLATE_PATTERN.matcher(normalized).matches();

    return Future.succeededFuture(isValid);
  }

  private JsonObject modelToJson(ModeleVehicule model) {
    JsonObject json = new JsonObject()
      .put("id", model.getId())
      .put("designation", model.getDesignation())
      .put("type", model.getType())
      .put("marque", model.getMarque());

    if (model.getAnnee() != null) {
      json.put("annee", model.getAnnee());
    }
    if (model.getPuissanceFiscale() != null) {
      json.put("puissance_fiscale", model.getPuissanceFiscale());
    }
    if (model.getCarburant() != null) {
      json.put("carburant", model.getCarburant());
    }
    if (model.getVehicleCount() != null) {
      json.put("vehicle_count", model.getVehicleCount());
    }

    return json;
  }

  private JsonObject vehiculeToJson(Vehicule vehicule) {
    JsonObject json = new JsonObject()
      .put("id", vehicule.getId())
      .put("immatriculation", vehicule.getImmatriculation())
      .put("date_immatriculation", vehicule.getDateImmatriculation().toString())
      .put("model_id", vehicule.getModelId());

    if (vehicule.getCompagnieId() != null) {
      json.put("compagnie_id", vehicule.getCompagnieId());
    }

    if (vehicule.getModeleVehicule() != null) {
      json.put("model", modelToJson(vehicule.getModeleVehicule()));
    }

    if (vehicule.getCompagnie() != null) {
      json.put("compagnie_name", vehicule.getCompagnie().getRaison_social());
    }

    if (vehicule.getAttestationCount() != null) {
      json.put("attestation_count", vehicule.getAttestationCount());
    }

    return json;
  }


}
