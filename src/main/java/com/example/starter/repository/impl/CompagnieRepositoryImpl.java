package com.example.starter.repository.impl;

import com.example.starter.model.Compagnie;
import com.example.starter.repository.CompagnieRepository;
import io.vertx.core.Future;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class CompagnieRepositoryImpl  implements CompagnieRepository {

  private final PgPool pgPool;

  public CompagnieRepositoryImpl(PgPool pgPool) {
    this.pgPool = pgPool;
  }

  // Step 1: Insert basic compagnie data
  public Future<Long> save(String raisonSocial, String email, String telephone, String adresse) {
    String sql = """
      INSERT INTO compagnies (raison_social, email, telephone, adresse)
      VALUES ($1, $2, $3, $4)
      RETURNING id
    """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(raisonSocial, email, telephone, adresse))
      .map(rows -> rows.iterator().next().getLong("id"));
  }

  // Step 2: Update login (nom) and password
  public Future<Void> createAccountForCompagnie(Long id, String nom, String password) {
    String sql = """
      UPDATE compagnies
      SET nom = $1, password = $2
      WHERE id = $3
    """;
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(nom,password,id))
      .mapEmpty();
  }


  // Find a compagnie by ID
  public Future<Compagnie> findById(Long id) {
    String sql = "SELECT * FROM compagnies WHERE id = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .map(rows -> rows.iterator().hasNext() ? mapRow(rows.iterator().next()) : null);
  }



  // Optional: Find by email
  public Future<Compagnie> findByEmail(String email) {

    String sql = "SELECT * FROM compagnies WHERE email = $1";

    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(email))
      .map(rows -> rows.iterator().hasNext() ? mapRow(rows.iterator().next()) : null);
  }




  public Future<List<Compagnie>> findAll() {
    String sql = "SELECT * FROM compagnies";

    return pgPool.query(sql)
      .execute()
      .map(rowSet -> {
        List<Compagnie> list = new ArrayList<>();
        for (Row row : rowSet) {
          list.add(mapRow(row));
        }
        return list;
      });
  }



  // Delete a compagnie by ID
  public Future<Void> deleteById(Long id) {
    String sql = "DELETE FROM compagnies WHERE id = $1";
    return pgPool.preparedQuery(sql)
      .execute(Tuple.of(id))
      .mapEmpty();
  }



  // Mapping method
  private Compagnie mapRow(Row row) {
    Compagnie compagnie = new Compagnie();
    compagnie.setId(row.getLong("id"));
    compagnie.setNom(row.getString("nom"));
    compagnie.setRaison_social(row.getString("raison_social"));
    compagnie.setAdresse(row.getString("adresse"));
    compagnie.setTelephone(row.getString("telephone"));
    compagnie.setEmail(row.getString("email"));
    compagnie.setStatut(row.getString("statut"));
    compagnie.setPassword(row.getString("password"));
    compagnie.setSoldeCompagnie(
      Optional.ofNullable(row.getBigDecimal("soldecompagnie")).orElse(BigDecimal.ZERO)
    );
    return compagnie;
  }

}
