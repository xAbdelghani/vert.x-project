package com.example.starter.service.impl;

import com.example.starter.model.TypeAbonnement;
import com.example.starter.repository.TypeAbonnementRepository;
import com.example.starter.service.TypeAbonnementService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.NoSuchElementException;


public class TypeAbonnementServiceImpl implements TypeAbonnementService {

  private final TypeAbonnementRepository typeAbonnementRepository;

  public TypeAbonnementServiceImpl(TypeAbonnementRepository typeAbonnementRepository) {
    this.typeAbonnementRepository = typeAbonnementRepository;
  }

  @Override
  public Future<Long> createType(JsonObject data) {
    // Validate required fields
    String libelle = data.getString("libelle");
    String categorie = data.getString("categorie");

    if (libelle == null || libelle.trim().isEmpty()) {
      return Future.failedFuture("Le libellé est requis");
    }

    if (categorie == null || (!categorie.equals("AVANCE") && !categorie.equals("CAUTION"))) {
      return Future.failedFuture("La catégorie doit être AVANCE ou CAUTION");
    }

    // Check if libelle already exists
    return typeAbonnementRepository.findByLibelle(libelle)
      .compose(existing -> {
        if (existing != null && existing.getActif()) {
          return Future.failedFuture("Un type avec ce libellé existe déjà");
        }

        TypeAbonnement typeAbonnement = new TypeAbonnement();
        typeAbonnement.setLibelle(libelle);
        typeAbonnement.setCategorie(categorie);
        typeAbonnement.setDuree(data.getDouble("duree"));
        typeAbonnement.setUnite(data.getString("unite"));
        typeAbonnement.setDescription(data.getString("description"));
        typeAbonnement.setActif(true);

        // Validate duration logic
        if (typeAbonnement.getDuree() != null && typeAbonnement.getDuree() > 0) {
          if (typeAbonnement.getUnite() == null || typeAbonnement.getUnite().isEmpty()) {
            return Future.failedFuture("L'unité est requise quand la durée est spécifiée");
          }
        } else {
          // For indefinite duration (mainly for CAUTION)
          typeAbonnement.setDuree(null);
          typeAbonnement.setUnite(null);
        }

        return typeAbonnementRepository.save(typeAbonnement);
      });
  }

  @Override
  public Future<JsonObject> getType(Long id) {
    return typeAbonnementRepository.findById(id)
      .map(type -> {
        if (type == null) {
          throw new NoSuchElementException("Type d'abonnement non trouvé");
        }
        return typeToJson(type);
      });
  }

  @Override
  public Future<JsonArray> getAllTypes() {
    return typeAbonnementRepository.findAll()
      .map(types -> {
        JsonArray array = new JsonArray();
        types.forEach(type -> array.add(typeToJson(type)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getTypesByCategorie(String categorie) {
    return typeAbonnementRepository.findByCategorie(categorie)
      .map(types -> {
        JsonArray array = new JsonArray();
        types.forEach(type -> array.add(typeToJson(type)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getActiveTypes() {
    return typeAbonnementRepository.findActifs()
      .map(types -> {
        JsonArray array = new JsonArray();
        types.forEach(type -> array.add(typeToJson(type)));
        return array;
      });
  }

  @Override
  public Future<Void> updateType(Long id, JsonObject data) {
    return typeAbonnementRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Type d'abonnement non trouvé");
        }

        String libelle = data.getString("libelle", existing.getLibelle());
        String categorie = data.getString("categorie", existing.getCategorie());

        // Validate categorie
        if (!categorie.equals("AVANCE") && !categorie.equals("CAUTION")) {
          return Future.failedFuture("La catégorie doit être AVANCE ou CAUTION");
        }

        TypeAbonnement updated = new TypeAbonnement();
        updated.setLibelle(libelle);
        updated.setCategorie(categorie);
        updated.setDuree(data.getDouble("duree"));
        updated.setUnite(data.getString("unite"));
        updated.setDescription(data.getString("description"));
        updated.setActif(data.getBoolean("actif", existing.getActif()));

        // Validate duration logic
        if (updated.getDuree() != null && updated.getDuree() > 0) {
          if (updated.getUnite() == null || updated.getUnite().isEmpty()) {
            return Future.failedFuture("L'unité est requise quand la durée est spécifiée");
          }
        }

        return typeAbonnementRepository.update(id, updated);
      });
  }

  @Override
  public Future<Void> deleteType(Long id) {
    // Check if type is used in any abonnements
    return typeAbonnementRepository.isUsedInAbonnements(id)
      .compose(isUsed -> {
        if (isUsed) {
          return Future.failedFuture("Ce type est utilisé dans des abonnements existants");
        }
        // Soft delete
        return typeAbonnementRepository.delete(id);
      });
  }



  private JsonObject typeToJson(TypeAbonnement type) {
    JsonObject json = new JsonObject()
      .put("id", type.getId())
      .put("libelle", type.getLibelle())
      .put("categorie", type.getCategorie())
      .put("actif", type.getActif());

    if (type.getDuree() != null) {
      json.put("duree", type.getDuree());
    }
    if (type.getUnite() != null) {
      json.put("unite", type.getUnite());
    }
    if (type.getDescription() != null) {
      json.put("description", type.getDescription());
    }

    // Add formatted duration for display
    if (type.getDuree() != null && type.getUnite() != null) {
      json.put("duree_format", formatDuration(type.getDuree(), type.getUnite()));
    } else {
      json.put("duree_format", "Indéterminée");
    }

    return json;
  }

  private String formatDuration(Double duree, String unite) {
    int dureeInt = duree.intValue();
    String uniteLabel = "";

    switch (unite) {
      case "JOURS":
        uniteLabel = dureeInt > 1 ? "jours" : "jour";
        break;
      case "MOIS":
        uniteLabel = "mois";
        break;
      case "ANNEES":
        uniteLabel = dureeInt > 1 ? "années" : "année";
        break;
      default:
        uniteLabel = unite.toLowerCase();
    }

    return dureeInt + " " + uniteLabel;
  }


}
