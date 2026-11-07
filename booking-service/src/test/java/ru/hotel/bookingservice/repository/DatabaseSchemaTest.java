package ru.hotel.bookingservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** The schema comes from the Liquibase changelog, not from Hibernate, and keeps its named keys and indexes. */
@DataJpaTest
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void changelogIsAppliedInOrder() {
        // when
        List<String> changeSets =
                jdbc.queryForList("select id from databasechangelog order by orderexecuted", String.class);

        // then
        assertThat(changeSets).containsExactly("001-users", "002-bookings", "003-bookings-slot-request-id");
    }

    @Test
    void keysAndConstraintsAreNamed() {
        // when
        List<String> constraints = jdbc.queryForList(
                "select constraint_name from information_schema.table_constraints where table_schema = 'PUBLIC'",
                String.class);

        // then
        assertThat(constraints)
                .contains("PK_USERS", "UQ_USERS_USERNAME", "PK_BOOKINGS", "FK_BOOKINGS_USER", "UQ_BOOKINGS_REQUEST_ID");
    }

    @Test
    void queriesHaveSupportingIndexes() {
        // when
        List<String> indexes = jdbc.queryForList(
                "select index_name from information_schema.indexes where table_schema = 'PUBLIC'", String.class);

        // then
        assertThat(indexes).contains("IDX_BOOKINGS_USER_CREATED");
    }
}
