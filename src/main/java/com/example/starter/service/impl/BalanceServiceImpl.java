package com.example.starter.service.impl;

import com.example.starter.model.Abonnement;
import com.example.starter.model.SoldePrepaye;
import com.example.starter.model.Transaction;
import com.example.starter.repository.AbonnementRepository;
import com.example.starter.repository.SoldePrePayeRepository;
import com.example.starter.repository.TransactionRepository;
import com.example.starter.repository.TypeAbonnementRepository;
import com.example.starter.repository.impl.CompagnieRepositoryImpl;
import com.example.starter.service.BalanceService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;

public class BalanceServiceImpl implements BalanceService {

  private final SoldePrePayeRepository soldeRepository;
  private final AbonnementRepository abonnementRepository;
  private final TypeAbonnementRepository typeAbonnementRepository;
  private final TransactionRepository transactionRepository;
  private final CompagnieRepositoryImpl compagnieRepository;

  // Attestation price - should be configurable
  private static final BigDecimal ATTESTATION_PRICE = new BigDecimal("50.00");

  public BalanceServiceImpl(
    SoldePrePayeRepository soldeRepository,
    AbonnementRepository abonnementRepository,
    TypeAbonnementRepository typeAbonnementRepository,
    TransactionRepository transactionRepository,
    CompagnieRepositoryImpl compagnieRepository
  ) {
    this.soldeRepository = soldeRepository;
    this.abonnementRepository = abonnementRepository;
    this.typeAbonnementRepository = typeAbonnementRepository;
    this.transactionRepository = transactionRepository;
    this.compagnieRepository = compagnieRepository;
  }

