package Marshmello.MarshmelloWas.global.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class PageResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void serializesTheApprovedPagingShapeAndGenericContent() throws Exception {
        PageResponse<Item> response = new PageResponse<>(
                List.of(new Item("first", 7)), 2, 10, 21L, 3);

        String serialized = mapper.writeValueAsString(response);
        JsonNode json = mapper.readTree(serialized);

        assertThat(json.properties()).extracting(entry -> entry.getKey())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages");
        assertThat(json.size()).isEqualTo(5);
        assertThat(json.get("content").get(0).get("label").asText()).isEqualTo("first");
        assertThat(json.get("content").get(0).get("score").asInt()).isEqualTo(7);
        assertThat(json.get("page").asInt()).isEqualTo(2);
        assertThat(json.get("size").asInt()).isEqualTo(10);
        assertThat(json.get("totalElements").asLong()).isEqualTo(21L);
        assertThat(json.get("totalPages").asInt()).isEqualTo(3);
    }

    private record Item(String label, int score) {
    }
}
