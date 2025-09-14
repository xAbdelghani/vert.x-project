package com.example.starter.service.impl;

import com.example.starter.model.Abonnement;
import com.example.starter.model.SoldePrepaye;
import com.example.starter.model.Transaction;
import com.example.starter.model.constant.AbonnementStatus;
import com.example.starter.repository.*;
import com.example.starter.service.SubscriptionService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Tuple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionServiceImpl implements SubscriptionService {

  private final PgPool pgPool;
  private final AbonnementRepository abonnementRepository;
  private final TypeAbonnementRepository typeAbonnementRepository;
  private final SoldePrePayeRepository soldePrePayeRepository;
  private final TransactionRepository transactionRepository;

  public SubscriptionServiceImpl(
    PgPool pgPool, AbonnementRepository abonnementRepository,
    TypeAbonnementRepository typeAbonnementRepository,
    SoldePrePayeRepository soldePrePayeRepository,
    TransactionRepository transactionRepository
  ) {
    this.pgPool = pgPool;
    this.abonnementRepository = abonnementRepository;
    this.typeAbonnementRepository = typeAbonnementRepository;
    this.soldePrePayeRepository = soldePrePayeRepository;
    this.transactionRepository = transactionRepository;
  }

  @Override
  public Future<JsonObject> createSubscription(JsonObject data) {
    Long compagnieId = data.getLong("compagnie_id");
    Long typeAbonnementId = data.getLong("type_abonnement_id");
    BigDecimal montant = new BigDecimal(data.getString("montant", "0"));
    String devise = "MAD"; // SIMPLE CHANGE: Always use MAD

    // Validate inputs
    if (compagnieId == null || typeAbonnementId == null) {
      return Future.failedFuture("Company ID and Type Abonnement ID are required");
    }

    if (montant.compareTo(BigDecimal.ZERO) <= 0) {
      return Future.failedFuture("Amount must be greater than zero");
    }

    // Check if company can create subscription
    return canCreateSubscription(compagnieId)
      .compose(canCreate -> {
        if (!canCreate) {
          return Future.failedFuture("Company already has an active subscription or prepaid balance");
        }

        // Get subscription type details
        return typeAbonnementRepository.findById(typeAbonnementId);
      })
      .compose(typeAbonnement -> {
        if (typeAbonnement == null || !typeAbonnement.getActif()) {
          return Future.failedFuture("Invalid or inactive subscription type");
        }

        // Only allow AVANCE or CAUTION
        if (!"AVANCE".equals(typeAbonnement.getCategorie()) &&
          !"CAUTION".equals(typeAbonnement.getCategorie())) {
          return Future.failedFuture("Only AVANCE or CAUTION subscriptions can be created");
        }

        // Create abonnement record with devise
        Abonnement abonnement = new Abonnement();
        abonnement.setCompagnieId(compagnieId);
        abonnement.setTypeabonnementId(typeAbonnementId);
        abonnement.setDateAbonnement(LocalDate.now());
        abonnement.setMontant(montant);
        abonnement.setType(typeAbonnement.getCategorie());
        abonnement.setStatut("ACTIF");
        abonnement.setDevise("MAD"); // SIMPLE CHANGE: Always MAD

        // Calculate end date if duration specified
        if (typeAbonnement.getDuree() != null && typeAbonnement.getDuree() > 0) {
          LocalDate endDate = calculateEndDate(
            LocalDate.now(),
            typeAbonnement.getDuree(),
            typeAbonnement.getUnite()
          );
          abonnement.setDateFin(endDate);
        }

        return abonnementRepository.save(abonnement)
          .compose(abonnementId -> {
            // For CAUTION, also create solde_prepaye record with devise
            if ("CAUTION".equals(typeAbonnement.getCategorie())) {
              return createCautionBalance(compagnieId, montant, abonnementId, devise);
            }

            // For AVANCE, just return the subscription info
            return Future.succeededFuture(new JsonObject()
              .put("abonnement_id", abonnementId)
              .put("type", typeAbonnement.getCategorie())
              .put("montant", montant)
              .put("devise", devise));
          })
          .map(result -> {
            result.put("compagnie_id", compagnieId);
            result.put("type_libelle", typeAbonnement.getLibelle());
            result.put("devise", devise);
            result.put("message", "Subscription created successfully");
            return result;
          });
      });
  }

  @Override
  public Future<JsonObject> getCompanySubscription(Long compagnieId) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(abonnement -> {
        if (abonnement == null) {
          // Check if it's a PREPAYE company
          return soldePrePayeRepository.findByCompagnieId(compagnieId)
            .map(solde -> {
              if (solde != null && "PREPAYE".equals(solde.getType())) {
                return new JsonObject()
                  .put("type", "PREPAYE")
                  .put("message", "Company uses prepaid model");
              }
              return new JsonObject()
                .put("message", "No active subscription found");
            });
        }

        // Return subscription details
        return enrichSubscriptionData(abonnement);
      });
  }

  @Override
  public Future<JsonArray> getAllSubscriptions() {
    return abonnementRepository.findAll()
      .compose(abonnements -> {
        // Filter only AVANCE and CAUTION
        List<Future<JsonObject>> futures = new ArrayList<>();

        for (Abonnement abonnement : abonnements) {
          if ("AVANCE".equals(abonnement.getType()) ||
            "CAUTION".equals(abonnement.getType())) {
            futures.add(enrichSubscriptionData(abonnement));
          }
        }

        return Future.all(futures).map(v -> {
          JsonArray array = new JsonArray();
          futures.forEach(f -> {
            if (f.result() != null) {
              array.add(f.result());
            }
          });
          return array;
        });
      });
  }

  @Override
  public Future<JsonArray> getSubscriptionsByCategory(String category) {
    if (!"AVANCE".equals(category) && !"CAUTION".equals(category)) {
      return Future.failedFuture("Invalid category. Must be AVANCE or CAUTION");
    }

    return abonnementRepository.findByType(category)
      .compose(abonnements -> {
        List<Future<JsonObject>> futures = new ArrayList<>();

        for (Abonnement abonnement : abonnements) {
          futures.add(enrichSubscriptionData(abonnement));
        }

        return Future.all(futures).map(v -> {
          JsonArray array = new JsonArray();
          futures.forEach(f -> {
            if (f.result() != null) {
              array.add(f.result());
            }
          });
          return array;
        });
      });
  }

  @Override
  public Future<JsonObject> updateSubscription(Long id, JsonObject data) {
    return abonnementRepository.findById(id)
      .compose(abonnement -> {
        if (abonnement == null) {
          return Future.failedFuture("Subscription not found");
        }

        // Only allow updating AVANCE or CAUTION subscriptions
        if (!"AVANCE".equals(abonnement.getType()) &&
          !"CAUTION".equals(abonnement.getType())) {
          return Future.failedFuture("Can only update AVANCE or CAUTION subscriptions");
        }

        // Update allowed fields
        boolean updated = false;

        if (data.containsKey("montant")) {
          BigDecimal newMontant = new BigDecimal(data.getString("montant"));
          if (newMontant.compareTo(BigDecimal.ZERO) > 0) {
            abonnement.setMontant(newMontant);
            updated = true;
          }
        }

        if (data.containsKey("date_fin")) {
          abonnement.setDateFin(LocalDate.parse(data.getString("date_fin")));
          updated = true;
        }

        if (!updated) {
          return Future.failedFuture("No valid fields to update");
        }

        return abonnementRepository.update(id, abonnement)
          .map(v -> new JsonObject()
            .put("id", id)
            .put("message", "Subscription updated successfully"));
      });
  }

  @Override
  public Future<Boolean> canCreateSubscription(Long compagnieId) {
    // Check if company already has an active subscription
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(existingAbonnement -> {
        if (existingAbonnement != null) {
          return Future.succeededFuture(false);
        }

        // Check if company has a PREPAYE balance
        return soldePrePayeRepository.findByCompagnieId(compagnieId)
          .map(solde -> {
            // Company can create subscription if no prepaid balance exists
            // or if the existing balance is not PREPAYE type
            return solde == null || !"PREPAYE".equals(solde.getType());
          });
      });
  }

  private Future<JsonObject> createCautionBalance(Long compagnieId, BigDecimal depositAmount, Long abonnementId, String devise) {
    // Create solde_prepaye record for CAUTION with devise
    SoldePrepaye solde = new SoldePrepaye();
    solde.setCompagnieId(compagnieId);
    solde.setSolde(depositAmount);
    solde.setType("CAUTION");
    solde.setStatut("ACTIF");
    solde.setDevise("MAD"); // SIMPLE CHANGE: Always MAD

    return soldePrePayeRepository.save(solde)
      .compose(soldeId -> {
        // Create initial transaction for deposit
        Transaction transaction = new Transaction();
        transaction.setCompagnieId(compagnieId);
        transaction.setType("DEPOT");
        transaction.setMontant(depositAmount);
        transaction.setSoldeAvant(BigDecimal.ZERO);
        transaction.setSoldeApres(depositAmount);
        transaction.setDescription("Dépôt de garantie initial - MAD"); // SIMPLE CHANGE

        return transactionRepository.save(transaction)
          .map(transactionId -> new JsonObject()
            .put("abonnement_id", abonnementId)
            .put("solde_id", soldeId)
            .put("transaction_id", transactionId)
            .put("type", "CAUTION")
            .put("montant", depositAmount)
            .put("devise", "MAD")); // Already MAD
      });
  }

  private Future<JsonObject> enrichSubscriptionData(Abonnement abonnement) {
    JsonObject json = new JsonObject()
      .put("id", abonnement.getId())
      .put("compagnie_id", abonnement.getCompagnieId())
      .put("type", abonnement.getType())
      .put("montant", abonnement.getMontant())
      .put("statut", abonnement.getStatut())
      .put("devise", "MAD") // SIMPLE CHANGE: Always show MAD
      .put("date_abonnement", abonnement.getDateAbonnement().toString());

    if (abonnement.getDateFin() != null) {
      json.put("date_fin", abonnement.getDateFin().toString());

      // Check if expired
      if (LocalDate.now().isAfter(abonnement.getDateFin())) {
        json.put("is_expired", true);
      }
    }

    // Add type details if available
    if (abonnement.getTypeAbonnement() != null) {
      json.put("type_libelle", abonnement.getTypeAbonnement().getLibelle());
      json.put("type_description", abonnement.getTypeAbonnement().getDescription());
    }

    // Add company name if available
    if (abonnement.getCompagnie() != null) {
      json.put("compagnie_name", abonnement.getCompagnie().getRaison_social());
    }

    // For CAUTION, add deposit balance info
    if ("CAUTION".equals(abonnement.getType())) {
      return soldePrePayeRepository.findByCompagnieId(abonnement.getCompagnieId())
        .map(solde -> {
          if (solde != null) {
            json.put("solde_disponible", solde.getSolde());
            json.put("depot_initial", abonnement.getMontant());
            json.put("depot_utilise",
              abonnement.getMontant().subtract(solde.getSolde()));
            // SIMPLE CHANGE: Always MAD
            json.put("devise", "MAD");
          }
          return json;
        });
    }

    return Future.succeededFuture(json);
  }

  private LocalDate calculateEndDate(LocalDate startDate, Double duration, String unit) {
    int dur = duration.intValue();

    switch (unit) {
      case "JOURS":
        return startDate.plusDays(dur);
      case "MOIS":
        return startDate.plusMonths(dur);
      case "ANNEES":
        return startDate.plusYears(dur);
      default:
        return startDate.plusMonths(dur); // Default to months
    }
  }

  @Override
  public Future<JsonObject> changeStatus(Long subscriptionId, String newStatus, String reason) {
    // Validate status
    if (!AbonnementStatus.isValidStatus(newStatus)) {
      return Future.failedFuture("Invalid status: " + newStatus);
    }

    return abonnementRepository.findById(subscriptionId)
      .compose(abonnement -> {
        if (abonnement == null) {
          return Future.failedFuture("Subscription not found");
        }

        // Only for AVANCE/CAUTION
        if (!"AVANCE".equals(abonnement.getType()) && !"CAUTION".equals(abonnement.getType())) {
          return Future.failedFuture("Status management only for AVANCE/CAUTION subscriptions");
        }

        String currentStatus = abonnement.getStatut();

        // Validate transition
        if (!isValidTransition(currentStatus, newStatus)) {
          return Future.failedFuture(
            String.format("Cannot transition from %s to %s", currentStatus, newStatus)
          );
        }

        // Update status
        abonnement.setStatut(newStatus);
        return abonnementRepository.update(subscriptionId, abonnement)
          .map(v -> new JsonObject()
            .put("subscription_id", subscriptionId)
            .put("previous_status", currentStatus)
            .put("new_status", newStatus)
            .put("reason", reason)
            .put("timestamp", LocalDate.now().toString()));
      });
  }

  @Override
  public Future<JsonObject> suspendSubscription(Long subscriptionId, String reason) {
    return changeStatus(subscriptionId, AbonnementStatus.SUSPENDU, reason);
  }

  @Override
  public Future<JsonObject> reactivateSubscription(Long subscriptionId) {
    return abonnementRepository.findById(subscriptionId)
      .compose(abonnement -> {
        if (abonnement == null) {
          return Future.failedFuture("Subscription not found");
        }

        // Can only reactivate from SUSPENDU or EXPIRE
        if (!AbonnementStatus.SUSPENDU.equals(abonnement.getStatut()) &&
          !AbonnementStatus.EXPIRE.equals(abonnement.getStatut())) {
          return Future.failedFuture("Can only reactivate suspended or expired subscriptions");
        }

        return changeStatus(subscriptionId, AbonnementStatus.ACTIF, "Reactivation");
      });
  }

  @Override
  public Future<JsonObject> expireSubscription(Long subscriptionId) {
    return changeStatus(subscriptionId, AbonnementStatus.EXPIRE, "Expiration");
  }

  @Override
  public Future<JsonObject> terminateSubscription(Long subscriptionId, String reason) {
    return changeStatus(subscriptionId, AbonnementStatus.RESILIE, reason);
  }

  private boolean isValidTransition(String from, String to) {
    if (from == null || from.equals(to)) return false;

    // RESILIE is final
    if (AbonnementStatus.RESILIE.equals(from)) {
      return false;
    }

    switch (from) {
      case AbonnementStatus.ACTIF:
        return true; // Can go to any status
      case AbonnementStatus.SUSPENDU:
        return AbonnementStatus.ACTIF.equals(to) || AbonnementStatus.RESILIE.equals(to);
      case AbonnementStatus.EXPIRE:
        return AbonnementStatus.ACTIF.equals(to) || AbonnementStatus.RESILIE.equals(to);
      default:
        return false;
    }
  }

  // C) CREDIT MANAGEMENT FOR AVANCE

  @Override
  public Future<Boolean> canUseCredit(Long compagnieId, BigDecimal amount) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .map(abonnement -> {
        if (abonnement == null || !"AVANCE".equals(abonnement.getType())) {
          return false;
        }

        // Check if subscription is active
        if (!AbonnementStatus.ACTIF.equals(abonnement.getStatut())) {
          return false;
        }

        // For now, just check against the credit limit
        // In a real system, you'd track used credit separately
        return amount.compareTo(abonnement.getMontant()) <= 0;
      });
  }

  @Override
  public Future<JsonObject> useCredit(Long compagnieId, BigDecimal amount, String description) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(abonnement -> {
        if (abonnement == null || !"AVANCE".equals(abonnement.getType())) {
          return Future.failedFuture("No active AVANCE subscription found");
        }

        if (!AbonnementStatus.ACTIF.equals(abonnement.getStatut())) {
          return Future.failedFuture("Subscription is not active");
        }

        // Check credit limit
        if (amount.compareTo(abonnement.getMontant()) > 0) {
          return Future.failedFuture("Amount exceeds credit limit");
        }

        // Record credit usage as transaction
        Transaction transaction = new Transaction();
        transaction.setCompagnieId(compagnieId);
        transaction.setType("CREDIT_USAGE");
        transaction.setMontant(amount);
        transaction.setSoldeAvant(BigDecimal.ZERO); // Credit doesn't affect balance
        transaction.setSoldeApres(BigDecimal.ZERO);
        transaction.setDescription(description);

        return transactionRepository.save(transaction)
          .map(transactionId -> new JsonObject()
            .put("transaction_id", transactionId)
            .put("amount_used", amount)
            .put("credit_limit", abonnement.getMontant())
            .put("description", description)
            .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
            .put("status", "SUCCESS"));
      });
  }

  @Override
  public Future<JsonObject> getCreditUsage(Long compagnieId) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(abonnement -> {
        if (abonnement == null || !"AVANCE".equals(abonnement.getType())) {
          return Future.failedFuture("No active AVANCE subscription found");
        }

        // Get current month's credit usage
        String sql = """
                SELECT COALESCE(SUM(montant), 0) as total_used
                FROM transaction
                WHERE compagnie_id = $1
                AND type = 'CREDIT_USAGE'
                AND date_transaction >= date_trunc('month', CURRENT_DATE)
                AND date_transaction < date_trunc('month', CURRENT_DATE) + interval '1 month'
            """;

        return pgPool.preparedQuery(sql)
          .execute(Tuple.of(compagnieId))
          .map(rows -> {
            BigDecimal totalUsed = rows.iterator().next().getBigDecimal("total_used");
            BigDecimal creditLimit = abonnement.getMontant();
            BigDecimal available = creditLimit.subtract(totalUsed);

            return new JsonObject()
              .put("credit_limit", creditLimit)
              .put("used_this_month", totalUsed)
              .put("available_credit", available)
              .put("usage_percentage",
                totalUsed.divide(creditLimit, 2, RoundingMode.HALF_UP)
                  .multiply(new BigDecimal("100")))
              .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
              .put("subscription_status", abonnement.getStatut());
          });
      });
  }

  // D) DEPOSIT MANAGEMENT FOR CAUTION

  @Override
  public Future<Boolean> canUseDeposit(Long compagnieId, BigDecimal amount) {
    return soldePrePayeRepository.findByCompagnieId(compagnieId)
      .compose(solde -> {
        if (solde == null || !"CAUTION".equals(solde.getType())) {
          return Future.succeededFuture(false);
        }

        // Check subscription status
        return abonnementRepository.findActiveByCompagnieId(compagnieId)
          .map(abonnement -> {
            if (abonnement == null || !AbonnementStatus.ACTIF.equals(abonnement.getStatut())) {
              return false;
            }

            // Check if enough deposit available
            return solde.getSolde().compareTo(amount) >= 0;
          });
      });
  }

  @Override
  public Future<JsonObject> useDeposit(Long compagnieId, BigDecimal amount, String description) {
    return canUseDeposit(compagnieId, amount)
      .compose(canUse -> {
        if (!canUse) {
          return Future.failedFuture("Cannot use deposit: insufficient balance or inactive subscription");
        }

        return soldePrePayeRepository.findByCompagnieId(compagnieId)
          .compose(solde -> {
            BigDecimal oldBalance = solde.getSolde();
            BigDecimal newBalance = oldBalance.subtract(amount);

            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setCompagnieId(compagnieId);
            transaction.setType("DEPOT_USAGE");
            transaction.setMontant(amount);
            transaction.setSoldeAvant(oldBalance);
            transaction.setSoldeApres(newBalance);
            transaction.setDescription(description);

            return transactionRepository.save(transaction)
              .compose(transactionId -> {
                // Update deposit balance
                return soldePrePayeRepository.updateSolde(compagnieId, newBalance)
                  .map(v -> new JsonObject()
                    .put("transaction_id", transactionId)
                    .put("amount_used", amount)
                    .put("old_balance", oldBalance)
                    .put("new_balance", newBalance)
                    .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
                    .put("status", "SUCCESS"));
              });
          });
      });
  }

  @Override
  public Future<JsonObject> releaseDeposit(Long compagnieId, BigDecimal amount, String reason) {
    return soldePrePayeRepository.findByCompagnieId(compagnieId)
      .compose(solde -> {
        if (solde == null || !"CAUTION".equals(solde.getType())) {
          return Future.failedFuture("No CAUTION deposit found");
        }

        BigDecimal oldBalance = solde.getSolde();
        BigDecimal newBalance = oldBalance.add(amount);

        // Get original deposit amount from subscription
        return abonnementRepository.findActiveByCompagnieId(compagnieId)
          .compose(abonnement -> {
            if (abonnement == null) {
              return Future.failedFuture("No active subscription found");
            }

            // Don't allow releasing more than original deposit
            if (newBalance.compareTo(abonnement.getMontant()) > 0) {
              return Future.failedFuture("Cannot release more than original deposit amount");
            }

            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setCompagnieId(compagnieId);
            transaction.setType("DEPOT_RELEASE");
            transaction.setMontant(amount);
            transaction.setSoldeAvant(oldBalance);
            transaction.setSoldeApres(newBalance);
            transaction.setDescription(reason);

            return transactionRepository.save(transaction)
              .compose(transactionId -> {
                // Update balance
                return soldePrePayeRepository.updateSolde(compagnieId, newBalance)
                  .map(v -> new JsonObject()
                    .put("transaction_id", transactionId)
                    .put("amount_released", amount)
                    .put("new_balance", newBalance)
                    .put("original_deposit", abonnement.getMontant())
                    .put("status", "SUCCESS"));
              });
          });
      });
  }

  @Override
  public Future<JsonObject> getDepositStatus(Long compagnieId) {
    return abonnementRepository.findActiveByCompagnieId(compagnieId)
      .compose(abonnement -> {
        if (abonnement == null || !"CAUTION".equals(abonnement.getType())) {
          return Future.failedFuture("No active CAUTION subscription found");
        }

        return soldePrePayeRepository.findByCompagnieId(compagnieId)
          .map(solde -> {
            BigDecimal originalDeposit = abonnement.getMontant();
            BigDecimal currentBalance = solde != null ? solde.getSolde() : BigDecimal.ZERO;
            BigDecimal usedAmount = originalDeposit.subtract(currentBalance);

            return new JsonObject()
              .put("original_deposit", originalDeposit)
              .put("current_balance", currentBalance)
              .put("used_amount", usedAmount)
              .put("usage_percentage",
                usedAmount.divide(originalDeposit, 2, RoundingMode.HALF_UP)
                  .multiply(new BigDecimal("100")))
              .put("devise", "MAD") // SIMPLE CHANGE: Add MAD
              .put("subscription_status", abonnement.getStatut());
          });
      });
  }
}
