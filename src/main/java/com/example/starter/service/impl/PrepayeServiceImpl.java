package com.example.starter.service.impl;

import com.example.starter.model.SoldePrepaye;
import com.example.starter.model.Transaction;
import com.example.starter.repository.SoldePrePayeRepository;
import com.example.starter.repository.TransactionRepository;
import com.example.starter.repository.impl.CompagnieRepositoryImpl;
import com.example.starter.service.PrepayeService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class PrepayeServiceImpl implements PrepayeService {

  private final SoldePrePayeRepository soldeRepository;
  private final TransactionRepository transactionRepository;
  private final CompagnieRepositoryImpl compagnieRepository;

  // Attestation price - should be configurable
  private static final BigDecimal ATTESTATION_PRICE = new BigDecimal("50.00");

  public PrepayeServiceImpl(
    SoldePrePayeRepository soldeRepository,
    TransactionRepository transactionRepository,
    CompagnieRepositoryImpl compagnieRepository
  ) {
    this.soldeRepository = soldeRepository;
    this.transactionRepository = transactionRepository;
    this.compagnieRepository = compagnieRepository;
  }

  @Override
  public Future<JsonObject> initializePrepaye(Long compagnieId, BigDecimal initialAmount, String devise) {
    // SIMPLE CHANGE: Always use MAD, ignore devise parameter
    devise = "MAD";

    // Check if PREPAYE balance already exists
    String finalDevise = devise;
    String finalDevise1 = devise;
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture("PREPAYE balance already exists for this company");
        }

        // Create PREPAYE balance record with devise
        SoldePrepaye solde = new SoldePrepaye();
        solde.setCompagnieId(compagnieId);
        solde.setSolde(initialAmount);
        solde.setType("PREPAYE");
        solde.setStatut("ACTIF");
        solde.setDevise("MAD"); // SIMPLE CHANGE: Always MAD
        solde.setDateAbonnement(LocalDate.now());

        return soldeRepository.save(solde);
      })
      .compose(soldeId -> {
        // Create initial transaction if amount > 0
        if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
          Transaction transaction = new Transaction();
          transaction.setCompagnieId(compagnieId);
          transaction.setType("CREDIT");
          transaction.setMontant(initialAmount);
          transaction.setSoldeAvant(BigDecimal.ZERO);
          transaction.setSoldeApres(initialAmount);
          transaction.setDescription("Initial PREPAYE balance - MAD"); // SIMPLE CHANGE
          transaction.setDateTransaction(LocalDateTime.now());

          return transactionRepository.save(transaction)
            .map(transactionId -> new JsonObject()
              .put("solde_id", soldeId)
              .put("transaction_id", transactionId)
              .put("initial_balance", initialAmount)
              .put("devise", "MAD") // SIMPLE CHANGE
              .put("status", "SUCCESS")
              .put("message", "PREPAYE account initialized successfully"));
        }

        return Future.succeededFuture(new JsonObject()
          .put("solde_id", soldeId)
          .put("initial_balance", initialAmount)
          .put("devise", "MAD") // SIMPLE CHANGE
          .put("status", "SUCCESS")
          .put("message", "PREPAYE account initialized successfully"));
      });
  }

  @Override
  public Future<JsonArray> getAllPrepayeBalances() {
    return soldeRepository.findAllByType("PREPAYE")
      .map(soldes -> {
        JsonArray array = new JsonArray();
        soldes.forEach(solde -> array.add(soldeToJson(solde)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> getPrepayeBalance(Long compagnieId) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .map(solde -> {
        if (solde == null || !"PREPAYE".equals(solde.getType())) {
          return new JsonObject()
            .put("compagnie_id", compagnieId)
            .put("solde", 0)
            .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
            .put("status", "NO_PREPAYE_BALANCE")
            .put("message", "Company does not have PREPAYE account");
        }

        return soldeToJson(solde)
          .put("status", "ACTIVE");
      });
  }

  @Override
  public Future<JsonObject> addCredit(Long compagnieId, BigDecimal amount, String description) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(currentSolde -> {
        if (currentSolde == null || !"PREPAYE".equals(currentSolde.getType())) {
          return Future.failedFuture("No PREPAYE account found for this company");
        }

        BigDecimal oldBalance = currentSolde.getSolde();
        BigDecimal newBalance = oldBalance.add(amount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setCompagnieId(compagnieId);
        transaction.setType("CREDIT");
        transaction.setMontant(amount);
        transaction.setSoldeAvant(oldBalance);
        transaction.setSoldeApres(newBalance);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.now());

        return transactionRepository.save(transaction)
          .compose(transactionId -> {
            // Update balance
            return soldeRepository.updateSolde(compagnieId, newBalance)
              .map(v -> new JsonObject()
                .put("transaction_id", transactionId)
                .put("old_balance", oldBalance)
                .put("amount_added", amount)
                .put("new_balance", newBalance)
                .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
                .put("status", "SUCCESS")
                .put("message", "Credit added successfully"));
          });
      });
  }

  @Override
  public Future<JsonObject> deductCredit(Long compagnieId, BigDecimal amount, String description) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(currentSolde -> {
        if (currentSolde == null || !"PREPAYE".equals(currentSolde.getType())) {
          return Future.failedFuture("No PREPAYE account found for this company");
        }

        BigDecimal oldBalance = currentSolde.getSolde();

        // Check sufficient balance
        if (oldBalance.compareTo(amount) < 0) {
          return Future.failedFuture("Insufficient PREPAYE balance. Current balance: " +
            oldBalance + " MAD, Required: " + amount + " MAD");
        }

        BigDecimal newBalance = oldBalance.subtract(amount);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setCompagnieId(compagnieId);
        transaction.setType("DEBIT");
        transaction.setMontant(amount);
        transaction.setSoldeAvant(oldBalance);
        transaction.setSoldeApres(newBalance);
        transaction.setDescription(description);
        transaction.setDateTransaction(LocalDateTime.now());

        return transactionRepository.save(transaction)
          .compose(transactionId -> {
            // Update balance
            return soldeRepository.updateSolde(compagnieId, newBalance)
              .map(v -> new JsonObject()
                .put("transaction_id", transactionId)
                .put("old_balance", oldBalance)
                .put("amount_deducted", amount)
                .put("new_balance", newBalance)
                .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
                .put("status", "SUCCESS")
                .put("message", "Debit processed successfully"));
          });
      });
  }

  @Override
  public Future<Boolean> hasSufficientBalance(Long compagnieId, BigDecimal amount) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .map(solde -> {
        if (solde == null || !"PREPAYE".equals(solde.getType())) {
          return false;
        }
        return solde.getSolde().compareTo(amount) >= 0;
      }
      );
  }

  @Override
  public Future<JsonArray> getLowBalanceCompanies(BigDecimal threshold) {
    return soldeRepository.findLowBalancesByType("PREPAYE", threshold)
      .map(soldes -> {
        JsonArray array = new JsonArray();
        soldes.forEach(solde -> array.add(soldeToJson(solde)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getTransactionHistory(Long compagnieId) {
    // First verify it's a PREPAYE company
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(solde -> {
        if (solde == null || !"PREPAYE".equals(solde.getType())) {
          return Future.succeededFuture(new JsonArray());
        }

        return transactionRepository.findByCompagnieId(compagnieId)
          .map(transactions -> {
            JsonArray array = new JsonArray();
            transactions.forEach(transaction -> array.add(transactionToJson(transaction)));
            return array;
          });
      });
  }

  @Override
  public Future<JsonObject> processAttestationPayment(Long compagnieId, String attestationRef) {
    return deductCredit(
      compagnieId,
      ATTESTATION_PRICE,
      "Attestation payment - Ref: " + attestationRef
    );
  }

  private JsonObject soldeToJson(SoldePrepaye solde) {
    JsonObject json = new JsonObject()
      .put("id", solde.getId())
      .put("compagnie_id", solde.getCompagnieId())
      .put("solde", solde.getSolde())
      .put("type", solde.getType())
      .put("statut", solde.getStatut())
      .put("devise", "MAD"); // SIMPLE CHANGE: Always show MAD

    if (solde.getCompagnie() != null) {
      json.put("compagnie_name", solde.getCompagnie().getRaison_social());
    }

    if (solde.getDateAbonnement() != null) {
      json.put("date_abonnement", solde.getDateAbonnement().toString());
    }

    // Calculate days since last recharge
    if (solde.getDateAbonnement() != null) {
      long daysSinceRecharge = ChronoUnit.DAYS.between(
        solde.getDateAbonnement(),
        LocalDate.now()
      );
      json.put("days_since_last_recharge", daysSinceRecharge);
    }

    return json;
  }

  private JsonObject transactionToJson(Transaction transaction) {
    return new JsonObject()
      .put("id", transaction.getId())
      .put("type", transaction.getType())
      .put("montant", transaction.getMontant())
      .put("devise", "MAD") // SIMPLE CHANGE: Add MAD to transactions
      .put("solde_avant", transaction.getSoldeAvant())
      .put("solde_apres", transaction.getSoldeApres())
      .put("description", transaction.getDescription())
      .put("date_transaction", transaction.getDateTransaction().toString())
      .put("reference", transaction.getReference());
  }
}
