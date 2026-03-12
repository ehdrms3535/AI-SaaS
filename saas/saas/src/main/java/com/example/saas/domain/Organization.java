package com.example.saas.domain;

import com.example.saas.billing.OrganizationPlan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String slug;
    
    @Column(nullable = false)
    private String name;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private OrganizationPlan plan;

    @Column(name = "business_open_time", nullable = false)
    private LocalTime businessOpenTime;

    @Column(name = "business_close_time", nullable = false)
    private LocalTime businessCloseTime;

    @Column(name = "closed_weekdays", nullable = false)
    private String closedWeekdays;
}
