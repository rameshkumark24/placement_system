package com.rameshkumar.placementsystem;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rameshkumar.placementsystem.repository.ApplicationRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end tests through the full Spring context (security filters, controllers, services,
 * JPA on H2). The bootstrap admin comes from src/test/resources/application.yaml.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@placement.test";
    private static final String ADMIN_PASSWORD = "AdminPass123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationRepository applicationRepository;

    // ---------------------------------------------------------------- health / wake-up

    @Test
    void healthEndpointsArePublicSoRenderCanWakeTheService() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docs").value("/swagger-ui/index.html"));

        mockMvc.perform(get("/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("UP"));
    }

    @Test
    void healthEndpointIgnoresStaleTokens() throws Exception {
        mockMvc.perform(get("/health").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void corsPreflightFromAllowedOriginSucceeds() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    // ---------------------------------------------------------------- authentication

    @Test
    void protectedEndpointWithoutTokenReturnsJson401() throws Exception {
        mockMvc.perform(get("/companies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required. Please sign in."));
    }

    @Test
    void invalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/companies").header(HttpHeaders.AUTHORIZATION, "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid JWT token"));
    }

    @Test
    void registrationValidatesInput() throws Exception {
        postJson("/auth/register", Map.of("name", "No Email", "password", "secret1"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.email").value("Email cannot be empty"));

        postJson("/auth/register", Map.of("name", "Short", "email", uniqueEmail(), "password", "123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.password").value(notNullValue()));
    }

    @Test
    void duplicateRegistrationIsConflictAndEmailIsCaseInsensitive() throws Exception {
        String email = uniqueEmail();
        register("Dup Student", email, "secret123");

        postJson("/auth/register", Map.of("name", "Dup Again", "email", email.toUpperCase(), "password", "secret123"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));

        // Login also works with different casing and surrounding spaces.
        postJson("/auth/login", Map.of("email", "  " + email.toUpperCase() + " ", "password", "secret123"), null)
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        postJson("/auth/login", Map.of("email", ADMIN_EMAIL, "password", "wrong-password"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void malformedJsonReturnsFriendly400() throws Exception {
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void refreshTokenRotatesTokensAndCannotBeUsedAsAccessToken() throws Exception {
        JsonNode tokens = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String refreshToken = tokens.get("refreshToken").asText();

        mockMvc.perform(get("/companies").header(HttpHeaders.AUTHORIZATION, "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());

        postJson("/auth/refresh", Map.of("refreshToken", refreshToken), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value(notNullValue()));

        // An access token is not a refresh token.
        postJson("/auth/refresh", Map.of("refreshToken", tokens.get("token").asText()), null)
                .andExpect(status().isUnauthorized());

        postJson("/auth/refresh", Map.of("refreshToken", "garbage"), null)
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- authorization

    @Test
    void bootstrapAdminCanUseAdminEndpoints() throws Exception {
        String adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        mockMvc.perform(get("/dashboard/stats").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalStudents").value(notNullValue()));

        mockMvc.perform(get("/students/paginated").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10));

        mockMvc.perform(get("/students/paginated?page=0&size=1000").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void studentsCannotReachAdminEndpointsOrOtherStudentsData() throws Exception {
        String email = uniqueEmail();
        register("Curious Student", email, "secret123");
        String studentToken = accessToken(email, "secret123");

        mockMvc.perform(get("/dashboard/stats").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        postJson("/companies", companyPayload("Hack Co", 1.0, LocalDate.now().plusDays(5)), studentToken)
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/students/1").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/students/search?skill=java").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/students/paginated").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- placement workflow

    @Test
    void fullPlacementWorkflow() throws Exception {
        String adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);

        long companyId = postJson("/companies", companyPayload("Acme " + UUID.randomUUID(), 7.5, LocalDate.now().plusDays(10)), adminToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .transform(this::readData).get("id").asLong();

        String email = uniqueEmail();
        register("Placement Student", email, "secret123");
        String studentToken = accessToken(email, "secret123");

        // A freshly registered student has an empty profile and cannot apply yet.
        mockMvc.perform(get("/students/me").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));
        postJson("/applications/apply/" + companyId, null, studentToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Complete your profile")));

        // Profile complete but CGPA below the company's cut-off.
        putJson("/students/me", profilePayload(7.0), studentToken).andExpect(status().isOk());
        postJson("/applications/apply/" + companyId, null, studentToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("below the minimum")));

        // Eligible now.
        putJson("/students/me", profilePayload(8.2), studentToken).andExpect(status().isOk());
        postJson("/applications/apply/" + companyId, null, studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));

        postJson("/applications/apply/" + companyId, null, studentToken)
                .andExpect(status().isConflict());

        String applications = mockMvc.perform(get("/applications/my").header(HttpHeaders.AUTHORIZATION, bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        long applicationId = readData(applications).get(0).get("id").asLong();

        // Admin moves the application forward.
        putJson("/applications/" + applicationId + "/status", Map.of("status", "shortlisted"), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));
        putJson("/applications/" + applicationId + "/status", Map.of("status", "HIRED"), adminToken)
                .andExpect(status().isBadRequest());

        // Unfiltered listings traverse student -> user and company outside the web layer.
        mockMvc.perform(get("/applications").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.studentEmail == '" + email + "')].companyName").value(notNullValue()));
        mockMvc.perform(get("/students").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email == '" + email + "')].cgpa").value(8.2));
        mockMvc.perform(get("/dashboard/stats").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortlistedApplications").value(notNullValue()));

        mockMvc.perform(get("/applications?status=SHORTLISTED&studentEmail=" + email)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].studentEmail").value(email));

        // Deleting a company with applications removes them instead of failing on the foreign key.
        mockMvc.perform(delete("/companies/" + companyId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        assertTrue(applicationRepository.findById(applicationId).isEmpty());
        mockMvc.perform(get("/companies/" + companyId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void applyingAfterDeadlineIsRejected() throws Exception {
        String adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        long companyId = postJson("/companies", companyPayload("Closing Soon " + UUID.randomUUID(), 5.0, LocalDate.now()), adminToken)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()
                .transform(this::readData).get("id").asLong();

        String email = uniqueEmail();
        register("Late Student", email, "secret123");
        String studentToken = accessToken(email, "secret123");
        putJson("/students/me", profilePayload(9.0), studentToken).andExpect(status().isOk());

        // Deadline today is still open.
        postJson("/applications/apply/" + companyId, null, studentToken).andExpect(status().isOk());

        postJson("/companies", companyPayload("Past", 5.0, LocalDate.now().minusDays(1)), adminToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingStudentRemovesTheirAccount() throws Exception {
        String adminToken = accessToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        String email = uniqueEmail();

        long studentId = postJson("/students", Map.of(
                        "name", "Admin Created",
                        "email", email,
                        "password", "secret123",
                        "cgpa", 8.0,
                        "skills", "Java",
                        "resumeLink", "https://example.com/resume.pdf"), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andReturn().getResponse().getContentAsString()
                .transform(this::readData).get("id").asLong();

        accessToken(email, "secret123");
        mockMvc.perform(get("/students?skill=jav&cgpa=7.5").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.email == '" + email + "')].name").value("Admin Created"));
        mockMvc.perform(get("/students/" + studentId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(delete("/students/" + studentId).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", email, "password", "secret123"), null)
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- helpers

    private ResultActions postJson(String path, Object body, String token) throws Exception {
        var request = post(path).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(objectMapper.writeValueAsString(body));
        }
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, bearer(token));
        }
        return mockMvc.perform(request);
    }

    private ResultActions putJson(String path, Object body, String token) throws Exception {
        return mockMvc.perform(put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void register(String name, String email, String password) throws Exception {
        postJson("/auth/register", Map.of("name", name, "email", email, "password", password), null)
                .andExpect(status().isOk());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = postJson("/auth/login", Map.of("email", email, "password", password), null)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return readData(body);
    }

    private String accessToken(String email, String password) throws Exception {
        return login(email, password).get("token").asText();
    }

    private JsonNode readData(String json) {
        try {
            return objectMapper.readTree(json).get("data");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Map<String, Object> companyPayload(String name, double eligibilityCgpa, LocalDate deadline) {
        return Map.of(
                "name", name,
                "role", "Backend Developer",
                "package", 12.5,
                "eligibilityCgpa", eligibilityCgpa,
                "deadline", deadline.toString());
    }

    private static Map<String, Object> profilePayload(double cgpa) {
        return Map.of(
                "name", "Placement Student",
                "cgpa", cgpa,
                "skills", "Java, Spring Boot",
                "resumeLink", "https://example.com/resume.pdf");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "student-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }
}
