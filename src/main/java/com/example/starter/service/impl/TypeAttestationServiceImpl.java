package com.example.starter.service.impl;

import com.example.starter.model.AttestationAutoriser;
import com.example.starter.model.Compagnie;
import com.example.starter.model.TypeAttestation;
import com.example.starter.repository.AttestationAutoriserRepository;
import com.example.starter.repository.CompagnieRepository;
import com.example.starter.repository.TypeAttestationRepository;
import com.example.starter.service.TypeAttestationService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class TypeAttestationServiceImpl implements TypeAttestationService {

  private final TypeAttestationRepository typeRepository;
  private final AttestationAutoriserRepository authRepository;
  private final CompagnieRepository compagnieRepository;

  public TypeAttestationServiceImpl(
    TypeAttestationRepository typeRepository,
    AttestationAutoriserRepository authRepository,
    CompagnieRepository compagnieRepository
  ) {
    this.typeRepository = typeRepository;
    this.authRepository = authRepository;
    this.compagnieRepository = compagnieRepository;
  }

  @Override
  public Future<JsonObject> createType(JsonObject data) {
    String libelle = data.getString("libelle");
    BigDecimal prixUnitaire = new BigDecimal(data.getString("prix_unitaire", "0"));
    String devise = "MAD"; // SIMPLE CHANGE: Always use MAD

    if (libelle == null || libelle.trim().isEmpty()) {
      return Future.failedFuture("Le libellé est requis");
    }

    if (prixUnitaire.compareTo(BigDecimal.ZERO) <= 0) {
      return Future.failedFuture("Le prix doit être supérieur à zéro");
    }

    // Check if libelle already exists
    return typeRepository.findByLibelle(libelle)
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture("Un type avec ce libellé existe déjà");
        }

        TypeAttestation type = new TypeAttestation();
        type.setLibelle(libelle);
        type.setPrixUnitaire(prixUnitaire);
        type.setDevise("MAD"); // SIMPLE CHANGE: Always MAD

        return typeRepository.save(type)
          .map(id -> new JsonObject()
            .put("id", id)
            .put("message", "Type d'attestation créé avec succès"));
      });
  }

  @Override
  public Future<JsonObject> getType(Long id) {
    return typeRepository.findById(id)
      .map(type -> {
        if (type == null) {
          throw new RuntimeException("Type d'attestation non trouvé");
        }
        return typeToJson(type);
      });
  }

  @Override
  public Future<JsonArray> getAllTypes() {
    return typeRepository.findAll()
      .map(types -> {
        JsonArray array = new JsonArray();
        types.forEach(type -> array.add(typeToJson(type)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> updateType(Long id, JsonObject data) {
    return typeRepository.findById(id)
      .compose(existing -> {
        if (existing == null) {
          return Future.failedFuture("Type d'attestation non trouvé");
        }

        String libelle = data.getString("libelle");
        BigDecimal prixUnitaire = new BigDecimal(data.getString("prix_unitaire", "0"));
        String devise = "MAD"; // SIMPLE CHANGE: Always use MAD

        if (libelle == null || libelle.trim().isEmpty()) {
          return Future.failedFuture("Le libellé est requis");
        }

        if (prixUnitaire.compareTo(BigDecimal.ZERO) <= 0) {
          return Future.failedFuture("Le prix doit être supérieur à zéro");
        }

        // Check if new libelle conflicts with another type
        if (!libelle.equalsIgnoreCase(existing.getLibelle())) {
          return typeRepository.findByLibelle(libelle)
            .compose(conflict -> {
              if (conflict != null) {
                return Future.failedFuture("Un autre type avec ce libellé existe déjà");
              }

              TypeAttestation updated = new TypeAttestation();
              updated.setLibelle(libelle);
              updated.setPrixUnitaire(prixUnitaire);
              updated.setDevise("MAD"); // SIMPLE CHANGE: Always MAD

              return typeRepository.update(id, updated)
                .map(v -> new JsonObject()
                  .put("message", "Type d'attestation modifié avec succès"));
            });
        } else {
          TypeAttestation updated = new TypeAttestation();
          updated.setLibelle(libelle);
          updated.setPrixUnitaire(prixUnitaire);
          updated.setDevise("MAD"); // SIMPLE CHANGE: Always MAD

          return typeRepository.update(id, updated)
            .map(v -> new JsonObject()
              .put("message", "Type d'attestation modifié avec succès"));
        }
      });
  }

  @Override
  public Future<Void> deleteType(Long id) {
    // Check if type is used
    return typeRepository.isUsedInAttestations(id)
      .compose(isUsed -> {
        if (isUsed) {
          return Future.failedFuture("Ce type est utilisé dans des attestations existantes");
        }
        return typeRepository.delete(id);
      });
  }

  @Override
  public Future<Void> authorizeCompany(Long compagnieId, Long typeAttestationId) {
    return authRepository.save(compagnieId, typeAttestationId, true);
  }

  @Override
  public Future<Void> unauthorizeCompany(Long compagnieId, Long typeAttestationId) {
    return authRepository.save(compagnieId, typeAttestationId, false);
  }

  @Override
  public Future<JsonArray> getCompanyAuthorizations(Long compagnieId) {
    return authRepository.findByCompagnieId(compagnieId)
      .map(authorizations -> {
        JsonArray array = new JsonArray();
        authorizations.forEach(auth -> array.add(authToJson(auth)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getTypeAuthorizations(Long typeAttestationId) {
    return authRepository.findByTypeAttestationId(typeAttestationId)
      .map(authorizations -> {
        JsonArray array = new JsonArray();
        authorizations.forEach(auth -> array.add(authToJson(auth)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> getAuthorizationMatrix() {
    return Future.all(
      compagnieRepository.findAll(),
      typeRepository.findAll(),
      authRepository.findAll()
    ).map(results -> {
      List<Compagnie> companies = results.resultAt(0);
      List<TypeAttestation> types = results.resultAt(1);
      List<AttestationAutoriser> authorizations = results.resultAt(2);

      // Build matrix
      JsonObject matrix = new JsonObject();
      JsonArray companiesArray = new JsonArray();
      JsonArray typesArray = new JsonArray();
      JsonObject authMatrix = new JsonObject();

      // Add companies
      companies.forEach(company -> {
        companiesArray.add(new JsonObject()
          .put("id", company.getId())
          .put("raison_social", company.getRaison_social()));
      });

      // Add types
      types.forEach(type -> {
        typesArray.add(typeToJson(type));
      });

      // Build authorization matrix
      authorizations.forEach(auth -> {
        String key = auth.getCompagnieId() + "_" + auth.getTypeAttestationId();
        authMatrix.put(key, auth.getFlag());
      });

      matrix.put("companies", companiesArray);
      matrix.put("types", typesArray);
      matrix.put("authorizations", authMatrix);

      return matrix;
    });
  }

  @Override
  public Future<Void> updateAuthorizationMatrix(JsonArray updates) {
    List<Future<Void>> futures = new ArrayList<>();

    for (int i = 0; i < updates.size(); i++) {
      JsonObject update = updates.getJsonObject(i);
      Long compagnieId = update.getLong("compagnie_id");
      Long typeId = update.getLong("type_id");
      Boolean authorized = update.getBoolean("authorized");

      futures.add(authRepository.save(compagnieId, typeId, authorized));
    }

    return Future.all(futures).mapEmpty();
  }

  @Override
  public Future<Boolean> isCompanyAuthorized(Long compagnieId, Long typeAttestationId) {
    return authRepository.isAuthorized(compagnieId, typeAttestationId);
  }

  @Override
  public Future<JsonArray> getAuthorizedTypesForCompany(Long compagnieId) {
    return authRepository.findByCompagnieId(compagnieId)
      .map(authorizations -> {
        JsonArray array = new JsonArray();
        authorizations.stream()
          .filter(auth -> auth.getFlag() && auth.getTypeAttestation() != null)
          .forEach(auth -> array.add(typeToJson(auth.getTypeAttestation())));
        return array;
      });
  }

  private JsonObject typeToJson(TypeAttestation type) {
    return new JsonObject()
      .put("id", type.getId())
      .put("libelle", type.getLibelle())
      .put("prix_unitaire", type.getPrixUnitaire())
      .put("devise", "MAD"); // SIMPLE CHANGE: Always show MAD
  }

  private JsonObject authToJson(AttestationAutoriser auth) {
    JsonObject json = new JsonObject()
      .put("compagnie_id", auth.getCompagnieId())
      .put("type_attestation_id", auth.getTypeAttestationId())
      .put("flag", auth.getFlag());

    if (auth.getCompagnie() != null) {
      json.put("compagnie_name", auth.getCompagnie().getRaison_social());
    }

    if (auth.getTypeAttestation() != null) {
      json.put("type_libelle", auth.getTypeAttestation().getLibelle())
        .put("prix_unitaire", auth.getTypeAttestation().getPrixUnitaire())
        .put("devise", "MAD"); // SIMPLE CHANGE: Add MAD to authorization display
    }
    return json;
  }

}
