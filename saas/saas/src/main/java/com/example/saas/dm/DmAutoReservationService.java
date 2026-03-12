package com.example.saas.dm;

import com.example.saas.api.error.BadRequestException;
import com.example.saas.api.error.ReservationConflictException;
import com.example.saas.billing.PlanLimitService;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.domain.Customer;
import com.example.saas.domain.Organization;
import com.example.saas.domain.Reservation;
import com.example.saas.domain.ReservationSource;
import com.example.saas.repo.CustomerRepository;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceRepository;
import com.example.saas.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.*;
import java.util.Arrays;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class DmAutoReservationService {

    private static final Pattern KOREAN_DATE_TIME = Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?");
    private static final Pattern SLASH_DATE_TIME = Pattern.compile("(\\d{1,2})/(\\d{1,2})\\s*(\\d{1,2}):(\\d{2})");
    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm");

    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final OrganizationRepository organizationRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final PlanLimitService planLimitService;
    private final Clock clock;

    public DmReservationResponse reserve(UUID orgId, UUID actorUserId, DmReservationRequest req) {
        if (req == null || req.message() == null || req.message().isBlank()) {
            throw new BadRequestException("DM_MESSAGE_REQUIRED", "DM 메시지는 필수입니다.");
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new BadRequestException("ORG_NOT_FOUND", "조직을 찾을 수 없습니다."));
        ZoneId zoneId = resolveZone(organization.getTimezone());

        CustomerResolution customerResolution = resolveCustomer(orgId, req);
        Customer customer = customerResolution.customer();
        if (customer == null) {
            return new DmReservationResponse(false, false, "고객을 찾지 못했습니다. 이름 또는 전화번호를 더 정확히 보내주세요.", null, null, null, null, null, List.of());
        }

        com.example.saas.domain.Service service = resolveService(orgId, req.serviceHint());
        if (req.serviceHint() != null && !req.serviceHint().isBlank() && service == null) {
            return new DmReservationResponse(false, customerResolution.created(), "서비스를 찾지 못했습니다. 서비스명을 다시 확인해주세요.", null, customer.getId(), null, null, null, List.of());
        }

        OffsetDateTime startAt = parseStartAt(req.message(), zoneId);
        if (startAt == null) {
            return new DmReservationResponse(false, customerResolution.created(), "예약 시간을 이해하지 못했습니다. 예: 3월 15일 14시", null, customer.getId(), service == null ? null : service.getId(), null, null, List.of());
        }

        int durationMinutes = service != null
                ? service.getDurationMinutes()
                : (req.durationMinutes() != null ? req.durationMinutes() : 60);
        OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);

        if (isClosedDay(organization, startAt, zoneId)) {
            return new DmReservationResponse(false, customerResolution.created(), "해당 날짜는 휴무일이라 예약할 수 없습니다.", null, customer.getId(), service == null ? null : service.getId(), startAt, endAt, List.of());
        }
        if (!isWithinBusinessHours(organization, startAt, endAt, zoneId)) {
            String hours = "%s~%s".formatted(organization.getBusinessOpenTime(), organization.getBusinessCloseTime());
            return new DmReservationResponse(false, customerResolution.created(), "영업시간 밖 요청입니다. 현재 영업시간은 " + hours + " 입니다.", null, customer.getId(), service == null ? null : service.getId(), startAt, endAt, List.of());
        }

        try {
            UUID reservationId = reservationService.create(
                    orgId,
                    customer.getId(),
                    service == null ? null : service.getId(),
                    startAt,
                    endAt,
                    actorUserId,
                    "[DM] " + req.message(),
                    ReservationSource.DM
            );
            String reply = "%s님 예약이 %s로 등록됐습니다.".formatted(
                    customer.getName(),
                    startAt.toLocalDateTime().toString().replace('T', ' ')
            );
            return new DmReservationResponse(true, customerResolution.created(), reply, reservationId, customer.getId(), service == null ? null : service.getId(), startAt, endAt, List.of());
        } catch (ReservationConflictException e) {
            List<OffsetDateTime> suggestions = suggestNextSlots(orgId, startAt, durationMinutes, zoneId);
            String reply = suggestions.isEmpty()
                    ? "해당 시간대에 이미 예약이 있습니다. 다른 시간을 제안해주세요."
                    : "해당 시간대는 이미 예약이 있습니다. 가능한 시간: " + formatSuggestions(suggestions, zoneId);
            return new DmReservationResponse(false, customerResolution.created(), reply, null, customer.getId(), service == null ? null : service.getId(), startAt, endAt, suggestions);
        }
    }

    private List<OffsetDateTime> suggestNextSlots(UUID orgId, OffsetDateTime requestedStartAt, int durationMinutes, ZoneId zoneId) {
        OffsetDateTime searchStart = floorToHalfHour(requestedStartAt);
        OffsetDateTime searchEnd = searchStart.plusDays(2);
        List<Reservation> reservations = reservationRepository.findActiveOverlapsInWindow(orgId, searchStart, searchEnd);

        java.util.ArrayList<OffsetDateTime> suggestions = new java.util.ArrayList<>();
        OffsetDateTime candidate = searchStart.plusMinutes(30);
        OffsetDateTime now = OffsetDateTime.now(clock.withZone(zoneId));

        while (candidate.isBefore(searchEnd) && suggestions.size() < 3) {
            OffsetDateTime candidateEnd = candidate.plusMinutes(durationMinutes);
            OffsetDateTime candidateStart = candidate;
            boolean overlap = reservations.stream().anyMatch(reservation ->
                    reservation.getStartAt().isBefore(candidateEnd) && reservation.getEndAt().isAfter(candidateStart)
            );
            if (!overlap && !candidate.isBefore(now.minusMinutes(1))) {
                suggestions.add(candidate);
            }
            candidate = candidate.plusMinutes(30);
        }
        return suggestions;
    }

    private OffsetDateTime floorToHalfHour(OffsetDateTime value) {
        int minute = value.getMinute();
        int flooredMinute = minute < 30 ? 0 : 30;
        return value.withMinute(flooredMinute).withSecond(0).withNano(0);
    }

    private String formatSuggestions(List<OffsetDateTime> suggestions, ZoneId zoneId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d HH:mm");
        return suggestions.stream()
                .map(time -> time.atZoneSameInstant(zoneId).format(formatter))
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private CustomerResolution resolveCustomer(UUID orgId, DmReservationRequest req) {
        if (req.senderPhone() != null && !req.senderPhone().isBlank()) {
            String normalized = normalizePhone(req.senderPhone());
            List<Customer> byPhone = customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                    .filter(customer -> normalizePhone(customer.getPhone()).equals(normalized))
                    .toList();
            if (!byPhone.isEmpty()) {
                return new CustomerResolution(byPhone.get(0), false);
            }
        }

        for (String hint : Arrays.asList(req.customerHint(), req.senderName())) {
            if (hint == null || hint.isBlank()) {
                continue;
            }
            List<Customer> candidates = customerRepository.searchByOrganizationId(orgId, hint.trim());
            if (!candidates.isEmpty()) {
                Customer customer = candidates.stream()
                        .sorted(Comparator.comparing(Customer::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                        .findFirst()
                        .orElse(null);
                return new CustomerResolution(customer, false);
            }
        }

        String customerName = pickCustomerName(req);
        if (customerName == null) {
            return new CustomerResolution(null, false);
        }

        planLimitService.assertCanCreateCustomer(orgId);
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setOrganizationId(orgId);
        customer.setName(customerName);
        customer.setPhone(blankToNull(req.senderPhone()));
        customer.setMemo("[DM AUTO-CREATED]");
        customerRepository.save(customer);
        return new CustomerResolution(customer, true);
    }

    private com.example.saas.domain.Service resolveService(UUID orgId, String hint) {
        List<com.example.saas.domain.Service> activeServices = serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .toList();
        if (hint == null || hint.isBlank()) {
            return activeServices.size() == 1 ? activeServices.get(0) : null;
        }
        List<com.example.saas.domain.Service> candidates = serviceRepository.searchByOrganizationId(orgId, hint.trim());
        return candidates.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .findFirst()
                .orElse(null);
    }

    private String pickCustomerName(DmReservationRequest req) {
        for (String candidate : Arrays.asList(req.senderName(), req.customerHint())) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            String trimmed = candidate.trim();
            if (!trimmed.matches("[0-9\\-\\s]+")) {
                return trimmed;
            }
        }
        return null;
    }

    private OffsetDateTime parseStartAt(String message, ZoneId zoneId) {
        OffsetDateTime iso = tryParseIso(message, zoneId);
        if (iso != null) {
            return iso;
        }
        OffsetDateTime korean = tryParseKorean(message, zoneId);
        if (korean != null) {
            return korean;
        }
        return tryParseSlash(message, zoneId);
    }

    private OffsetDateTime tryParseIso(String message, ZoneId zoneId) {
        Matcher matcher = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2})").matcher(message);
        if (!matcher.find()) {
            return null;
        }
        try {
            LocalDateTime localDateTime = LocalDateTime.parse(matcher.group(1).replace(' ', 'T'), ISO_LOCAL);
            return localDateTime.atZone(zoneId).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private OffsetDateTime tryParseKorean(String message, ZoneId zoneId) {
        Matcher matcher = KOREAN_DATE_TIME.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        int year = Year.now(clock.withZone(zoneId)).getValue();
        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        int hour = Integer.parseInt(matcher.group(3));
        int minute = matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4));

        LocalDate date = LocalDate.of(year, month, day);
        ZonedDateTime zoned = ZonedDateTime.of(date, LocalTime.of(hour, minute), zoneId);
        if (zoned.isBefore(ZonedDateTime.now(clock.withZone(zoneId)).minusHours(1))) {
            zoned = zoned.plusYears(1);
        }
        return zoned.toOffsetDateTime();
    }

    private OffsetDateTime tryParseSlash(String message, ZoneId zoneId) {
        Matcher matcher = SLASH_DATE_TIME.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        int year = Year.now(clock.withZone(zoneId)).getValue();
        int month = Integer.parseInt(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));
        int hour = Integer.parseInt(matcher.group(3));
        int minute = Integer.parseInt(matcher.group(4));

        LocalDateTime localDateTime = LocalDateTime.of(year, month, day, hour, minute);
        ZonedDateTime zoned = localDateTime.atZone(zoneId);
        if (zoned.isBefore(ZonedDateTime.now(clock.withZone(zoneId)).minusHours(1))) {
            zoned = zoned.plusYears(1);
        }
        return zoned.toOffsetDateTime();
    }

    private ZoneId resolveZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception ignored) {
            return clock.getZone();
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    private boolean isClosedDay(Organization organization, OffsetDateTime startAt, ZoneId zoneId) {
        String closedWeekdays = organization.getClosedWeekdays();
        if (closedWeekdays == null || closedWeekdays.isBlank()) {
            return false;
        }
        String dayName = startAt.atZoneSameInstant(zoneId).getDayOfWeek().name();
        return List.of(closedWeekdays.split(",")).stream()
                .map(String::trim)
                .anyMatch(value -> value.equalsIgnoreCase(dayName));
    }

    private boolean isWithinBusinessHours(Organization organization, OffsetDateTime startAt, OffsetDateTime endAt, ZoneId zoneId) {
        LocalDate localDate = startAt.atZoneSameInstant(zoneId).toLocalDate();
        LocalDate endDate = endAt.atZoneSameInstant(zoneId).toLocalDate();
        if (!localDate.equals(endDate)) {
            return false;
        }
        LocalTime open = organization.getBusinessOpenTime();
        LocalTime close = organization.getBusinessCloseTime();
        LocalTime localStart = startAt.atZoneSameInstant(zoneId).toLocalTime();
        LocalTime localEnd = endAt.atZoneSameInstant(zoneId).toLocalTime();
        return !localStart.isBefore(open) && !localEnd.isAfter(close);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record CustomerResolution(Customer customer, boolean created) {
    }
}
