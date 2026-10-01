package io.github.wimdeblauwe.shadleaf.nav;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@code #slNav.current(path)} and {@code #slNav.current(path, exact)}, through a template as a layout uses them. */
class NavigationTest {

  private static String current(String requestUri, String path) {
    return render(ComponentRenderTester.builder().requestUri(requestUri).build(),
        "#slNav.current('%s')".formatted(path));
  }

  private static String exact(String requestUri, String path) {
    return render(ComponentRenderTester.builder().requestUri(requestUri).build(),
        "#slNav.current('%s', true)".formatted(path));
  }

  private static String render(ComponentRenderTester tester, String expression) {
    return tester.render("<span th:text=\"${%s}\"></span>".formatted(expression)).root().text();
  }

  @ParameterizedTest(name = "request {0}, path {1} -> {2}")
  @CsvSource({
      "/people, /people, true",
      // Pages below it.
      "/people/12, /people, true",
      "/people/12/edit, /people, true",
      // Not a page that only starts with the same letters.
      "/peoples, /people, false",
      "/people-load-more, /people, false",
      "/settings, /people, false",
      // The query string of the request or the path is ignored, and so is a trailing slash.
      "/people?sort=name%2Cdesc&page=2, /people, true",
      "/people, /people?sort=name, true",
      "/people/, /people, true",
      "/people, /people/, true",
      "/people;jsessionid=ABC, /people, true",
      // The root is only current for itself.
      "/, /, true",
      "/people, /, false"})
  void aPathIsCurrentForItsPageAndThePagesBelowIt(String requestUri, String path, String current) {
    assertThat(current(requestUri, path)).isEqualTo(current);
  }

  @ParameterizedTest(name = "request {0}, path {1} -> {2}")
  @CsvSource({
      "/people, /people, true",
      "/people?page=2, /people, true",
      "/people/, /people, true",
      "/people/12, /people, false",
      "/, /, true"})
  void exactOnlyMatchesThePageItself(String requestUri, String path, String current) {
    assertThat(exact(requestUri, path)).isEqualTo(current);
  }

  @Test
  void theContextPathMakesNoDifference() {
    ComponentRenderTester tester = ComponentRenderTester.builder()
        .contextPath("/shop")
        .requestUri("/people/12?tab=roles")
        .build();

    assertThat(render(tester, "#slNav.current('/people')")).isEqualTo("true");
    assertThat(render(tester, "#slNav.current('/people', true)")).isEqualTo("false");
    assertThat(render(tester, "#slNav.current('/shop/people')")).isEqualTo("false");
    assertThat(render(tester, "#slNav.current('/')")).isEqualTo("false");
  }

  @Test
  void theRootUnderAContextPathIsTheApplicationsRoot() {
    ComponentRenderTester tester = ComponentRenderTester.builder().contextPath("/shop").requestUri("/").build();

    assertThat(render(tester, "#slNav.current('/')")).isEqualTo("true");
  }

  @Test
  void setsActiveOnAMenuButton() {
    ComponentRenderTester tester = ComponentRenderTester.builder().requestUri("/people/12").build();

    assertThat(tester.render("""
        <sl:sidebar-menu-button th:href="@{/people}" th:active="${#slNav.current('/people')}">People\
        </sl:sidebar-menu-button>""")).root().hasAttribute("aria-current", "page");
    assertThat(tester.render("""
        <sl:sidebar-menu-button th:href="@{/}" th:active="${#slNav.current('/')}">Home</sl:sidebar-menu-button>"""))
        .root().hasNoAttribute("aria-current");
  }

  @Test
  void aPathMustStartWithASlash() {
    assertThatRenderFailure(() -> current("/people", "people"))
        .hasMessageContaining("#slNav.current")
        .hasMessageContaining("'people'");
  }
}
