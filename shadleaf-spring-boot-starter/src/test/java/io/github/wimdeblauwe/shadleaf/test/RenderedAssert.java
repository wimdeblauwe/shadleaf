package io.github.wimdeblauwe.shadleaf.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ListAssert;
import org.jsoup.nodes.Element;

/** Assertions on a whole render; {@link #root()} and {@link #element(String)} step down to one element. */
public class RenderedAssert extends AbstractAssert<RenderedAssert, Rendered> {

  // An sl: or th: tag or attribute, or a comment, that should have been processed away.
  private static final Pattern LEAKED_MARKUP = Pattern.compile("<!--|</?(sl|th):|\\s(sl|th):[\\w-]+=?");

  public RenderedAssert(Rendered actual) {
    super(actual, RenderedAssert.class);
  }

  /** The first rendered element. */
  public ElementAssert root() {
    isNotNull();
    return new ElementAssert(actual.root());
  }

  /** The first element matching the CSS query; fails when there is none. */
  public ElementAssert element(String cssQuery) {
    isNotNull();
    Element element = actual.document().body().selectFirst(cssQuery);
    if (element == null) {
      failWithMessage("Expected an element matching <%s> in:%n%s", cssQuery, actual.html());
    }
    return new ElementAssert(element);
  }

  /** Every element matching the CSS query, for {@code hasSize}, {@code allSatisfy} and friends. */
  public ListAssert<Element> elements(String cssQuery) {
    isNotNull();
    return Assertions.assertThat(actual.select(cssQuery))
        .as("elements matching <%s>", cssQuery);
  }

  public RenderedAssert hasNoElement(String cssQuery) {
    isNotNull();
    if (!actual.select(cssQuery).isEmpty()) {
      failWithMessage("Expected no element matching <%s> in:%n%s", cssQuery, actual.html());
    }
    return this;
  }

  /** Nothing of the template language reaches the page: no {@code sl:} or {@code th:} markup and no comments. */
  public RenderedAssert hasNoLeakedMarkup() {
    isNotNull();
    Matcher matcher = LEAKED_MARKUP.matcher(actual.html());
    if (matcher.find()) {
      failWithMessage("Expected no template markup in the output, but found <%s> in:%n%s", matcher.group().strip(),
          actual.html());
    }
    return this;
  }
}