  @Override
  public Future<JsonObject> getCompanyBalance(Long compagnieId) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(solde -> {
        if (solde == null) {
          return Future.succeededFuture(new JsonObject()
            .put("compagnie_id", compagnieId)
            .put("solde", 0)
            .put("status", "NO_BALANCE"));
        }

        // Get active subscription info
        return abonnementRepository.findActiveByCompagnieId(compagnieId)
          .map(abonnement -> {
            JsonObject result = soldeToJson(solde);
            if (abonnement != null && abonnement.getTypeAbonnement() != null) {
              result.put("payment_model", abonnement.getTypeAbonnement().getLibelle());
            }
            return result;
          });
      });
  }

  @Override
  public Future<JsonArray> getAllBalances() {
    return soldeRepository.findAll()
      .map(soldes -> {
        JsonArray array = new JsonArray();
        soldes.forEach(solde -> array.add(soldeToJson(solde)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> addCredit(Long compagnieId, BigDecimal amount, String description) {
    // First get current balance
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(currentSolde -> {
        if (currentSolde == null) {
          return Future.failedFuture("No balance record found for company");
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

        return transactionRepository.save(transaction)
          .compose(transactionId -> {
            // Update balance
            return soldeRepository.updateSolde(compagnieId, newBalance)
              .map(v -> new JsonObject()
                .put("transaction_id", transactionId)
                .put("old_balance", oldBalance)
                .put("amount_added", amount)
                .put("new_balance", newBalance)
                .put("status", "SUCCESS"));
          });
      });
  }

  @Override
  public Future<JsonObject> deductCredit(Long compagnieId, BigDecimal amount, String description) {
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(currentSolde -> {
        if (currentSolde == null) {
          return Future.failedFuture("No balance record found for company");
        }

        BigDecimal oldBalance = currentSolde.getSolde();

        // Check sufficient balance
        if (oldBalance.compareTo(amount) < 0) {
          return Future.failedFuture("Insufficient balance");
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

        return transactionRepository.save(transaction)
          .compose(transactionId -> {
            // Update balance
            return soldeRepository.updateSolde(compagnieId, newBalance)
              .map(v -> new JsonObject()
                .put("transaction_id", transactionId)
                .put("old_balance", oldBalance)
                .put("amount_deducted", amount)
                .put("new_balance", newBalance)
                .put("status", "SUCCESS"));
          });
      });
  }

  @Override
  public Future<Boolean> hassufficientBalance(Long compagnieId, BigDecimal amount) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(abonnement -> {
        if (abonnement == null || abonnement.getTypeAbonnement() == null) {
          return Future.succeededFuture(false);
        }

        String paymentModel = abonnement.getTypeAbonnement().getLibelle();

        // For PREPAYE, check actual balance
        if ("PREPAYE".equals(paymentModel)) {
          return soldeRepository.findByCompagnieId(compagnieId)
            .map(solde -> solde != null && solde.getSolde().compareTo(amount) >= 0);
        }

        // For AVANCE (credit), check credit limit (simplified - you'd need credit limit field)
        if ("AVANCE".equals(paymentModel)) {
          // TODO: Implement credit limit check
          return Future.succeededFuture(true); // Allow for now
        }

        // For CAUTION, check if within deposit limit
        if ("CAUTION".equals(paymentModel)) {
          // TODO: Implement deposit limit check
          return Future.succeededFuture(true); // Allow for now
        }

        return Future.succeededFuture(false);
      });
  }

  @Override
  public Future<JsonArray> getLowBalanceCompanies(BigDecimal threshold) {
    return soldeRepository.findLowBalances(threshold)
      .map(soldes -> {
        JsonArray array = new JsonArray();
        soldes.forEach(solde -> array.add(soldeToJson(solde)));
        return array;
      });
  }

  @Override
  public Future<JsonArray> getTransactionHistory(Long compagnieId) {
    return transactionRepository.findByCompagnieId(compagnieId)
      .map(transactions -> {
        JsonArray array = new JsonArray();
        transactions.forEach(transaction -> array.add(transactionToJson(transaction)));
        return array;
      });
  }

  @Override
  public Future<JsonObject> initializeCompanyBalance(Long compagnieId, String paymentModel, BigDecimal initialAmount, String devise) {
    // Validate devise
    if (devise == null || devise.trim().isEmpty()) {
      devise = "MAD"; // Default to MAD
    }

    final String finalDevise = devise; // For lambda usage

    // First check if balance already exists
    return soldeRepository.findByCompagnieId(compagnieId)
      .compose(existing -> {
        if (existing != null) {
          return Future.failedFuture("Balance already exists for this company");
        }

        // For PREPAYE, we don't need TypeAbonnement
        if ("PREPAYE".equals(paymentModel)) {
          // This should be handled by PrepayeService, but as fallback:
          SoldePrepaye solde = new SoldePrepaye();
          solde.setCompagnieId(compagnieId);
          solde.setSolde(initialAmount);
          solde.setType(paymentModel);
          solde.setStatut("ACTIF");
          solde.setDevise(finalDevise);

          return soldeRepository.save(solde)
            .compose(soldeId -> {
              // Create initial transaction
              if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
                Transaction transaction = new Transaction();
                transaction.setCompagnieId(compagnieId);
                transaction.setType("CREDIT");
                transaction.setMontant(initialAmount);
                transaction.setSoldeAvant(BigDecimal.ZERO);
                transaction.setSoldeApres(initialAmount);
                transaction.setDescription("Initial balance - " + paymentModel + " - " + finalDevise);

                return transactionRepository.save(transaction)
                  .map(transactionId -> new JsonObject()
                    .put("solde_id", soldeId)
                    .put("transaction_id", transactionId)
                    .put("devise", finalDevise)
                    .put("status", "SUCCESS"));
              }

              return Future.succeededFuture(new JsonObject()
                .put("solde_id", soldeId)
                .put("devise", finalDevise)
                .put("status", "SUCCESS"));
            });
        }

        // For AVANCE/CAUTION, get payment model type
        return typeAbonnementRepository.findByLibelle(paymentModel)
          .compose(typeAbonnement -> {
            if (typeAbonnement == null) {
              return Future.failedFuture("Invalid payment model");
            }

            // Create subscription record with devise
            Abonnement abonnement = new Abonnement();
            abonnement.setCompagnieId(compagnieId);
            abonnement.setTypeabonnementId(typeAbonnement.getId());
            abonnement.setDateAbonnement(LocalDate.now());
            abonnement.setMontant(initialAmount);
            abonnement.setType(paymentModel);
            abonnement.setStatut("ACTIF");
            abonnement.setDevise(finalDevise); // Use provided devise

            return abonnementRepository.save(abonnement);
          })
          .compose(abonnementId -> {
            // Create balance record with devise
            SoldePrepaye solde = new SoldePrepaye();
            solde.setCompagnieId(compagnieId);
            solde.setSolde(initialAmount);
            solde.setType(paymentModel);
            solde.setStatut("ACTIF");
            solde.setDevise(finalDevise); // Use provided devise

            return soldeRepository.save(solde);
          })
          .compose(soldeId -> {
            // Create initial transaction
            if (initialAmount.compareTo(BigDecimal.ZERO) > 0) {
              Transaction transaction = new Transaction();
              transaction.setCompagnieId(compagnieId);
              transaction.setType("CREDIT");
              transaction.setMontant(initialAmount);
              transaction.setSoldeAvant(BigDecimal.ZERO);
              transaction.setSoldeApres(initialAmount);
              transaction.setDescription("Initial balance - " + paymentModel + " - " + finalDevise);

              return transactionRepository.save(transaction)
                .map(transactionId -> new JsonObject()
                  .put("solde_id", soldeId)
                  .put("transaction_id", transactionId)
                  .put("devise", finalDevise)
                  .put("status", "SUCCESS"));
            }

            return Future.succeededFuture(new JsonObject()
              .put("solde_id", soldeId)
              .put("devise", finalDevise)
              .put("status", "SUCCESS"));
          });
      });
  }

  private JsonObject soldeToJson(SoldePrepaye solde) {

    JsonObject json = new JsonObject()
      .put("id", solde.getId())
      .put("compagnie_id", solde.getCompagnieId())
      .put("solde", solde.getSolde())
      .put("type", solde.getType())
      .put("statut", solde.getStatut())
      .put("devise", solde.getDevise());

    if (solde.getCompagnie() != null) {
      json.put("compagnie_name", solde.getCompagnie().getRaison_social());
    }

    if (solde.getDateAbonnement() != null) {
      json.put("date_abonnement", solde.getDateAbonnement().toString());
    }

    if (solde.getSoldeAttestation() != null) {
      json.put("solde_attestation", solde.getSoldeAttestation());
    }

    return json;
  }

  private JsonObject transactionToJson(Transaction transaction) {
    return new JsonObject()
      .put("id", transaction.getId())
      .put("type", transaction.getType())
      .put("montant", transaction.getMontant())
      .put("solde_avant", transaction.getSoldeAvant())
      .put("solde_apres", transaction.getSoldeApres())
      .put("description", transaction.getDescription())
      .put("date_transaction", transaction.getDateTransaction().toString())
      .put("reference", transaction.getReference());
  }

}
