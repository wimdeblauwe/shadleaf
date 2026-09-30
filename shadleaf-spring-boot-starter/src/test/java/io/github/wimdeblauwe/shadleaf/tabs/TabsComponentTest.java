package io.github.wimdeblauwe.shadleaf.tabs;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TabsComponentTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  private static final String SETTINGS = """
      <sl:tabs id="settings" th:active="${tab}">
        <sl:tabs-list>
          <sl:tabs-trigger tab="account">Account</sl:tabs-trigger>
          <sl:tabs-trigger tab="password">Password</sl:tabs-trigger>
        </sl:tabs-list>
        <sl:tabs-content tab="account">A</sl:tabs-content>
        <sl:tabs-content tab="password">P</sl:tabs-content>
      </sl:tabs>""";

  @Test
  void theServerRendersTheActiveTabAndItsPanel() {
    Rendered rendered = tester.render(SETTINGS, Map.of("tab", "password"));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasClass("tabs").hasAttribute("id", "settings").hasAttribute("x-data", "slTabs")
        .hasNoAttribute("data-orientation", "data-activation", "active");
    assertThat(rendered).element(".tabs-list").hasAttribute("role", "tablist").hasNoAttribute("aria-orientation");
    assertThat(rendered).element("#settings-account-trigger")
        .hasTag("button")
        .hasAttribute("type", "button")
        .hasAttribute("role", "tab")
        .hasAttribute("aria-selected", "false")
        .hasAttribute("aria-controls", "settings-account-content")
        .hasNoAttribute("tabindex", "tab");
    assertThat(rendered).element("#settings-password-trigger").hasAttribute("aria-selected", "true");
    assertThat(rendered).element("#settings-account-content")
        .hasAttribute("role", "tabpanel")
        .hasAttribute("tabindex", "0")
        .hasAttribute("aria-labelledby", "settings-account-trigger")
        .hasAttribute("hidden");
    assertThat(rendered).element("#settings-password-content").hasNoAttribute("hidden");
  }

  @Test
  void anEnumOrNumberNamesTheActiveTabToo() {
    Rendered rendered = tester.render("""
        <sl:tabs id="t" th:active="${2}"><sl:tabs-list><sl:tabs-trigger tab="1">1</sl:tabs-trigger>
          <sl:tabs-trigger tab="2">2</sl:tabs-trigger></sl:tabs-list></sl:tabs>""");

    assertThat(rendered).element("#t-2-trigger").hasAttribute("aria-selected", "true");
  }

  @Test
  void aTriggerWithAnHrefIsALink() {
    Rendered rendered = tester.render("""
        <sl:tabs id="s" active="a"><sl:tabs-list>
          <sl:tabs-trigger tab="a" th:href="@{/settings(tab=a)}">A</sl:tabs-trigger>
          <sl:tabs-trigger tab="b" href="/settings?tab=b" disabled>B</sl:tabs-trigger>
        </sl:tabs-list></sl:tabs>""");

    assertThat(rendered).element("#s-a-trigger").hasTag("a").hasAttribute("href", "/settings?tab=a")
        .hasAttribute("role", "tab").hasAttribute("aria-selected", "true").hasNoAttribute("type", "aria-disabled");
    assertThat(rendered).element("#s-b-trigger").hasTag("a").hasNoAttribute("href")
        .hasAttribute("aria-disabled", "true");
    assertThat(rendered).hasNoElement("button");
  }

  @Test
  void aDisabledButton() {
    assertThat(tester.render("<sl:tabs-trigger tab=\"x\" disabled>X</sl:tabs-trigger>")).root()
        .hasTag("button").hasAttribute("disabled");
  }

  @Test
  void orientationAndActivation() {
    Rendered rendered = tester.render("""
        <sl:tabs id="t" active="a" orientation="vertical" activation-mode="manual">
          <sl:tabs-list variant="line"><sl:tabs-trigger tab="a">A</sl:tabs-trigger></sl:tabs-list>
        </sl:tabs>""");

    assertThat(rendered).root().hasAttribute("data-orientation", "vertical").hasAttribute("data-activation", "manual");
    assertThat(rendered).element(".tabs-list").hasAttribute("aria-orientation", "vertical")
        .hasAttribute("data-variant", "line");
  }

  @Test
  void nestedTabsKeepTheirOwnIdsAndActiveTab() {
    Rendered rendered = tester.render("""
        <sl:tabs id="outer" active="a">
          <sl:tabs-list><sl:tabs-trigger tab="a">A</sl:tabs-trigger></sl:tabs-list>
          <sl:tabs-content tab="a">
            <sl:tabs id="inner" active="y"><sl:tabs-list><sl:tabs-trigger tab="x">X</sl:tabs-trigger>
              <sl:tabs-trigger tab="y">Y</sl:tabs-trigger></sl:tabs-list>
              <sl:tabs-content tab="y">Y</sl:tabs-content></sl:tabs>
          </sl:tabs-content>
        </sl:tabs>""");

    assertThat(rendered).element("#outer-a-content").hasNoAttribute("hidden");
    assertThat(rendered).element("#inner-x-trigger").hasAttribute("aria-selected", "false");
    assertThat(rendered).element("#inner-y-content").hasNoAttribute("hidden");
  }

  @Test
  void needsAnIdAndAnActiveTab() {
    assertThatRenderFailure(() -> tester.render("<sl:tabs active=\"a\">x</sl:tabs>"))
        .hasMessageContaining("<sl:tabs> needs id");
    assertThatRenderFailure(() -> tester.render("<sl:tabs id=\"t\" th:active=\"${missing}\">x</sl:tabs>"))
        .hasMessageContaining("active");
    assertThatRenderFailure(() -> tester.render("<sl:tabs-trigger>X</sl:tabs-trigger>"))
        .hasMessageContaining("tab");
  }
}
