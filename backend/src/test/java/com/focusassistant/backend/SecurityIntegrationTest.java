package com.focusassistant.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.focusassistant.backend.dto.AuthRequest;
import com.focusassistant.backend.dto.PomodoroStartRequest;
import com.focusassistant.backend.model.StudySession;
import com.focusassistant.backend.model.User;
import com.focusassistant.backend.repository.PomodoroSessionRepository;
import com.focusassistant.backend.repository.StudySessionRepository;
import com.focusassistant.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("API security")
class SecurityIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository userRepository;
    @Autowired private StudySessionRepository studySessionRepository;
    @Autowired private PomodoroSessionRepository pomodoroRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clean() {
        pomodoroRepository.deleteAll();
        studySessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- helpers ----------

    private String signupAndGetToken(String username, String password) throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername(username);
        req.setPassword(password);

        String body = mvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return json.readTree(body).get("token").asText();
    }

    private User createUser(String username, String rawPassword) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        return userRepository.save(u);
    }

    private StudySession createSessionFor(User owner) {
        StudySession s = new StudySession();
        s.setSubject("DSA");
        s.setDuration(45);
        s.setPlannedDuration(60);
        s.setFocusRating(4);
        s.setFocusScore(60.0);
        s.setSessionDate(LocalDate.now());
        s.setUser(owner);
        return studySessionRepository.save(s);
    }

    // ==========================================================
    // Authentication
    // ==========================================================

    @Test
    @DisplayName("rejects a request with no token")
    void rejectsMissingToken() throws Exception {
        mvc.perform(get("/api/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rejects a malformed token")
    void rejectsGarbageToken() throws Exception {
        mvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer not-a-real-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("accepts a valid token")
    void acceptsValidToken() throws Exception {
        String token = signupAndGetToken("alice", "secret123");

        mvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login and signup are reachable without a token")
    void authEndpointsArePublic() throws Exception {
        AuthRequest req = new AuthRequest();
        req.setUsername("bob");
        req.setPassword("secret123");

        mvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // ==========================================================
    // Password handling
    // ==========================================================

    @Test
    @DisplayName("never returns a password hash in a session response")
    void neverLeaksPasswordHash() throws Exception {
        String token = signupAndGetToken("carol", "secret123");
        User carol = userRepository.findByUsername("carol").orElseThrow();
        createSessionFor(carol);

        mvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].username").value("carol"));
    }

    @Test
    @DisplayName("stores the password hashed, never in plain text")
    void hashesPassword() throws Exception {
        signupAndGetToken("dave", "secret123");

        User dave = userRepository.findByUsername("dave").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(dave.getPassword())
                .isNotEqualTo("secret123")
                .startsWith("$2");   // BCrypt prefix
    }

    // ==========================================================
    // Username enumeration
    // ==========================================================

    @Test
    @DisplayName("gives the same error for an unknown user and a wrong password")
    void doesNotLeakWhichUsernamesExist() throws Exception {
        createUser("erin", "correct-password");

        AuthRequest wrongPassword = new AuthRequest();
        wrongPassword.setUsername("erin");
        wrongPassword.setPassword("wrong-password");

        AuthRequest unknownUser = new AuthRequest();
        unknownUser.setUsername("nobody");
        unknownUser.setPassword("any-password");

        String a = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(wrongPassword)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String b = mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(unknownUser)))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        String messageA = json.readTree(a).get("errors").get(0).asText();
        String messageB = json.readTree(b).get("errors").get(0).asText();

        org.assertj.core.api.Assertions.assertThat(messageA).isEqualTo(messageB);
    }

    @Test
    @DisplayName("rejects a duplicate username with 409")
    void rejectsDuplicateUsername() throws Exception {
        signupAndGetToken("frank", "secret123");

        AuthRequest again = new AuthRequest();
        again.setUsername("frank");
        again.setPassword("secret123");

        mvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(again)))
                .andExpect(status().isConflict());
    }

    // ==========================================================
    // Per-user data isolation
    // ==========================================================

    @Test
    @DisplayName("shows a user only their own sessions")
    void isolatesSessionsPerUser() throws Exception {
        String graceToken = signupAndGetToken("grace", "secret123");
        User grace = userRepository.findByUsername("grace").orElseThrow();
        createSessionFor(grace);

        User heidi = createUser("heidi", "secret123");
        createSessionFor(heidi);
        createSessionFor(heidi);

        mvc.perform(get("/api/sessions")
                        .header("Authorization", "Bearer " + graceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("grace"));
    }

    @Test
    @DisplayName("scopes analytics to the authenticated user")
    void isolatesAnalyticsPerUser() throws Exception {
        String ivanToken = signupAndGetToken("ivan", "secret123");
        User ivan = userRepository.findByUsername("ivan").orElseThrow();
        createSessionFor(ivan);

        User judy = createUser("judy", "secret123");
        createSessionFor(judy);
        createSessionFor(judy);
        createSessionFor(judy);

        mvc.perform(get("/api/analytics/summary")
                        .header("Authorization", "Bearer " + ivanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSessions").value(1));
    }

    // ==========================================================
    // IDOR — the ownership check
    // ==========================================================

    @Test
    @DisplayName("refuses to delete another user's session")
    void cannotDeleteSomeoneElsesSession() throws Exception {
        String mallory = signupAndGetToken("mallory", "secret123");

        User victim = createUser("victim", "secret123");
        StudySession victimSession = createSessionFor(victim);

        mvc.perform(delete("/api/sessions/" + victimSession.getId())
                        .header("Authorization", "Bearer " + mallory))
                .andExpect(status().isForbidden());

        org.assertj.core.api.Assertions
                .assertThat(studySessionRepository.findById(victimSession.getId()))
                .isPresent();
    }

    @Test
    @DisplayName("allows deleting your own session")
    void canDeleteOwnSession() throws Exception {
        String token = signupAndGetToken("nina", "secret123");
        User nina = userRepository.findByUsername("nina").orElseThrow();
        StudySession own = createSessionFor(nina);

        mvc.perform(delete("/api/sessions/" + own.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions
                .assertThat(studySessionRepository.findById(own.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("returns 404 for a session that does not exist")
    void deleteMissingSessionIs404() throws Exception {
        String token = signupAndGetToken("oscar", "secret123");

        mvc.perform(delete("/api/sessions/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ==========================================================
    // Validation
    // ==========================================================

    @Test
    @DisplayName("rejects a short username and password with field messages")
    void rejectsInvalidSignup() throws Exception {
        AuthRequest bad = new AuthRequest();
        bad.setUsername("ab");
        bad.setPassword("123");

        mvc.perform(post("/api/auth/signup")
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(2)));
    }

    @Test
    @DisplayName("rejects a Pomodoro with no subject")
    void rejectsPomodoroWithoutSubject() throws Exception {
        String token = signupAndGetToken("peggy", "secret123");

        PomodoroStartRequest bad = new PomodoroStartRequest();
        bad.setFocusMinutes(25);

        mvc.perform(post("/api/pomodoro/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(bad)))
                .andExpect(status().isBadRequest());
    }

    // ==========================================================
    // Pomodoro lifecycle through HTTP
    // ==========================================================

    @Test
    @DisplayName("returns 404 when no Pomodoro is active")
    void noActivePomodoroIs404() throws Exception {
        String token = signupAndGetToken("quinn", "secret123");

        mvc.perform(get("/api/pomodoro/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("refuses to start a second Pomodoro with 409")
    void rejectsSecondPomodoro() throws Exception {
        String token = signupAndGetToken("rupert", "secret123");

        PomodoroStartRequest req = new PomodoroStartRequest();
        req.setSubject("DSA");

        mvc.perform(post("/api/pomodoro/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/pomodoro/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("keeps one user's Pomodoro invisible to another")
    void pomodoroIsPerUser() throws Exception {
        String sybilToken = signupAndGetToken("sybil", "secret123");
        String trentToken = signupAndGetToken("trent", "secret123");

        PomodoroStartRequest req = new PomodoroStartRequest();
        req.setSubject("OS");

        mvc.perform(post("/api/pomodoro/start")
                        .header("Authorization", "Bearer " + sybilToken)
                        .contentType(APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Trent has no session of his own
        mvc.perform(get("/api/pomodoro/active")
                        .header("Authorization", "Bearer " + trentToken))
                .andExpect(status().isNotFound());
    }
}
