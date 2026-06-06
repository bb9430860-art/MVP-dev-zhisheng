package com.zhisheng.mvp.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ProcessRouteTemplateApiTest {

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
    void createsUpdatesEnablesDisablesAndSoftDeletesRouteTemplates() throws Exception {
        long routeId = createRoute("ROUTE-SPIRIT", "Spirit route", "SPIRIT_FORTRESS", true);

        mockMvc.perform(put("/api/process/route-templates/{id}", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "routeCode", "ROUTE-SPIRIT-UPDATED",
                                "routeName", "Updated spirit route",
                                "productType", "SPIRIT_FORTRESS",
                                "description", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.routeName").value("Updated spirit route"));

        mockMvc.perform(patch("/api/process/route-templates/{id}/enabled", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/process/route-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].routeName").value("Updated spirit route"));

        mockMvc.perform(get("/api/process/route-templates/options")
                        .param("productType", "SPIRIT_FORTRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        createStep(routeId, "STEP-DESIGN", "Design", "WORKER");

        mockMvc.perform(patch("/api/process/route-templates/{id}/enabled", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(delete("/api/process/route-templates/{id}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/process/route-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/process/route-templates/options")
                        .param("productType", "SPIRIT_FORTRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        Integer deleted = jdbcTemplate.queryForObject(
                "select deleted from process_route_template where id = ?",
                Integer.class,
                routeId);
        Long deleteMarker = jdbcTemplate.queryForObject(
                "select delete_marker from process_route_template where id = ?",
                Long.class,
                routeId);
        assertThat(deleted).isEqualTo(1);
        assertThat(deleteMarker).isNotZero();
    }

    @Test
    void optionsReturnExactProductGeneralAndEmptyProductTypeTemplates() throws Exception {
        long exactId = createRoute("ROUTE-EXACT", "Exact route", "SPIRIT_FORTRESS", true);
        long generalId = createRoute("ROUTE-GENERAL", "General route", "GENERAL", true);
        long emptyId = createRoute("ROUTE-EMPTY", "Empty product route", "", true);
        createRoute("ROUTE-OTHER", "Other route", "FLOOR_SIGN", true);
        createRoute("ROUTE-DISABLED", "Disabled exact route", "SPIRIT_FORTRESS", false);
        long deletedId = createRoute("ROUTE-DELETED", "Deleted exact route", "SPIRIT_FORTRESS", true);
        mockMvc.perform(delete("/api/process/route-templates/{id}", deletedId)).andExpect(status().isOk());

        JsonNode data = dataNode(mockMvc.perform(get("/api/process/route-templates/options")
                        .param("productType", "SPIRIT_FORTRESS"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        List<Long> ids = stream(data).map(node -> node.get("id").asLong()).toList();
        assertThat(ids).containsExactlyInAnyOrder(exactId, generalId, emptyId);
    }

    @Test
    void routeTemplateRequiresActiveEnabledStepBeforeEnable() throws Exception {
        long routeId = createRoute("ROUTE-ENABLE", "Enable route", "GENERAL", false);

        mockMvc.perform(patch("/api/process/route-templates/{id}/enabled", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Route template requires at least one active enabled step"));

        long disabledStepId = createStep(routeId, "STEP-DISABLED", "Disabled step", "WORKER");
        mockMvc.perform(patch("/api/process/route-templates/{routeId}/steps/{stepId}/enabled", routeId, disabledStepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/process/route-templates/{id}/enabled", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Route template requires at least one active enabled step"));

        createStep(routeId, "STEP-ACTIVE", "Active step", "WORKER");

        mockMvc.perform(patch("/api/process/route-templates/{id}/enabled", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("enabled", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
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
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private long createStep(long routeId, String code, String name, String role) throws Exception {
        String response = mockMvc.perform(post("/api/process/route-templates/{routeId}/steps", routeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "stepCode", code,
                                "stepName", name,
                                "assignedRole", role,
                                "photoRequired", false,
                                "remarkRequired", false,
                                "mobileEnabled", true,
                                "estimatedHours", 1,
                                "operationInstruction", name + " instruction"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("data").get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode dataNode(String response) throws Exception {
        return objectMapper.readTree(response).get("data");
    }

    private java.util.stream.Stream<JsonNode> stream(JsonNode node) {
        return java.util.stream.StreamSupport.stream(node.spliterator(), false);
    }
}
