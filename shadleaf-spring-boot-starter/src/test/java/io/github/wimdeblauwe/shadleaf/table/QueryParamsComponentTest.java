package io.github.wimdeblauwe.shadleaf.table;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.paging.Paging;
import io.github.wimdeblauwe.shadleaf.paging.PagingParameters;
import io.github.wimdeblauwe.shadleaf.paging.QueryParam;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.ExpressionContext;

class QueryParamsComponentTest {

  private static Rendered render(String requestUri, String snippet) {
    return ComponentRenderTester.builder().requestUri(requestUri).build().render(snippet);
  }

  /** Each hidden input as {@code name=value}. */
  private static List<String> inputs(Rendered rendered) {
    return rendered.select("input").stream()
        .map(input -> input.attr("name") + "=" + input.attr("value"))
        .toList();
  }

  @Test
  void copiesTheRequestsParametersExceptTheFormsOwnAndThePage() {
    Rendered rendered = render("/people?q=ada&sort=name,desc&page=3&size=50",
        "<form><sl:query-params except=\"q\"/></form>");

    assertThat(rendered).hasNoLeakedMarkup();
    Assertions.assertThat(inputs(rendered)).containsExactly("sort=name,desc", "size=50");
    for (Element input : rendered.select("input")) {
      assertThat(input).hasAttribute("type", "hidden");
    }
  }

  @Test
  void repeatedParametersStayRepeatedInTheirOrder() {
    Rendered rendered = render("/people?sort=name,desc&status=open&sort=email&status=closed",
        "<form><sl:query-params/></form>");

    Assertions.assertThat(inputs(rendered))
        .containsExactly("sort=name,desc", "status=open", "sort=email", "status=closed");
  }

  @Test
  void valuesAreDecodedOnceAndEscapedByThymeleaf() {
    Rendered rendered = render("/people?q=ada+l%C3%A9&tag=a%26b%3Dc&label=%3Cb%3E%22x%22&flag",
        "<form><sl:query-params/></form>");

    Assertions.assertThat(inputs(rendered))
        .containsExactly("q=ada lé", "tag=a&b=c", "label=<b>\"x\"", "flag=");
    Assertions.assertThat(rendered.html())
        .contains("value=\"a&amp;b=c\"")
        .contains("value=\"&lt;b&gt;&quot;x&quot;\"");
  }

  @Test
  void exceptTakesSeveralNames() {
    Rendered rendered = render("/people?q=ada&status=open&sort=name&size=10",
        "<form><sl:query-params except=\"q, status\"/></form>");

    Assertions.assertThat(inputs(rendered)).containsExactly("sort=name", "size=10");
  }

  @Test
  void aQualifierLeavesOutItsOwnPageParameter() {
    Rendered rendered = render("/teams?members_page=2&page=4&members_sort=name&q=x",
        "<form><sl:query-params except=\"q\" qualifier=\"members\"/></form>");

    Assertions.assertThat(inputs(rendered)).containsExactly("page=4", "members_sort=name");
  }

  @Test
  void theConfiguredPageParameterIsLeftOut() {
    Rendered rendered = ComponentRenderTester.builder()
        .requestUri("/people?p=2&page=x&sort=name")
        .pagingParameters(new PagingParameters("p", "s", true, "", "_", "sort"))
        .build()
        .render("<form><sl:query-params/></form>");

    Assertions.assertThat(inputs(rendered)).containsExactly("page=x", "sort=name");
  }

  @Test
  void otherAttributesGoToEveryInput() {
    Rendered rendered = render("/people?sort=name&size=10", "<div><sl:query-params form=\"search\"/></div>");

    Assertions.assertThat(rendered.select("input").eachAttr("form")).containsExactly("search", "search");
  }

  @Test
  void aRequestWithoutParametersRendersNothing() {
    Rendered rendered = render("/people?page=2", "<form><sl:query-params/></form>");

    assertThat(rendered).hasNoElement("input");
    Assertions.assertThat(render("/people", "<form><sl:query-params/></form>").select("input")).isEmpty();
  }

  @Test
  void outsideAWebRequestThereIsNothingToCopy() {
    Paging paging = new Paging(new ExpressionContext(new SpringTemplateEngine().getConfiguration()),
        PagingParameters.defaults());

    List<QueryParam> params = paging.queryParams("q", null);

    Assertions.assertThat(params).isEmpty();
  }
}
