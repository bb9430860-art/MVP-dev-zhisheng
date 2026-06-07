package com.zhisheng.mvp.process;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProcessRouteTemplateMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatesProcessTemplateTablesWithDeleteMarkerIndexesAndNoForbiddenTables() throws Exception {
        Set<String> tables = tableNames();

        assertThat(tables)
                .contains("process_route_template", "process_step_template")
                .doesNotContain(
                        "file_asset",
                        "inventory_stock",
                        "attendance_record",
                        "customer",
                        "orders",
                        "order_item",
                        "contribution_account",
                        "contribution_transaction");

        assertThat(columns("process_route_template")).contains("deleted", "delete_marker", "product_type");
        assertThat(columns("process_step_template")).contains(
                "deleted",
                "delete_marker",
                "route_template_id",
                "step_order",
                "assigned_role",
                "photo_required",
                "remark_required",
                "mobile_enabled");

        String migration = Files.readString(Path.of(
                "src/main/resources/db/migration/V030__process_route_template.sql"));
        assertThat(migration).contains(
                "UNIQUE KEY uk_step_template_active_order (tenant_id, route_template_id, step_order, delete_marker)");
    }

    private Set<String> tableNames() throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (rs.next()) {
                names.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }
        return names;
    }

    private Set<String> columns(String tableName) throws Exception {
        Set<String> names = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet rs = connection.getMetaData().getColumns(null, null, tableName, "%")) {
            while (rs.next()) {
                names.add(rs.getString("COLUMN_NAME").toLowerCase());
            }
        }
        return names;
    }

}
