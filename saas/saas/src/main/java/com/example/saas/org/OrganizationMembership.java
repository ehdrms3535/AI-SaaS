package com.example.saas.org;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "organization_memberships")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationMembership {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrgRole role;

    public enum OrgRole { OWNER, STAFF }
}