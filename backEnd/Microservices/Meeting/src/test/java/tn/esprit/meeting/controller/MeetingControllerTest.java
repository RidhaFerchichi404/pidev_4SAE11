package tn.esprit.meeting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.meeting.client.ContractClient;
import tn.esprit.meeting.client.ProjectClient;
import tn.esprit.meeting.dto.*;
import tn.esprit.meeting.enums.MeetingStatus;
import tn.esprit.meeting.enums.MeetingType;
import tn.esprit.meeting.exception.MeetingNotFoundException;
import tn.esprit.meeting.exception.MeetingValidationException;
import tn.esprit.meeting.service.MeetingService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web-layer slice tests for {@link MeetingController}.
 *
 * Uses {@code @WebMvcTest} to load only the controller + Spring MVC infrastructure.
 * All service dependencies are replaced with Mockito mocks via {@code @MockitoBean}.
 * Spring Security auto-configuration is excluded because the real SecurityConfig
 * permits all requests; excluding it avoids the need for additional test setup.
 */
@WebMvcTest(
        value = MeetingController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                OAuth2ResourceServerAutoConfiguration.class
        }
)
@DisplayName("MeetingController – Web Layer Tests")
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeetingService meetingService;

    @MockitoBean
    private ProjectClient projectClient;

    @MockitoBean
    private ContractClient contractClient;

    private ObjectMapper objectMapper;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final Long USER_ID       = 1L;
    private static final Long MEETING_ID    = 10L;
    private static final String X_USER_ID   = "X-User-Id";

    private static final LocalDateTime FUTURE_START = LocalDateTime.now().plusDays(1);
    private static final LocalDateTime FUTURE_END   = FUTURE_START.plusHours(1);

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Required to serialize/deserialize java.time types
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /** Builds a minimal {@link MeetingResponse} stub to return from mocked service. */
    private MeetingResponse stubResponse(Long id, MeetingStatus status) {
        return MeetingResponse.builder()
                .id(id)
                .clientId(USER_ID)
                .freelancerId(2L)
                .title("Project Kickoff")
                .agenda("Discuss requirements")
                .startTime(FUTURE_START)
                .endTime(FUTURE_END)
                .meetingType(MeetingType.VIDEO_CALL)
                .status(status)
                .canJoinNow(false)
                .build();
    }

    /** Builds a valid {@link CreateMeetingRequest} as a JSON string. */
    private String createRequestJson() throws Exception {
        CreateMeetingRequest req = new CreateMeetingRequest();
        req.setFreelancerId(2L);
        req.setTitle("Project Kickoff");
        req.setAgenda("Discuss scope");
        req.setStartTime(FUTURE_START);
        req.setEndTime(FUTURE_END);
        return objectMapper.writeValueAsString(req);
    }

    // ── POST /api/meetings ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/meetings – should return 201 CREATED with meeting body")
    void createMeeting_validRequest_returns201() throws Exception {
        // Arrange
        when(meetingService.createMeeting(eq(USER_ID), any(CreateMeetingRequest.class)))
                .thenReturn(stubResponse(MEETING_ID, MeetingStatus.PENDING));

        // Act & Assert
        mockMvc.perform(post("/api/meetings")
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(MEETING_ID))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/meetings – should return 400 when required fields are missing")
    void createMeeting_missingTitle_returns400() throws Exception {
        // Arrange – title is blank (violates @NotBlank)
        CreateMeetingRequest req = new CreateMeetingRequest();
        req.setFreelancerId(2L);
        req.setTitle("");
        req.setStartTime(FUTURE_START);
        req.setEndTime(FUTURE_END);

        // Act & Assert
        mockMvc.perform(post("/api/meetings")
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/meetings ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings – should return 200 with list of meetings")
    void getMyMeetings_returns200WithList() throws Exception {
        // Arrange
        List<MeetingResponse> responses = List.of(
                stubResponse(1L, MeetingStatus.PENDING),
                stubResponse(2L, MeetingStatus.ACCEPTED)
        );
        when(meetingService.getMeetingsForUser(USER_ID)).thenReturn(responses);

        // Act & Assert
        mockMvc.perform(get("/api/meetings")
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/meetings – should return 200 with empty array when no meetings")
    void getMyMeetings_noMeetings_returnsEmptyArray() throws Exception {
        // Arrange
        when(meetingService.getMeetingsForUser(USER_ID)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/meetings")
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── GET /api/meetings/{meetingId} ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings/{id} – should return 200 with meeting body")
    void getMeetingById_existingId_returns200() throws Exception {
        // Arrange
        when(meetingService.getById(MEETING_ID)).thenReturn(stubResponse(MEETING_ID, MeetingStatus.ACCEPTED));

        // Act & Assert
        mockMvc.perform(get("/api/meetings/{id}", MEETING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEETING_ID))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("GET /api/meetings/{id} – should return 404 when meeting does not exist")
    void getMeetingById_unknownId_returns404() throws Exception {
        // Arrange
        when(meetingService.getById(999L))
                .thenThrow(new MeetingNotFoundException("Meeting not found with id: 999"));

        // Act & Assert
        mockMvc.perform(get("/api/meetings/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/meetings/upcoming ────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings/upcoming – should return 200 with upcoming meetings")
    void getUpcoming_returns200() throws Exception {
        // Arrange
        when(meetingService.getUpcomingMeetings(USER_ID))
                .thenReturn(List.of(stubResponse(MEETING_ID, MeetingStatus.ACCEPTED)));

        // Act & Assert
        mockMvc.perform(get("/api/meetings/upcoming")
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── GET /api/meetings/by-status ───────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings/by-status – should return 200 filtered by status")
    void getByStatus_returns200() throws Exception {
        // Arrange
        when(meetingService.getMeetingsByStatus(USER_ID, MeetingStatus.PENDING))
                .thenReturn(List.of(stubResponse(MEETING_ID, MeetingStatus.PENDING)));

        // Act & Assert
        mockMvc.perform(get("/api/meetings/by-status")
                        .header(X_USER_ID, USER_ID)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    // ── GET /api/meetings/stats ───────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings/stats – should return 200 with stats object")
    void getStats_returns200() throws Exception {
        // Arrange
        MeetingStatsDTO stats = new MeetingStatsDTO();
        stats.setTotal(5L);
        stats.setPending(2L);
        stats.setAccepted(3L);
        when(meetingService.getMeetingStats(USER_ID)).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/meetings/stats")
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.accepted").value(3));
    }

    // ── PUT /api/meetings/{meetingId} ─────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/meetings/{id} – should return 200 with updated meeting")
    void updateMeeting_validRequest_returns200() throws Exception {
        // Arrange
        UpdateMeetingRequest req = new UpdateMeetingRequest();
        req.setTitle("Updated Title");

        MeetingResponse updated = stubResponse(MEETING_ID, MeetingStatus.PENDING);
        when(meetingService.updateMeeting(eq(MEETING_ID), eq(USER_ID), any(UpdateMeetingRequest.class)))
                .thenReturn(updated);

        // Act & Assert
        mockMvc.perform(put("/api/meetings/{id}", MEETING_ID)
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEETING_ID));
    }

    @Test
    @DisplayName("PUT /api/meetings/{id} – should return 400 when service throws MeetingValidationException")
    void updateMeeting_serviceThrowsValidation_returns400() throws Exception {
        // Arrange
        when(meetingService.updateMeeting(anyLong(), anyLong(), any()))
                .thenThrow(new MeetingValidationException("Only PENDING meetings can be edited"));

        // Act & Assert
        mockMvc.perform(put("/api/meetings/{id}", MEETING_ID)
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateMeetingRequest())))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/meetings/{meetingId}/status ────────────────────────────────

    @Test
    @DisplayName("PATCH /api/meetings/{id}/status – ACCEPT should return 200")
    void updateStatus_accept_returns200() throws Exception {
        // Arrange
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(MeetingStatus.ACCEPTED);

        when(meetingService.updateStatus(eq(MEETING_ID), eq(USER_ID), any(StatusUpdateRequest.class)))
                .thenReturn(stubResponse(MEETING_ID, MeetingStatus.ACCEPTED));

        // Act & Assert
        mockMvc.perform(patch("/api/meetings/{id}/status", MEETING_ID)
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    @DisplayName("PATCH /api/meetings/{id}/status – should return 400 when validation fails")
    void updateStatus_validationError_returns400() throws Exception {
        // Arrange
        StatusUpdateRequest req = new StatusUpdateRequest();
        req.setStatus(MeetingStatus.ACCEPTED);

        when(meetingService.updateStatus(anyLong(), anyLong(), any()))
                .thenThrow(new MeetingValidationException("Only the invited freelancer can accept"));

        // Act & Assert
        mockMvc.perform(patch("/api/meetings/{id}/status", MEETING_ID)
                        .header(X_USER_ID, USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /api/meetings/{meetingId} ──────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/meetings/{id} – should return 204 NO CONTENT on success")
    void deleteMeeting_success_returns204() throws Exception {
        // Arrange
        doNothing().when(meetingService).deleteMeeting(MEETING_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/meetings/{id}", MEETING_ID)
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isNoContent());

        verify(meetingService).deleteMeeting(MEETING_ID, USER_ID);
    }

    @Test
    @DisplayName("DELETE /api/meetings/{id} – should return 404 when meeting not found")
    void deleteMeeting_notFound_returns404() throws Exception {
        // Arrange
        doThrow(new MeetingNotFoundException("Meeting not found with id: " + MEETING_ID))
                .when(meetingService).deleteMeeting(MEETING_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/meetings/{id}", MEETING_ID)
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/meetings/{id} – should return 400 when meeting is not PENDING")
    void deleteMeeting_notPending_returns400() throws Exception {
        // Arrange
        doThrow(new MeetingValidationException("Only PENDING meetings can be deleted"))
                .when(meetingService).deleteMeeting(MEETING_ID, USER_ID);

        // Act & Assert
        mockMvc.perform(delete("/api/meetings/{id}", MEETING_ID)
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/meetings/my-projects ─────────────────────────────────────────

    @Test
    @DisplayName("GET /api/meetings/my-projects – should return 200 with empty list when both Feign calls fail")
    void getMyProjects_feignThrows_returnsEmptyList() throws Exception {
        // Arrange
        when(projectClient.getProjectsByClient(USER_ID)).thenThrow(new RuntimeException("Service down"));
        when(projectClient.getProjectsByFreelancer(USER_ID)).thenThrow(new RuntimeException("Service down"));

        // Act & Assert
        mockMvc.perform(get("/api/meetings/my-projects")
                        .header(X_USER_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
