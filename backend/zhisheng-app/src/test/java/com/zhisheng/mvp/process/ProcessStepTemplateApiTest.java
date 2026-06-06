package com.zhisheng.mvp.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProcessStepTemplateApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("delete from process_step_template");
        jdbcTemplate.update("delete from process_route_template");
    }

    @Test
    void createsUpdatesEnablesDisablesAndSoftDeletesStepTemplates() throws Exception {
        long routeId = createRoute("ROUTE-STEPS", "Step route", "SPIRIT_FORTRESS", true);
        long stepId = createStep(routeId, "STEP-DESIGN", "Design", "WORKER", true, true, true);

        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/{stepId}", routeId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "stepCode", "STEP-DESIGN-UPDATED",
                                "stepName", "Design updated",
                                "assignedRole", "PRODUCTION_MANAGER",
                                "photoRequired", false,
                                "remarkRequired", true,
                                "mobileEnabled", false,
                                "estimatedHours", 3,
                                "operationInstruction", "updated instruction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stepName").value("Design updated"))
                .andExpect(jsonPath("$.data.assignedRole").value("PRODUCTION_MANAGER"))
                .andExpect(jsonPath("$.data.photoRequired").value(false))
                .andExpect(jsonPath("$.data.mobileEnabled").value(false));

        mockMvc.perform(patch("/api/process/route-templates/{routeId}/steps/{stepId}/enabled", routeId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        long secondStepId = createStep(routeId, "STEP-CUT", "Cut", "WORKER", false, false, true);
        List<Integer> activeOrdersAfterDisable = jdbcTemplate.queryForList(
                "select step_order from process_step_template where route_template_id = ? and enabled = 1 and deleted = 0 order by step_order",
                Integer.class,
                routeId);
        assertThat(activeOrdersAfterDisable).containsExactly(1);
        assertThat(jdbcTemplate.queryForObject(
                "select step_order from process_step_template where id = ?",
                Integer.class,
                secondStepId)).isEqualTo(1);

        mockMvc.perform(patch("/api/process/route-templates/{routeId}/steps/{stepId}/enabled", routeId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(delete("/api/process/route-templates/{routeId}/steps/{stepId}", routeId, stepId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from process_step_template where id = ?",
                Integer.class,
                stepId);
        Long deleteMarker = jdbcTemplate.queryForObject(
                "select delete_marker from process_step_template where id = ?",
                Long.class,
                stepId);
        assertThat(deleted).isEqualTo(1);
        assertThat(deleteMarker).isNotZero();
    }

    @Test
    void movesStepsUpDownAndSavesManualOrder() throws Exception {
        long routeId = createRoute("ROUTE-ORDER", "Order route", "GENERAL", true);
        long first = createStep(routeId, "STEP-1", "First", "WORKER", false, false, true);
        long second = createStep(routeId, "STEP-2", "Second", "WORKER", false, false, true);
        long third = createStep(routeId, "STEP-3", "Third", "WORKER", false, false, true);

        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/{stepId}/move-up", routeId, third))
                .andExpect(status().isOk());
        assertThat(activeStepIds(routeId)).containsExactly(first, third, second);

        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/{stepId}/move-down", routeId, first))
                .andExpect(status().isOk());
        assertThat(activeStepIds(routeId)).containsExactly(third, first, second);

        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(second, first, third)))))
                .andExpect(status().isOk());
        assertThat(activeStepIds(routeId)).containsExactly(second, first, third);
        assertThat(activeStepOrders(routeId)).containsExactly(1, 2, 3);
    }

    @Test
    void rejectsForeignDeletedDisabledDuplicatedAndMissingStepIdsForManualOrder() throws Exception {
        long routeId = createRoute("ROUTE-VALIDATION", "Validation route", "GENERAL", true);
        long foreignRouteId = createRoute("ROUTE-FOREIGN", "Foreign route", "GENERAL", true);
        long first = createStep(routeId, "STEP-A", "A", "WORKER", false, false, true);
        long second = createStep(routeId, "STEP-B", "B", "WORKER", false, false, true);
        long disabled = createStep(routeId, "STEP-DISABLED", "Disabled", "WORKER", false, false, true);
        long deleted = createStep(routeId, "STEP-DELETED", "Deleted", "WORKER", false, false, true);
        long foreign = createStep(foreignRouteId, "STEP-FOREIGN", "Foreign", "WORKER", false, false, true);

        mockMvc.perform(patch("/api/process/route-templates/{routeId}/steps/{stepId}/enabled", routeId, disabled)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/process/route-templates/{routeId}/steps/{stepId}", routeId, deleted))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(first, first)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(first)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(first, second, foreign)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(first, second, deleted)))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/process/route-templates/{routeId}/steps/reorder", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("stepIds", List.of(first, second, disabled)))))
                .andExpect(status().isBadRequest());
    }

    private long createRoute(String code, String name, String productType, boolean enabled) throws Exception {
        String response = mockMvc.perform(post("/api/process/route-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "routeCode", code,
                                "routeName", name,
                                "productType", productType,
                                "description", name + " description",
                                "enabled", enabled))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private long createStep(
            long routeId,
            String code,
            String name,
            String role,
            boolean photoRequired,
            boolean remarkRequired,
            boolean mobileEnabled) throws Exception {
        String response = mockMvc.perform(post("/api/process/route-templates/{routeId}/steps", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "stepCode", code,
                                "stepName", name,
                                "assignedRole", role,
                                "photoRequired", photoRequired,
                                "remarkRequired", remarkRequired,
                                "mobileEnabled", mobileEnabled,
                                "estimatedHours", 2,
                                "operationInstruction", name + " instruction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private List<Long> activeStepIds(long routeId) {
        return jdbcTemplate.queryForList(
                "select id from process_step_template where route_template_id = ? and enabled = 1 and deleted = 0 order by step_order",
                Long.class,
                routeId);
    }

    private List<Integer> activeStepOrders(long routeId) {
        return jdbcTemplate.queryForList(
                "select step_order from process_step_template where route_template_id = ? and enabled = 1 and deleted = 0 order by step_order",
                Integer.class,
                routeId);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
