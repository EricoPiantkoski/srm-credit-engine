package com.srm.creditengine.infrastructure.adapter.in.web;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController implements HealthIndicator {

    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    public void setDataSource(DataSource dataSource) {
        if (dataSource != null) {
            this.jdbcTemplate = new JdbcTemplate(dataSource);
        }
    }

    @GetMapping
    public HealthResponse liveness() {
        return new HealthResponse("UP");
    }

    @GetMapping("/readiness")
    public HealthResponse readiness() {
        if (jdbcTemplate == null) {
            return new HealthResponse("DOWN - No DataSource");
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return new HealthResponse("UP");
        } catch (Exception e) {
            return new HealthResponse("DOWN - " + e.getMessage());
        }
    }

    @Override
    public Health health() {
        if (jdbcTemplate == null) {
            return Health.down().withDetail("error", "No DataSource").build();
        }
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }

    public record HealthResponse(String status) {
    }
}
