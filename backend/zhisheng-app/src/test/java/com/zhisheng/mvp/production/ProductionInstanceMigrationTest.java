package com.zhisheng.mvp.production;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductionInstanceMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatesProductionInstanceTablesAndNoOutOfScopeTables() throws Exception {
        Set<String> tables = tableNames();

        assertThat(tables)
                .contains("production_route_instance", "production_step_instance")
                .doesNotContain(
                        "production_step_checkin",
                        "inventory_stock",
                        "attendance_record",
                        "customer",
                        "orders",
                        "order_item",
                        "contribution_account",
                        "contribution_transaction");

        assertThat(columns("production_route_instance")).contains(
                "order_id",
                "order_item_id",
                "source_route_template_id",
                "route_code_snapshot",
                "route_name_snapshot",
                "status",
                "production_progress",
                "frozen",
                "deleted",
                "delete_marker");

        assertThat(columns("production_step_instance")).contains(
                "route_instance_id",
                "order_id",
                "order_item_id",
                "source_step_template_id",
                "step_code_snapshot",
                "step_name",
                "step_order",
                "assigned_role",
                "assigned_user_id",
                "photo_required",
                "remark_required",
                "mobile_enabled",
                "status",
                "frozen",
                "deleted",
                "delete_marker");
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
