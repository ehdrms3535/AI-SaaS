package com.example.saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchOrgRequest(
	@JsonAlias({"orgId", "organizationId"}) @NotNull UUID orgId
) {}