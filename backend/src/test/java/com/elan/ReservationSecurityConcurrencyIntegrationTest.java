package com.elan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.elan.dto.ReservationDtos.ReservationRequest;
import com.elan.entity.Administrator;
import com.elan.repository.AdministratorRepository;
import com.elan.repository.ReservationRepository;
import com.elan.service.ReservationService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReservationSecurityConcurrencyIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("elan").withUsername("elan").withPassword("elan_test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("elan.email.delivery-mode", () -> "log");
    }

    private void createAdministrator(String email, boolean active) {
        Administrator administrator = new Administrator();
        administrator.id = UUID.randomUUID();
        administrator.email = email;
        administrator.passwordHash = passwordEncoder.encode("correct-horse-battery");
        administrator.displayName = "Test Administrator";
        administrator.role = "ADMIN";
        administrator.active = active;
        administrator.createdAt = java.time.LocalDateTime.now();
        administrator.updatedAt = administrator.createdAt;
        administrators.save(administrator);
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired TestRestTemplate http;
    @Autowired MockMvc mvc;
    @Autowired AdministratorRepository administrators;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired ReservationService reservationService;
    @Autowired ReservationRepository reservations;

    @BeforeEach
    void oneEligibleTableAndDeterministicSettings() {
        jdbc.update("UPDATE dining_tables SET is_active = FALSE");
        jdbc.update("""
                UPDATE restaurant_settings SET restaurant_timezone = 'UTC', booking_enabled = TRUE,
                    maximum_advance_booking_days = 90, minimum_advance_booking_hours = 0,
                    minimum_party_size = 1, maximum_online_party_size = 8,
                    default_reservation_duration_minutes = 60
                """);
        jdbc.update("DELETE FROM restaurant_service_periods WHERE name = 'Concurrency test'");
        jdbc.update("""
                INSERT INTO dining_tables (id, table_number, display_name, minimum_capacity, maximum_capacity,
                    location, is_active, created_at, updated_at)
                VALUES (?, 'T-CONCURRENCY', 'Concurrency Test Table', 1, 2, 'MAIN_DINING', TRUE, NOW(), NOW())
                ON CONFLICT (table_number) DO UPDATE SET is_active = TRUE, minimum_capacity = 1, maximum_capacity = 2
                """, UUID.fromString("a1000000-0000-0000-0000-000000000001"));
    }

    @Test
    void concurrentRequestsForTheOnlyTableCannotBothBookTheSameInterval() throws Exception {
        List<LocalDate> dates = nextDates(LocalDate.now(ZoneId.of("UTC")).plusDays(14), 5);
        var pool = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < dates.size(); attempt++) {
                LocalDate date = dates.get(attempt);
                jdbc.update("""
                        INSERT INTO restaurant_service_periods
                        (id, name, day_of_week, start_time, end_time, slot_interval_minutes, dining_duration_minutes, is_active)
                        VALUES (?, 'Concurrency test', ?, '18:00', '22:00', 30, 60, TRUE)
                        """, UUID.randomUUID(), date.getDayOfWeek().getValue());
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);
                ReservationRequest first = request(date, "first-" + attempt + "@example.test", "First");
                ReservationRequest second = request(date, "second-" + attempt + "@example.test", "Second");
                int run = attempt;
                Future<ResponseEntity<String>> one = pool.submit(() -> sendBooking(first, ready, start, "book-key-000000000000-" + run));
                Future<ResponseEntity<String>> two = pool.submit(() -> sendBooking(second, ready, start, "book-key-111111111111-" + run));
                ready.await();
                start.countDown();
                List<Integer> statuses = List.of(one.get().getStatusCode().value(), two.get().getStatusCode().value());
                assertEquals(1, statuses.stream().filter(status -> status == HttpStatus.CREATED.value()).count());
                assertEquals(1, statuses.stream().filter(status -> status == HttpStatus.CONFLICT.value()).count());
                Integer active = jdbc.queryForObject("""
                        SELECT COUNT(*) FROM reservations
                        WHERE reservation_date = ? AND dining_table_id = ?
                          AND status IN ('PENDING','CONFIRMED','SEATED')
                        """, Integer.class, date, UUID.fromString("a1000000-0000-0000-0000-000000000001"));
                assertEquals(1, active);
                Integer guests = jdbc.queryForObject("SELECT COUNT(*) FROM reservation_guests", Integer.class);
                assertEquals(attempt + 1, guests, "failed concurrent creates must not leave guest rows");
                jdbc.update("UPDATE reservations SET status = 'CANCELLED' WHERE reservation_date = ?", date);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void administrativeEndpointsRequireAnAuthenticatedAdministrator() throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffCannotChangeAdministrativeSettings() throws Exception {
        mvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restaurantTimezone":"UTC","minimumAdvanceBookingHours":2,
                                "maximumAdvanceBookingDays":90,"minimumPartySize":1,"maximumOnlinePartySize":8,
                                "defaultReservationDurationMinutes":120,"reservationIntervalMinutes":30,
                                "cancellationCutoffHours":24,"bookingEnabled":true,"privateDiningEnabled":true}
                                """)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAndInactiveAdministratorCredentialsAreRejectedGenerically() throws Exception {
        createAdministrator("inactive@example.test", false);
        mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inactive@example.test\",\"password\":\"correct-horse-battery\"}")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Email or password is incorrect."));
        mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.test\",\"password\":\"wrong-password\"}")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message")
                        .value("Email or password is incorrect."));
    }

    @Test
    void activeAdministratorCanLoginAndLogoutInvalidatesItsSession() throws Exception {
        createAdministrator("active@example.test", true);
        MvcResult result = mvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"active@example.test\",\"password\":\"correct-horse-battery\"}")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.role").value("ADMIN"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/api/v1/admin/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.email")
                        .value("active@example.test"));
        mvc.perform(post("/api/v1/admin/auth/logout").session(session)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanReadSettings() throws Exception {
        mvc.perform(get("/api/v1/admin/settings")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void dashboardUsesDatabaseMetricsAndOperationalAlerts() throws Exception {
        mvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.confirmedReservations").isNumber())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.alerts").isArray());
        mvc.perform(get("/api/v1/admin/reservations")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/private-dining/inquiries")).andExpect(status().isOk());
    }

    @Test
    @Transactional
    @WithMockUser(username = "service-admin@example.test", roles = "ADMIN")
    void servicePeriodAndClosureChangesCannotInvalidateAnUpcomingReservation() throws Exception {
        createAdministrator("service-admin@example.test", true);
        LocalDate date = LocalDate.now(ZoneId.of("UTC")).plusDays(10);
        int dayOfWeek = date.getDayOfWeek().getValue();
        jdbc.update("UPDATE restaurant_service_periods SET is_active = FALSE WHERE day_of_week = ?", dayOfWeek);
        UUID periodId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO restaurant_service_periods
                (id, name, day_of_week, start_time, end_time, slot_interval_minutes, dining_duration_minutes, is_active)
                VALUES (?, 'Admin test period', ?, '17:00', '22:00', 30, 60, TRUE)
                """, periodId, dayOfWeek);
        var first = reservationService.create(request(date, "service-period@example.test", "Service"), "admin-test-key-000001");
        reservationService.create(new ReservationRequest("Second", "Guest", "adjacent@example.test",
                "+1 555 010 0200", date, LocalTime.of(19, 0), 2, null, null, null, null),
                "admin-test-key-000002");

        UUID firstId = reservations.findByConfirmationCode(first.confirmationCode()).orElseThrow().id;
        mvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/reservations/" + firstId + "/modify")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reservationDate":"%s","reservationTime":"19:00","partySize":2,
                                "dietaryNotes":null,"specialRequests":null}
                                """.formatted(date)))
                .andExpect(status().isConflict());

        mvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/settings/service-periods/" + periodId)
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Admin test period","dayOfWeek":%d,"startTime":"17:00","endTime":"18:59",
                                "slotIntervalMinutes":30,"diningDurationMinutes":60,"active":true}
                                """.formatted(dayOfWeek)))
                .andExpect(status().isConflict());

        mvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/settings/blocked-dates")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"blockedDate":"%s","reason":"Private event","fullDay":true,
                                "startTime":null,"endTime":null}
                                """.formatted(date)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void stateChangingAdminRequestsRequireCsrf() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/tables")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void tableCapacityRangeIsValidated() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/admin/tables")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tableNumber":"T-INVALID","displayName":"Invalid Capacity",
                                "minimumCapacity":5,"maximumCapacity":2,"location":"MAIN_DINING","active":true}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void loginRateLimitReturnsTooManyRequests() throws Exception {
        for (int attempt = 0; attempt < 6; attempt++) {
            var request = post("/api/v1/admin/auth/login")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                    .with(servletRequest -> {
                        servletRequest.setRemoteAddr("192.0.2.88");
                        return servletRequest;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"missing@example.test\",\"password\":\"wrong-password\"}");
            MvcResult result = mvc.perform(request).andReturn();
            assertEquals(attempt < 5 ? 401 : 429, result.getResponse().getStatus());
        }
    }

    private ResponseEntity<String> sendBooking(ReservationRequest request, CountDownLatch ready, CountDownLatch start, String key)
            throws InterruptedException {
        ready.countDown();
        start.await();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", key);
        return http.exchange("/api/v1/reservations", HttpMethod.POST,
                new HttpEntity<>(request, headers), String.class);
    }

    private static ReservationRequest request(LocalDate date, String email, String firstName) {
        return new ReservationRequest(firstName, "Guest", email, "+1 555 010 0200",
                date, LocalTime.of(18, 0), 2, null, null, null, null);
    }

    private static List<LocalDate> nextDates(LocalDate from, int count) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate candidate = from;
        while (candidate.getDayOfWeek().getValue() != 4) candidate = candidate.plusDays(1);
        while (dates.size() < count) {
            dates.add(candidate);
            candidate = candidate.plusDays(7);
        }
        return dates;
    }
}
