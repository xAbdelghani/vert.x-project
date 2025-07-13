package com.example.starter.repository.impl;

import com.example.starter.model.Compagnie;
import com.example.starter.model.Contact;
import com.example.starter.model.Fonction;
import com.example.starter.repository.ContactRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class ContactRepositoryImpl implements ContactRepository {
  private final PgPool pgPool;

  public ContactRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  @Override
  public Future<Long> save(Contact contact) {
    // First, get the next ID
    String getNextIdSql = "SELECT COALESCE(MAX(id), 0) + 1 as next_id FROM contact";

    return pgPool.preparedQuery(getNextIdSql)
      .execute()
      .compose(rows -> {
        Long nextId = rows.iterator().next().getLong("next_id");

        String insertSql = """
                    INSERT INTO contact (
                        id, nomc, prenomc, fax, telephonec, emailc,
                        remarquec, compagnie_id, fonction_id
                    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
                    RETURNING id
                """;

        return pgPool.preparedQuery(insertSql)
          .execute(Tuple.of(
            nextId,
            contact.getNomc(),
            contact.getPrenomc(),
            contact.getFax(),
            contact.getTelephonec(),
            contact.getEmailc(),
            contact.getRemarquec(),
            contact.getCompagnieId(),
            contact.getFonctionId()
          ))
          .map(insertRows -> insertRows.iterator().next().getLong("id"));
      });
  }

  @Override
  public Future<Contact> findById(Long id) {
    String sql = """
            SELECT c.*, f.qualite as fonction_qualite,
                   comp.raison_social as compagnie_raison_social
            FROM contact c
            LEFT JOIN fonction f ON c.fonction_id = f.id
            LEFT JOIN compagnies comp ON c.compagnie_id = comp.id
            WHERE c.id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return null;
        return mapRowToContactWithRelations(rows.iterator().next());
      });
  }

  @Override
  public Future<List<Contact>> findAll() {
    String sql = """
            SELECT c.*, f.qualite as fonction_qualite,
                   comp.raison_social as compagnie_raison_social
            FROM contact c
            LEFT JOIN fonction f ON c.fonction_id = f.id
            LEFT JOIN compagnies comp ON c.compagnie_id = comp.id
            ORDER BY c.nomc, c.prenomc
        """;

    return pgPool.preparedQuery(sql)
      .execute()
      .map(rows -> {
        List<Contact> contacts = new ArrayList<>();
        rows.forEach(row -> contacts.add(mapRowToContactWithRelations(row)));
        return contacts;
      });
  }

  @Override
  public Future<List<Contact>> findByCompagnieId(Long compagnieId) {
    String sql = """
            SELECT c.*, f.qualite as fonction_qualite
            FROM contact c
            LEFT JOIN fonction f ON c.fonction_id = f.id
            WHERE c.compagnie_id = $1
            ORDER BY c.nomc, c.prenomc
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(compagnieId))
      .map(rows -> {
        List<Contact> contacts = new ArrayList<>();
        rows.forEach(row -> {
          Contact contact = mapRowToContact(row);
          // Add fonction info if available
          if (row.getString("fonction_qualite") != null) {
            Fonction fonction = new Fonction();
            fonction.setId(contact.getFonctionId());
            fonction.setQualite(row.getString("fonction_qualite"));
            contact.setFonction(fonction);
          }
          contacts.add(contact);
        });
        return contacts;
      });
  }

  @Override
  public Future<List<Contact>> findByFonctionId(Long fonctionId) {
    String sql = """
            SELECT c.*, comp.raison_social as compagnie_raison_social
            FROM contact c
            LEFT JOIN compagnies comp ON c.compagnie_id = comp.id
            WHERE c.fonction_id = $1
            ORDER BY c.nomc, c.prenomc
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(fonctionId))
      .map(rows -> {
        List<Contact> contacts = new ArrayList<>();
        rows.forEach(row -> {
          Contact contact = mapRowToContact(row);
          // Add compagnie info if available
          if (row.getString("compagnie_raison_social") != null) {
            Compagnie compagnie = new Compagnie();
            compagnie.setId(contact.getCompagnieId());
            compagnie.setRaison_social(row.getString("compagnie_raison_social"));
            contact.setCompagnie(compagnie);
          }
          contacts.add(contact);
        });
        return contacts;
      });
  }

  @Override
  public Future<Void> update(Long id, Contact contact) {
    String sql = """
            UPDATE contact SET
                nomc = $1, prenomc = $2, fax = $3,
                telephonec = $4, emailc = $5, remarquec = $6,
                compagnie_id = $7, fonction_id = $8
            WHERE id = $9
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(
        contact.getNomc(),
        contact.getPrenomc(),
        contact.getFax(),
        contact.getTelephonec(),
        contact.getEmailc(),
        contact.getRemarquec(),
        contact.getCompagnieId(),
        contact.getFonctionId(),
        id
      ))
      .map(rows -> null);
  }

  @Override
  public Future<Void> delete(Long id) {
    String sql = """
            DELETE FROM contact WHERE id = $1
        """;

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> null);
  }

  private Contact mapRowToContact(Row row) {
    Contact contact = new Contact();
    contact.setId(row.getLong("id"));
    contact.setNomc(row.getString("nomc"));
    contact.setPrenomc(row.getString("prenomc"));
    contact.setFax(row.getString("fax"));
    contact.setTelephonec(row.getString("telephonec"));
    contact.setEmailc(row.getString("emailc"));
    contact.setRemarquec(row.getString("remarquec"));
    contact.setCompagnieId(row.getLong("compagnie_id"));
    contact.setFonctionId(row.getLong("fonction_id"));
    return contact;
  }

  private Contact mapRowToContactWithRelations(Row row) {
    Contact contact = mapRowToContact(row);

    // Add related fonction info if available
    try {
      String fonctionQualite = row.getString("fonction_qualite");
      if (fonctionQualite != null) {
        Fonction fonction = new Fonction();
        fonction.setId(contact.getFonctionId());
        fonction.setQualite(fonctionQualite);
        contact.setFonction(fonction);
      }
    } catch (Exception e) {
      // Column doesn't exist in this query
    }

    // Add related compagnie info if available
    try {
      String compagnieRaisonSocial = row.getString("compagnie_raison_social");
      if (compagnieRaisonSocial != null) {
        Compagnie compagnie = new Compagnie();
        compagnie.setId(contact.getCompagnieId());
        compagnie.setRaison_social(compagnieRaisonSocial);
        contact.setCompagnie(compagnie);
      }
    } catch (Exception e) {
      // Column doesn't exist in this query
    }

    return contact;
  }
}
