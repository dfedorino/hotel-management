package ru.hotel.bookingservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;

/** Log events land in the log file as one ECS JSON object per line, carrying the MDC correlation fields. */
@SpringBootTest(properties = "logging.file.name=target/structured-logging-test/booking-service.json")
class StructuredLoggingTest {

    private static final Path LOG_FILE = Path.of("target/structured-logging-test/booking-service.json");
    private static final Logger LOG = LoggerFactory.getLogger(StructuredLoggingTest.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void eventsAreWrittenAsEcsJsonWithCorrelationFields() throws IOException {
        // when
        try (MDC.MDCCloseable ignored = MDC.putCloseable("bookingId", "42")) {
            LOG.info("requestId=req-1 booking CONFIRMED");
        }

        // then
        JsonNode event = lastEventMatching("booking CONFIRMED");
        assertThat(event.path("service").path("name").asText()).isEqualTo("booking-service");
        assertThat(event.path("log").path("level").asText()).isEqualTo("INFO");
        assertThat(event.path("bookingId").asText()).isEqualTo("42");
        assertThat(event.path("@timestamp").asText()).isNotBlank();
    }

    private JsonNode lastEventMatching(String text) throws IOException {
        List<String> lines = Files.readAllLines(LOG_FILE);
        for (int i = lines.size() - 1; i >= 0; i--) {
            JsonNode event = objectMapper.readTree(lines.get(i));
            if (event.path("message").asText().contains(text)) {
                return event;
            }
        }
        throw new AssertionError("No log event with '" + text + "' in " + LOG_FILE);
    }
}
