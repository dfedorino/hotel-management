package ru.hotel.hotelservice.repository;

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
        assertThat(changeSets).containsExactly("001-hotels-and-rooms", "002-room-reservations");
    }

    @Test
    void keysAndConstraintsAreNamed() {
        // when
        List<String> constraints = jdbc.queryForList(
                "select constraint_name from information_schema.table_constraints where table_schema = 'PUBLIC'",
                String.class);

        // then
        assertThat(constraints)
                .contains(
                        "PK_HOTELS",
                        "PK_ROOMS",
                        "FK_ROOMS_HOTEL",
                        "UQ_ROOMS_HOTEL_NUMBER",
                        "PK_ROOM_RESERVATIONS",
                        "FK_RESERVATIONS_ROOM",
                        "UQ_RESERVATIONS_REQUEST_ID");
    }

    @Test
    void queriesHaveSupportingIndexes() {
        // when
        List<String> indexes = jdbc.queryForList(
                "select index_name from information_schema.indexes where table_schema = 'PUBLIC'", String.class);

        // then
        assertThat(indexes).contains("IDX_ROOMS_TIMES_BOOKED", "IDX_RESERVATIONS_ROOM_DATES");
    }
}
