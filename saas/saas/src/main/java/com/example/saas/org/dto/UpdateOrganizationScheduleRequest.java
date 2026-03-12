package com.example.saas.org.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.List;

public record UpdateOrganizationScheduleRequest(
        @NotNull LocalTime businessOpenTime,
        @NotNull LocalTime businessCloseTime,
        @NotEmpty List<String> closedWeekdays
) {
}
