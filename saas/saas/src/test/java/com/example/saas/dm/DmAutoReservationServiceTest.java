package com.example.saas.dm;

import com.example.saas.billing.PlanLimitService;
import com.example.saas.dm.dto.DmReservationRequest;
import com.example.saas.dm.dto.DmReservationResponse;
import com.example.saas.domain.Customer;
import com.example.saas.domain.Organization;
import com.example.saas.domain.Reservation;
import com.example.saas.domain.Service;
import com.example.saas.repo.CustomerRepository;
import com.example.saas.repo.OrganizationRepository;
import com.example.saas.repo.ReservationRepository;
import com.example.saas.repo.ServiceRepository;
import com.example.saas.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DmAutoReservationServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private PlanLimitService planLimitService;

    private DmAutoReservationService dmAutoReservationService;
    private Clock clock;
    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-11T00:00:00Z"), ZoneOffset.UTC);
        dmAutoReservationService = new DmAutoReservationService(
                customerRepository,
                serviceRepository,
                organizationRepository,
                reservationRepository,
                reservationService,
                planLimitService,
                clock
        );
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(defaultOrganization()));
    }

    @Test
    void createsCustomerWhenNotFoundFromDm() {
        Service service = activeService("커트", 60);
        when(customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of());
        when(customerRepository.searchByOrganizationId(eq(orgId), any())).thenReturn(List.of());
        when(serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(service));
        when(serviceRepository.searchByOrganizationId(orgId, "커트")).thenReturn(List.of(service));
        when(reservationService.create(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(UUID.randomUUID());
        doAnswer(invocation -> invocation.getArgument(0)).when(customerRepository).save(any(Customer.class));

        DmReservationResponse response = dmAutoReservationService.reserve(
                orgId,
                userId,
                new DmReservationRequest("2026-03-12 14:00 커트 예약", null, "새고객", "010-9999-0000", "커트", 60)
        );

        assertThat(response.success()).isTrue();
        assertThat(response.customerCreated()).isTrue();

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("새고객");
        assertThat(captor.getValue().getPhone()).isEqualTo("010-9999-0000");
    }

    @Test
    void rejectsRequestOutsideBusinessHours() {
        Customer customer = customer("김고객", "01011112222");
        when(customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(customer));
        when(serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(activeService("기본", 60)));

        DmReservationResponse response = dmAutoReservationService.reserve(
                orgId,
                userId,
                new DmReservationRequest("2026-03-12 22:00 예약", null, null, "010-1111-2222", null, 60)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.reply()).contains("영업시간 밖");
    }

    @Test
    void rejectsRequestOnClosedWeekday() {
        Organization organization = defaultOrganization();
        organization.setClosedWeekdays("WEDNESDAY");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        Customer customer = customer("김고객", "01011112222");
        when(customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(customer));
        when(serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(activeService("기본", 60)));

        DmReservationResponse response = dmAutoReservationService.reserve(
                orgId,
                userId,
                new DmReservationRequest("2026-03-11 14:00 예약", null, null, "010-1111-2222", null, 60)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.reply()).contains("휴무일");
    }

    @Test
    void suggestsAlternativeTimesWhenSlotConflicts() {
        Customer customer = customer("김고객", "01011112222");
        Service service = activeService("기본", 60);
        when(customerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(customer));
        when(serviceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)).thenReturn(List.of(service));
        when(reservationService.create(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new com.example.saas.api.error.ReservationConflictException("충돌"));
        when(reservationRepository.findActiveOverlapsInWindow(any(), any(), any())).thenReturn(List.of(
                reservation("2026-03-12T14:00:00Z", "2026-03-12T15:00:00Z"),
                reservation("2026-03-12T15:30:00Z", "2026-03-12T16:30:00Z")
        ));

        DmReservationResponse response = dmAutoReservationService.reserve(
                orgId,
                userId,
                new DmReservationRequest("2026-03-12 14:00 예약", null, null, "010-1111-2222", null, 60)
        );

        assertThat(response.success()).isFalse();
        assertThat(response.suggestedStartTimes()).hasSize(3);
        assertThat(response.reply()).contains("가능한 시간");
    }

    private Organization defaultOrganization() {
        Organization organization = new Organization();
        organization.setId(orgId);
        organization.setTimezone("Asia/Seoul");
        organization.setBusinessOpenTime(LocalTime.of(9, 0));
        organization.setBusinessCloseTime(LocalTime.of(21, 0));
        organization.setClosedWeekdays("SUNDAY");
        return organization;
    }

    private Customer customer(String name, String normalizedPhone) {
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setOrganizationId(orgId);
        customer.setName(name);
        customer.setPhone(normalizedPhone);
        customer.setCreatedAt(OffsetDateTime.parse("2026-03-10T00:00:00Z"));
        return customer;
    }

    private Service activeService(String name, int durationMinutes) {
        Service service = new Service();
        service.setId(UUID.randomUUID());
        service.setOrganizationId(orgId);
        service.setName(name);
        service.setDurationMinutes(durationMinutes);
        service.setPrice(30000);
        service.setActive(true);
        return service;
    }

    private Reservation reservation(String startAt, String endAt) {
        Reservation reservation = new Reservation();
        reservation.setStartAt(OffsetDateTime.ofInstant(Instant.parse(startAt), ZoneId.of("UTC")));
        reservation.setEndAt(OffsetDateTime.ofInstant(Instant.parse(endAt), ZoneId.of("UTC")));
        return reservation;
    }
}
