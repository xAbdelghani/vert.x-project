package com.example.starter.model;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@Setter
public class Transaction {

  private Long id;
  private Long compagnieId;
  private String type;           // CREDIT, DEBIT, DEPOSIT, REFUND
  private BigDecimal montant;
  private BigDecimal soldeAvant;
  private BigDecimal soldeApres;
  private String description;
  private LocalDateTime dateTransaction;
  private Long userId;           // Who performed the transaction
  private String reference;
  // Invoice number, attestation ID, etc.
}
