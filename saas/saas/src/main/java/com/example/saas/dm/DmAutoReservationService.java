package com.example.saas.dm;

import com.example.saas.api.error.BadRequestException;
import com.example.saas.api.error.ReservationConflictException;
import com.example.saas.billing.PlanLimitService;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.domain.Customer;
import com.example.saas.domain.DmIntent;
import com.example.saas.domain.Organization;
import com.example.saas.domain.Reservation;
import com.example.saas.domain.ReservationSource;
import com.example.saas.repo.CustomerRepository;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceRepository;
import com.example.saas.reservation.ReservationUpdateRequest;
import com.example.saas.security.JwtPrincipal;
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
    private static final Pattern PHONE_PATTERN = Pattern.compile("(01[016789])[- ]?(\\d{3,4})[- ]?(\\d{4})");
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

        OffsetDateTime startAt = parseStartAt(req.message(), zoneId);
        if (startAt == null) {
            return new DmReservationResponse(false, customerResolution.created(), "예약 시간을 이해하지 못했습니다. 예: 3월 15일 14시", null, customer.getId(), null, null, null, List.of());
        }

        DmIntent intent = inferIntent(req.message());
        if (intent == DmIntent.CANCEL) {
            return new DmReservationResponse(false, customerResolution.created(), "예약 취소 요청으로 분류되었습니다. 기존 예약을 선택해 취소를 확정해주세요.", null, customer.getId(), null, startAt, null, List.of());
        }
        if (intent == DmIntent.UPDATE) {
            return new DmReservationResponse(false, customerResolution.created(), "예약 변경 요청으로 분류되었습니다. 기존 예약을 선택해 변경을 확정해주세요.", null, customer.getId(), null, startAt, null, List.of());
        }

        com.example.saas.domain.Service service = resolveService(orgId, req);
        if (service == null) {
            return new DmReservationResponse(false, customerResolution.created(), "서비스를 찾지 못했습니다. 서비스명을 다시 확인해주세요.", null, customer.getId(), null, startAt, null, List.of());
        }

        int durationMinutes = service.getDurationMinutes();
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
                    service.getId(),
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
            return new DmReservationResponse(true, customerResolution.created(), reply, reservationId, customer.getId(), service.getId(), startAt, endAt, List.of());
        } catch (ReservationConflictException e) {
            List<OffsetDateTime> suggestions = suggestNextSlots(orgId, startAt, durationMinutes, zoneId);
            String reply = suggestions.isEmpty()
                    ? "해당 시간대에 이미 예약이 있습니다. 다른 시간을 제안해주세요."
                    : "해당 시간대는 이미 예약이 있습니다. 가능한 시간: " + formatSuggestions(suggestions, zoneId);
            return new DmReservationResponse(false, customerResolution.created(), reply, null, customer.getId(), service.getId(), startAt, endAt, suggestions);
        }
    }

    public UUID confirmManualReservation(UUID orgId,
                                         UUID actorUserId,
                                         UUID customerId,
                                         UUID serviceId,
                                         OffsetDateTime startAt,
                                         OffsetDateTime endAt,
                                         String notes) {
        return reservationService.create(
                orgId,
                customerId,
                serviceId,
                startAt,
                endAt,
                actorUserId,
                notes,
                ReservationSource.DM
        );
    }

    public void cancelManualReservation(UUID orgId, UUID actorUserId, UUID reservationId) {
        reservationService.cancel(reservationId, new JwtPrincipal(actorUserId, "dm-review@local", orgId));
    }

    public void updateManualReservation(UUID orgId,
                                        UUID actorUserId,
                                        UUID reservationId,
                                        UUID customerId,
                                        UUID serviceId,
                                        OffsetDateTime startAt,
                                        OffsetDateTime endAt,
                                        String notes) {
        reservationService.update(
                reservationId,
                new ReservationUpdateRequest(customerId, serviceId, startAt, endAt, notes),
                new JwtPrincipal(actorUserId, "dm-review@local", orgId)
        );
    }

    public DmIntent inferIntent(String message) {
        String normalized = normalizeText(message);
        if (normalized.contains("취소")) {
            return DmIntent.CANCEL;
        }
        if (normalized.contains("변경") || normalized.contains("바꿔") || normalized.contains("옮겨") || normalized.contains("미뤄")) {
            return DmIntent.UPDATE;
        }
        return DmIntent.BOOK;
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
        for (String phoneCandidate : Arrays.asList(req.senderPhone(), inferPhoneFromMessage(req.message()))) {
            if (phoneCandidate == null || phoneCandidate.isBlank()) {
                continue;
            }
            Customer byPhone = findCustomerByPhone(orgId, phoneCandidate);
            if (byPhone != null) {
                return new CustomerResolution(byPhone, false);
            }
        }

        for (String hint : Arrays.asList(req.customerHint(), req.senderName(), inferCustomerNameFromMessage(req.message()))) {
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
        customer.setPhone(blankToNull(firstNonBlank(req.senderPhone(), inferPhoneFromMessage(req.message()))));
        customer.setMemo("[DM AUTO-CREATED]");
        customerRepository.save(customer);
        return new CustomerResolution(customer, true);
    }

    private com.example.saas.domain.Service resolveService(UUID orgId, DmReservationRequest req) {
        List<com.example.saas.domain.Service> activeServices = serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .toList();
        com.example.saas.domain.Service inferredByMessage = findBestMatchingService(activeServices, req.message());
        if (inferredByMessage != null) {
            return inferredByMessage;
        }

        String effectiveHint = firstNonBlank(req.serviceHint(), inferServiceHintFromMessage(activeServices, req.message()));
        if (effectiveHint == null || effectiveHint.isBlank()) {
            return activeServices.size() == 1 ? activeServices.get(0) : null;
        }
        com.example.saas.domain.Service inferredByHint = findBestMatchingService(activeServices, effectiveHint);
        if (inferredByHint != null) {
            return inferredByHint;
        }
        List<com.example.saas.domain.Service> candidates = serviceRepository.searchByOrganizationId(orgId, effectiveHint.trim());
        return candidates.stream()
                .filter(service -> Boolean.TRUE.equals(service.getActive()))
                .findFirst()
                .orElse(null);
    }

    private String pickCustomerName(DmReservationRequest req) {
        for (String candidate : Arrays.asList(req.senderName(), req.customerHint(), inferCustomerNameFromMessage(req.message()))) {
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

    private Customer findCustomerByPhone(UUID orgId, String phoneCandidate) {
        String normalized = normalizePhone(phoneCandidate);
        if (normalized.isBlank()) {
            return null;
        }
        return customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .filter(customer -> normalizePhone(customer.getPhone()).equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private String inferPhoneFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        Matcher matcher = PHONE_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + matcher.group(2) + matcher.group(3);
    }

    private String inferCustomerNameFromMessage(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        String candidate = message;
        candidate = candidate.replaceAll("(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}).*", "");
        candidate = candidate.replaceAll("(\\d{1,2})월\\s*(\\d{1,2})일\\s*(\\d{1,2})시(?:\\s*(\\d{1,2})분)?.*", "");
        candidate = candidate.replaceAll("(\\d{1,2})/(\\d{1,2})\\s*(\\d{1,2}):(\\d{2}).*", "");
        candidate = candidate.replaceAll("(예약.*|잡아.*|원해.*|부탁.*)$", "");
        candidate = candidate.trim();
        if (candidate.isBlank()) {
            return null;
        }
        String[] tokens = candidate.split("\\s+");
        if (tokens.length == 0) {
            return null;
        }
        String first = tokens[0].trim();
        if (first.matches(".*\\d.*")) {
            return null;
        }
        return first.length() > 20 ? first.substring(0, 20) : first;
    }

    private String inferServiceHintFromMessage(List<com.example.saas.domain.Service> activeServices, String message) {
        return activeServices.stream()
                .map(com.example.saas.domain.Service::getName)
                .filter(name -> name != null && !name.isBlank())
                .filter(name -> {
                    String normalizedMessage = normalizeText(message);
                    String normalizedName = normalizeText(name);
                    return !normalizedMessage.isBlank()
                            && !normalizedName.isBlank()
                            && (normalizedMessage.contains(normalizedName) || normalizedName.contains(normalizedMessage));
                })
                .findFirst()
                .orElse(null);
    }

    private com.example.saas.domain.Service findBestMatchingService(List<com.example.saas.domain.Service> activeServices, String text) {
        String normalizedText = normalizeText(text);
        if (normalizedText.isBlank()) {
            return null;
        }
        return activeServices.stream()
                .filter(service -> {
                    String normalizedName = normalizeText(service.getName());
                    return !normalizedName.isBlank()
                            && (normalizedText.contains(normalizedName) || normalizedName.contains(normalizedText));
                })
                .sorted(Comparator.comparingInt((com.example.saas.domain.Service service) -> normalizeText(service.getName()).length()).reversed())
                .findFirst()
                .orElse(null);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("\\s+", "")
                .replaceAll("[^0-9a-z가-힣]", "");
    }

    private record CustomerResolution(Customer customer, boolean created) {
    }
}
