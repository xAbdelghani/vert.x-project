package com.example.starter.repository.impl;

import com.example.starter.model.Attestation;
import com.example.starter.model.Compagnie;
import com.example.starter.model.TypeAttestation;
import com.example.starter.model.Vehicule;
import com.example.starter.repository.AttestationRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AttestationRepositoryImpl implements AttestationRepository {

  private final PgPool pgPool;

  public AttestationRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Attestation attestation) {
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM attestation";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String sql = """
          INSERT INTO attestation (
            id, compagnie_id, vehicule_id, typeattestation_id,
            référence_flotte, date_generation, date_debut, date_fin,
            qr_code, reste_attributs_json
          ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
          RETURNING id
        """;

        // First, insert into statut_att if not exists
        String insertStatusSql = """
          INSERT INTO statut_att (id, libelle)
          VALUES (1, 'EN_COURS'), (2, 'TERMINE'), (3, 'ANNULE')
          ON CONFLICT (id) DO NOTHING
        """;

        return pgPool.query(insertStatusSql).execute()
          .compose(v -> pgPool.preparedQuery(sql)
            .execute(Tuple.of(
              nextId,
              attestation.getCompagnieId(),
              attestation.getVehiculeId(),
              attestation.getTypeAttestationId(),
              attestation.getReferenceFlotte(),
              attestation.getDateGeneration(),
              attestation.getDateDebut(),
              attestation.getDateFin(),
              attestation.getQrCode(),
              attestation.getResteAttributsJson()
            )))
          .compose(insertRows -> {
            // Add initial status history
            String historySql = """
              INSERT INTO historique_statut_att (
                id, attestation_id, statut_att_id, date_statut, statut, raison
              ) VALUES (
                (SELECT COALESCE(MAX(id), 0) + 1 FROM historique_statut_att),
                $1, 1, $2, 'EN_COURS', 'Création initiale'
              )
            """;

            return pgPool.preparedQuery(historySql)
              .execute(Tuple.of(nextId, LocalDate.now().toString()))
              .map(v -> nextId);
          });
      });
  }

  @Override
  public Future<Attestation> findById(Long id) {
    String sql = """
      SELECT a.*,
             c.raison_social,
             v.immatriculation,
             ta.libelle as type_libelle, ta.prix_unitaire,
             (SELECT statut FROM historique_statut_att
              WHERE attestation_id = a.id
              ORDER BY id DESC LIMIT 1) as statut
      FROM attestation a
      LEFT JOIN compagnies c ON a.compagnie_id = c.id
      LEFT JOIN vehicule v ON a.vehicule_id = v.id
      LEFT JOIN type_attestation ta ON a.typeattestation_id = ta.id
      WHERE a.id = $1
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToAttestation(rows.iterator().next());
      });
  }

  @Override
  public Future<Attestation> findByReference(String reference) {
    String sql = """
      SELECT a.*,
             c.raison_social,
             v.immatriculation,
             ta.libelle as type_libelle, ta.prix_unitaire,
             (SELECT statut FROM historique_statut_att
              WHERE attestation_id = a.id
              ORDER BY id DESC LIMIT 1) as statut
      FROM attestation a
      LEFT JOIN compagnies c ON a.compagnie_id = c.id
      LEFT JOIN vehicule v ON a.vehicule_id = v.id
      LEFT JOIN type_attestation ta ON a.typeattestation_id = ta.id
      WHERE a.référence_flotte = $1
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(reference))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToAttestation(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Attestation>> findByCompagnieId(Long compagnieId) {
    String sql = """
      SELECT a.*,
             v.immatriculation,
             ta.libelle as type_libelle, ta.prix_unitaire,
             (SELECT statut FROM historique_statut_att
              WHERE attestation_id = a.id
              ORDER BY id DESC LIMIT 1) as statut
      FROM attestation a
      LEFT JOIN vehicule v ON a.vehicule_id = v.id
      LEFT JOIN type_attestation ta ON a.typeattestation_id = ta.id
      WHERE a.compagnie_id = $1
      ORDER BY a.date_generation DESC
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<Attestation> attestations = new ArrayList<>();
        rows.forEach(row -> attestations.add(mapRowToAttestation(row)));
        return attestations;
      });
  }

  @Override
  public Future<List<Attestation>> findByVehiculeId(Long vehiculeId) {
    String sql = """
      SELECT a.*,
             c.raison_social,
             ta.libelle as type_libelle, ta.prix_unitaire,
             (SELECT statut FROM historique_statut_att
              WHERE attestation_id = a.id
              ORDER BY id DESC LIMIT 1) as statut
      FROM attestation a
      LEFT JOIN compagnies c ON a.compagnie_id = c.id
      LEFT JOIN type_attestation ta ON a.typeattestation_id = ta.id
      WHERE a.vehicule_id = $1
      ORDER BY a.date_generation DESC
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(vehiculeId))
      .map(rows -> {
        List<Attestation> attestations = new ArrayList<>();
        rows.forEach(row -> attestations.add(mapRowToAttestation(row)));
        return attestations;
      });
  }

  @Override
  public Future<List<Attestation>> findActiveByVehiculeId(Long vehiculeId) {
    String sql = """
      SELECT a.*,
             c.raison_social,
             ta.libelle as type_libelle, ta.prix_unitaire,
             h.statut
      FROM attestation a
      LEFT JOIN compagnies c ON a.compagnie_id = c.id
      LEFT JOIN type_attestation ta ON a.typeattestation_id = ta.id
      INNER JOIN (
        SELECT DISTINCT ON (attestation_id)
               attestation_id, statut
        FROM historique_statut_att
        ORDER BY attestation_id, id DESC
      ) h ON h.attestation_id = a.id
      WHERE a.vehicule_id = $1 AND h.statut = 'EN_COURS'
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(vehiculeId))
      .map(rows -> {
        List<Attestation> attestations = new ArrayList<>();
        rows.forEach(row -> attestations.add(mapRowToAttestation(row)));
        return attestations;
      });
  }

  @Override
  public Future<List<Attestation>> findExpiringAttestations(LocalDate date) {
    String sql = """
      SELECT a.*, h.statut
      FROM attestation a
      INNER JOIN (
        SELECT DISTINCT ON (attestation_id)
               attestation_id, statut
        FROM historique_statut_att
        ORDER BY attestation_id, id DESC
      ) h ON h.attestation_id = a.id
      WHERE a.date_fin < $1 AND h.statut = 'EN_COURS'
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(date))
      .map(rows -> {
        List<Attestation> attestations = new ArrayList<>();
        rows.forEach(row -> attestations.add(mapRowToAttestation(row)));
        return attestations;
      });
  }

  @Override
  public Future<Void> updateStatus(Long id, String status) {
    // Status is tracked in history table, no need to update attestation table
    return Future.succeededFuture();
  }

  @Override
  public Future<Void> addStatusHistory(Long attestationId, String status, String reason) {
    String sql = """
      INSERT INTO historique_statut_att (
        id, attestation_id, statut_att_id, date_statut, statut, raison
      ) VALUES (
        (SELECT COALESCE(MAX(id), 0) + 1 FROM historique_statut_att),
        $1,
        (SELECT id FROM statut_att WHERE libelle = $2),
        $3, $4, $5
      )
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        attestationId,
        status,
        LocalDate.now().toString(),
        status,
        reason
      ))
      .mapEmpty();
  }

  @Override
  public Future<Void> updatePDFInfo(Long id, String fileName, String filePath, byte[] pdfData) {
    String sql = """
      UPDATE attestation
      SET nom_fichier = $1,
          chemin_dépôt_attestation_générée = $2,
          fichier_data = $3
      WHERE id = $4
    """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(fileName, filePath, pdfData, id))
      .mapEmpty();
  }

  @Override
  public Future<byte[]> getPDFData(Long id) {
    String sql = "SELECT fichier_data FROM attestation WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return rows.iterator().next().getBuffer("fichier_data").getBytes();
      });
  }

  private Attestation mapRowToAttestation(Row row) {
    Attestation attestation = new Attestation();
    attestation.setId(row.getLong("id"));
    attestation.setCompagnieId(row.getLong("compagnie_id"));
    attestation.setVehiculeId(row.getLong("vehicule_id"));
    attestation.setTypeAttestationId(row.getLong("typeattestation_id"));
    attestation.setReferenceFlotte(row.getString("référence_flotte"));
    attestation.setDateGeneration(row.getLocalDate("date_generation"));
    attestation.setDateDebut(row.getLocalDate("date_debut"));
    attestation.setDateFin(row.getLocalDate("date_fin"));
    attestation.setQrCode(row.getString("qr_code"));
    attestation.setNomFichier(row.getString("nom_fichier"));
    attestation.setCheminDepotAttestationGeneree(row.getString("chemin_dépôt_attestation_générée"));
    attestation.setResteAttributsJson(row.getString("reste_attributs_json"));

    // Get status from query
    try {
      attestation.setStatut(row.getString("statut"));
    } catch (Exception e) {
      attestation.setStatut("EN_COURS");
    }

    // Map related entities if available
    try {
      String raisonSocial = row.getString("raison_social");
      if (raisonSocial != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(attestation.getCompagnieId());
        compagnie.setRaison_social(raisonSocial);
        attestation.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column not in query
    }

    try {
      String immatriculation = row.getString("immatriculation");
      if (immatriculation != null) {
        Vehicule vehicule = new Vehicule();
        vehicule.setId(attestation.getVehiculeId());
        vehicule.setImmatriculation(immatriculation);
        attestation.setVehicule(vehicule);
      }
    } catch (Exception e) {
      // Column not in query
    }

    try {
      String typeLibelle = row.getString("type_libelle");
      if (typeLibelle != null) {
        TypeAttestation type = new TypeAttestation();
        type.setId(attestation.getTypeAttestationId());
        type.setLibelle(typeLibelle);
        type.setPrixUnitaire(row.getBigDecimal("prix_unitaire"));
        attestation.setTypeAttestation(type);
      }
    } catch (Exception e) {
      // Column not in query
    }

    return attestation;
  }
}
