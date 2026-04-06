package com.michael.global_logistics_api.logistics.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "tb_shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private String originCountry;

    @Column(nullable = false)
    private String destinationCountry;

    @Column(nullable = false)
    private Double weight;

    @Column(nullable = false)
    private BigDecimal insuranceValue;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = ShipmentStatus.PENDING;
    }
}