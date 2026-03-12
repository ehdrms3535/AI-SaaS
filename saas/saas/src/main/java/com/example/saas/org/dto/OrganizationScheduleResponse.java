package com.example.saas.org.dto;

import java.time.LocalTime;
import java.util.List;

public record OrganizationScheduleResponse(
        LocalTime businessOpenTime,
        LocalTime businessCloseTime,
        List<String> closedWeekdays
) {
}
