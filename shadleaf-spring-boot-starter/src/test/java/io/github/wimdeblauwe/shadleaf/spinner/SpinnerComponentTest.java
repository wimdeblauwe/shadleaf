package io.github.wimdeblauwe.shadleaf.spinner;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class SpinnerComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void isALoaderIconThatIsANamedStatus() {
    Rendered rendered = tester.render("<sl:spinner/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("svg")
        .hasClassName("icon spinner")
        .hasAttribute("role", "status")
        .hasAttribute("aria-label", "Loading")
        .hasNoAttribute("aria-hidden")
        .hasNoAttribute("data-icon-size");
  }

  @Test
  void sizeSetsWidthAndHeight() {
    assertThat(tester.render("<sl:spinner size=\"32\"/>")).root()
        .hasAttribute("width", "32")
        .hasAttribute("height", "32")
        .hasAttribute("data-icon-size", "32");
  }

  @Test
  void ownAriaLabelReplacesTheDefault() {
    assertThat(tester.render("<sl:spinner aria-label=\"Saving\"/>")).root().hasAttribute("aria-label", "Saving");
    assertThat(tester.render("<sl:spinner th:aria-label=\"${label}\"/>", Map.of("label", "Uploading"))).root()
        .hasAttribute("aria-label", "Uploading");
  }

  @Test
  void otherAttributesPassThrough() {
    assertThat(tester.render("<sl:spinner id=\"upload-spinner\" class=\"app-spinner\"/>")).root()
        .hasAttribute("id", "upload-spinner")
        .hasClass("spinner", "app-spinner");
  }

  @Test
  void labelIsAMessage() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.spinner.label", Locale.ENGLISH, "Please wait");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester reworded = ComponentRenderTester.builder().messageSource(messages).build();

    assertThat(reworded.render("<sl:spinner/>")).root().hasAttribute("aria-label", "Please wait");
  }
}
