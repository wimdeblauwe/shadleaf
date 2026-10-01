package io.github.wimdeblauwe.shadleaf.theme;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.i18n.ShadleafMessageSource;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticMessageSource;

class ThemeToggleComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void menuWithLightDarkAndSystem() {
    Rendered rendered = tester.render("<sl:theme-toggle/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root()
        .hasTag("div")
        .hasClassName("theme-toggle")
        .hasAttribute("x-data", "slThemeToggle");
    assertThat(rendered).element(".theme-toggle > button.dropdown-menu-trigger")
        .hasClass("btn", "theme-toggle-trigger")
        .hasAttribute("id", "theme-toggle-trigger")
        .hasAttribute("popovertarget", "theme-toggle")
        .hasAttribute("aria-haspopup", "menu")
        .hasAttribute("aria-label", "Theme")
        .hasAttribute("data-variant", "outline")
        .hasAttribute("data-size", "icon")
        // The server cannot know the theme: the trigger has no state, and CSS picks the icon.
        .hasNoAttribute("aria-pressed")
        .element("svg.theme-toggle-light[aria-hidden=true]");
    assertThat(rendered).element(".theme-toggle-trigger > svg.theme-toggle-dark[aria-hidden=true]");
    assertThat(rendered).element(".theme-toggle > div.dropdown-menu-content")
        .hasAttribute("id", "theme-toggle")
        .hasAttribute("role", "menu")
        .hasAttribute("aria-labelledby", "theme-toggle-trigger")
        .hasAttribute("data-align", "end")
        .hasNoAttribute("data-side");
    assertThat(rendered).elements(".dropdown-menu-radio-group > [role=menuitemradio]")
        .extracting(item -> item.attr("data-theme-choice") + ":" + item.text() + ":" + item.attr("aria-checked"))
        .containsExactly("light:Light:false", "dark:Dark:false", "system:System:false");
    assertThat(rendered).elements("[role=menuitemradio] > svg:first-child").hasSize(3);
  }

  @Test
  void buttonForDarkMode() {
    Rendered rendered = tester.render("<sl:theme-toggle as=\"button\" variant=\"ghost\" size=\"icon-sm\"/>");

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).hasNoElement(".dropdown-menu-content");
    assertThat(rendered).element(".theme-toggle > button.theme-toggle-button")
        .hasClass("btn", "theme-toggle-trigger")
        .hasAttribute("type", "button")
        .hasAttribute("aria-label", "Dark mode")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "icon-sm")
        // slThemeToggle sets aria-pressed; a wrong state before it runs would be worse than none.
        .hasNoAttribute("id", "aria-pressed", "popovertarget");
  }

  @Test
  void ownIdLabelSideAndAttributes() {
    Rendered menu = tester.render("""
        <sl:theme-toggle id="footer-theme" side="top" align="center" aria-label="Colour scheme" class="ms-auto"
                         data-testid="theme"/>""");

    assertThat(menu).element(".theme-toggle-trigger")
        .hasAttribute("id", "footer-theme-trigger")
        .hasAttribute("popovertarget", "footer-theme")
        .hasAttribute("aria-label", "Colour scheme")
        .hasAttribute("data-testid", "theme")
        .hasClass("ms-auto");
    assertThat(menu).root().hasAttribute("class", "theme-toggle");
    assertThat(menu).element(".dropdown-menu-content")
        .hasAttribute("id", "footer-theme")
        .hasAttribute("data-side", "top")
        .hasAttribute("data-align", "center");

    Rendered button = tester.render("<sl:theme-toggle as=\"button\" id=\"dark-mode\" th:aria-label=\"${label}\"/>",
        Map.of("label", "Night"));
    assertThat(button).element(".theme-toggle-button")
        .hasAttribute("id", "dark-mode")
        .hasAttribute("aria-label", "Night");
  }

  @Test
  void labelsFromTheApplicationsMessages() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.addMessage("sl.theme-toggle.label", Locale.ENGLISH, "Thema");
    messages.addMessage("sl.theme-toggle.system", Locale.ENGLISH, "Systeem");
    ShadleafMessageSource.attachTo(messages);
    ComponentRenderTester renamed = ComponentRenderTester.builder().messageSource(messages).locale(Locale.ENGLISH).build();

    Rendered rendered = renamed.render("<sl:theme-toggle/>");

    assertThat(rendered).element(".theme-toggle-trigger").hasAttribute("aria-label", "Thema");
    assertThat(rendered).element("[data-theme-choice=system]").hasText("Systeem");
    assertThat(rendered).element("[data-theme-choice=light]").hasText("Light");
  }
}
