package io.github.wimdeblauwe.shadleaf.table;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;
import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThatRenderFailure;

import io.github.wimdeblauwe.shadleaf.paging.PagingParameters;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class TableComponentTest {

  private static final String PEOPLE = """
      <sl:table th:page="${people}">
        <thead><tr>
          <sl:table-head sort="name">Name</sl:table-head>
          <sl:table-head sort="email">Email</sl:table-head>
          <sl:table-head align="end">Orders</sl:table-head>
        </tr></thead>
        <tbody><tr><td>Ada</td><td>ada@example.com</td><td data-align="end">3</td></tr></tbody>
      </sl:table>""";

  private static final ComponentRenderTester TESTER = ComponentRenderTester.create();

  /** The link's icon is the named one: icons carry no name, so it is compared with the icon on its own. */
  private static void assertIcon(Rendered rendered, String linkSelector, String name) {
    Assertions.assertThat(rendered.select(linkSelector + " > svg").html())
        .isEqualTo(TESTER.render("<sl:icon name=\"" + name + "\"/>").root().html());
  }

  @Test
  void theTableIsAPlainTableInAScrollContainer() {
    Rendered rendered = TESTER.render("""
        <sl:table id="people" class="extra">
          <caption>People</caption>
          <tbody><tr th:each="name : ${names}"><td th:text="${name}">Name</td></tr></tbody>
        </sl:table>""", Map.of("names", List.of("Ada", "Grace")));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).root().hasTag("div").hasClass("table-container").hasAttributeNames("class");
    assertThat(rendered).element("div > table").hasClass("table", "extra").hasAttribute("id", "people");
    assertThat(rendered).elements("table > tbody > tr > td").extracting(td -> td.text()).containsExactly("Ada",
        "Grace");
  }

  @Test
  void aSortLinkReadsTheOrderFromTheRequestWithoutAPage() {
    Rendered rendered = ComponentRenderTester.builder()
        .requestUri("/people?q=ada%20l&sort=name,desc&page=3&size=20")
        .build()
        .render(PEOPLE);

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("th:nth-child(1)")
        .hasClass("table-head")
        .hasAttribute("aria-sort", "descending")
        .hasNoAttribute("data-align", "sort");
    assertThat(rendered).element("th:nth-child(1) > a")
        .hasClass("btn", "table-sort")
        .hasAttribute("data-variant", "ghost")
        .hasAttribute("data-size", "sm")
        .hasAttribute("id", "sort-name")
        .hasAttribute("href", "/people?q=ada%20l&size=20&sort=name,asc")
        .hasText("Name");
    assertIcon(rendered, "#sort-name", "arrow-down");
    assertThat(rendered).element("th:nth-child(2)").hasNoAttribute("aria-sort");
    assertThat(rendered).element("#sort-email")
        .hasAttribute("href", "/people?q=ada%20l&size=20&sort=email,asc");
    assertIcon(rendered, "#sort-email", "chevrons-up-down");
  }

  @Test
  void aHeaderWithoutSortIsAPlainHeaderCell() {
    Rendered rendered = TESTER.render(PEOPLE);

    assertThat(rendered).element("th:nth-child(3)")
        .hasClass("table-head")
        .hasAttribute("data-align", "end")
        .hasNoAttribute("aria-sort", "align")
        .hasNoElement("a")
        .hasText("Orders");
    assertThat(rendered).element("td[data-align]").hasAttribute("data-align", "end");
  }

  @Test
  void aSortedColumnLinksToTheOtherDirection() {
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people?sort=name").build().render(PEOPLE);

    assertThat(rendered).element("th:nth-child(1)").hasAttribute("aria-sort", "ascending");
    assertThat(rendered).element("#sort-name").hasAttribute("href", "/people?sort=name,desc");
    assertIcon(rendered, "#sort-name", "arrow-up");
  }

  @Test
  void aPageTellsHowTheRowsAreSorted() {
    PageImpl<String> people = new PageImpl<>(List.of("Ada"),
        PageRequest.of(2, 10, Sort.by("email").descending().and(Sort.by("name"))), 97);
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people?page=2").build()
        .render(PEOPLE, Map.of("people", people));

    assertThat(rendered).element("th:nth-child(1)").hasNoAttribute("aria-sort");
    assertThat(rendered).element("th:nth-child(2)").hasAttribute("aria-sort", "descending");
    assertThat(rendered).element("#sort-email").hasAttribute("href", "/people?sort=email,asc");
    assertThat(rendered).element("#sort-name").hasAttribute("href", "/people?sort=name,asc");
  }

  @Test
  void anUnsortedPageSortsNoColumnEvenWhenTheRequestAsks() {
    PageImpl<String> people = new PageImpl<>(List.of("Ada"), PageRequest.of(0, 10), 1);
    Rendered rendered = ComponentRenderTester.builder().requestUri("/people?sort=name,desc").build()
        .render(PEOPLE, Map.of("people", people));

    assertThat(rendered).hasNoElement("th[aria-sort]");
  }

  @Test
  void linksStartWithTheContextPath() {
    Rendered rendered = ComponentRenderTester.builder().contextPath("/shop").requestUri("/people").build()
        .render(PEOPLE);

    assertThat(rendered).element("#sort-name").hasAttribute("href", "/shop/people?sort=name,asc");
  }

  @Test
  void aQualifierGivesTheTableItsOwnParameters() {
    Rendered rendered = ComponentRenderTester.builder()
        .requestUri("/team?sort=name&page=1&members_sort=email,desc&members_page=4")
        .build()
        .render("""
            <sl:table qualifier="members"><thead><tr>
              <sl:table-head sort="email">Email</sl:table-head>
              <sl:table-head sort="address.city">City</sl:table-head>
            </tr></thead></sl:table>""");

    assertThat(rendered).element("th:nth-child(1)").hasAttribute("aria-sort", "descending");
    assertThat(rendered).element("#members-sort-email")
        .hasAttribute("href", "/team?sort=name&page=1&members_sort=email,asc");
    assertThat(rendered).element("#members-sort-address-city")
        .hasAttribute("href", "/team?sort=name&page=1&members_sort=address.city,asc");
  }

  @Test
  void theParameterNamesAreTheApplications() {
    Rendered rendered = ComponentRenderTester.builder()
        .pagingParameters(new PagingParameters("p", "s", true, "x_", "__", "order"))
        .requestUri("/people?x_p=3&x_s=50&order=name,desc&sort=email")
        .build()
        .render(PEOPLE);

    assertThat(rendered).element("th:nth-child(1)").hasAttribute("aria-sort", "descending");
    assertThat(rendered).element("#sort-name").hasAttribute("href", "/people?x_s=50&sort=email&order=name,asc");
  }

  @Test
  void anythingButAPageOrSliceFails() {
    assertThatRenderFailure(() -> TESTER.render(PEOPLE, Map.of("people", List.of("Ada"))))
        .hasMessageContaining("th:page takes a Spring Data Page or Slice");
  }

  @Test
  void aPageCannotBeALiteral() {
    assertThatRenderFailure(() -> TESTER.render("<sl:table page=\"people\"></sl:table>"))
        .hasMessageContaining("<sl:table> takes page as an object: write th:page=\"${...}\"");
  }

  @Test
  void theEmptyRowSpansTheColumnsAndSaysNoResults() {
    Rendered rendered = TESTER.render("""
        <sl:table><tbody><sl:table-empty colspan="3" th:if="${people.isEmpty()}"/></tbody></sl:table>""",
        Map.of("people", List.of()));

    assertThat(rendered).hasNoLeakedMarkup();
    assertThat(rendered).element("tbody > tr").hasClass("table-empty").hasAttributeNames("class");
    assertThat(rendered).element("tr.table-empty > td").hasAttribute("colspan", "3").hasText("No results.");
  }

  @Test
  void contentOfItsOwnReplacesTheEmptyText() {
    Rendered rendered = TESTER.render("""
        <sl:table><tbody><sl:table-empty th:colspan="${2}">Nobody matches <b>ada</b>.</sl:table-empty>
        </tbody></sl:table>""");

    assertThat(rendered).element("tr.table-empty > td").hasAttribute("colspan", "2").hasText("Nobody matches ada.");
  }

  @Test
  void theEmptyRowNeedsItsColspan() {
    assertThatRenderFailure(() -> TESTER.render("<sl:table-empty/>"))
        .hasMessageContaining("<sl:table-empty> needs colspan");
  }
}
