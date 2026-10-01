package io.github.wimdeblauwe.shadleaf.toast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The JSON a {@link Toast} becomes is the detail slToaster reads from an {@code sl-toast} event. */
class ToastTest {

  private final JsonMapper jsonMapper = JsonMapper.builder().build();

  @Test
  void serializesToTheDetailOfTheSlToastEvent() {
    assertThat(jsonMapper.writeValueAsString(Toast.success("Ada was deleted")))
        .isEqualTo("{\"title\":\"Ada was deleted\",\"description\":null,\"variant\":\"success\",\"duration\":null}");
    assertThat(jsonMapper.writeValueAsString(
        Toast.error("Could not save").withDescription("Try again.").withDuration(Duration.ofSeconds(8))))
        .isEqualTo("{\"title\":\"Could not save\",\"description\":\"Try again.\",\"variant\":\"error\","
            + "\"duration\":8000}");
    assertThat(Toast.of("Hi").untilClosed().duration()).isZero();
    assertThat(Toast.of("Hi").variant()).isEqualTo("default");
  }

  @Test
  void writesOnlyAsciiSoTheJsonFitsInAnHttpHeader() {
    Toast toast = Toast.error("Zo\u00eb \u0141ukasiewicz \"quoted\" \\ \uD83D\uDE00").withDescription("line\nbreak");

    String json = jsonMapper.writeValueAsString(toast);
    assertThat(json).isEqualTo(toast.toJson());
    assertThat(json).isEqualTo("{\"title\":\"Zo\\u00eb \\u0141ukasiewicz \\\"quoted\\\" \\\\ \\ud83d\\ude00\","
        + "\"description\":\"line\\u000abreak\",\"variant\":\"error\",\"duration\":null}");
    assertThat(json.chars()).allMatch(character -> character >= 0x20 && character <= 0x7e);
    // As htmx-spring-boot puts it in HX-Trigger: a map of events, and a list of toasts.
    assertThat(jsonMapper.writeValueAsString(Map.of("sl-toast", List.of(toast, Toast.of("Two")))))
        .isEqualTo("{\"sl-toast\":[" + toast.toJson() + "," + Toast.of("Two").toJson() + "]}");
    Map<?, ?> read = jsonMapper.readValue(json, Map.class);
    assertThat(read.get("title")).isEqualTo(toast.title());
    assertThat(read.get("description")).isEqualTo("line\nbreak");
  }

  @Test
  void rejectsWhatTheToasterCannotShow() {
    assertThatThrownBy(() -> Toast.of(" ")).hasMessageContaining("title");
    assertThatThrownBy(() -> new Toast("x", null, "danger", null)).hasMessageContaining("danger");
    assertThatThrownBy(() -> Toast.of("x").withDuration(Duration.ofMillis(-1))).hasMessageContaining("negative");
  }
}
