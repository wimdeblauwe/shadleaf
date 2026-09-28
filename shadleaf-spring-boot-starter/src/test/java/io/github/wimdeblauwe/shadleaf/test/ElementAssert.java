package io.github.wimdeblauwe.shadleaf.test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.AbstractAssert;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

/** Assertions on one rendered element. Failure messages show the element's outer HTML. */
public class ElementAssert extends AbstractAssert<ElementAssert, Element> {

  public ElementAssert(Element actual) {
    super(actual, ElementAssert.class);
  }

  public ElementAssert hasTag(String tagName) {
    isNotNull();
    if (!actual.normalName().equals(tagName)) {
      failWithMessage("Expected a <%s> element but was:%n%s", tagName, actual.outerHtml());
    }
    return this;
  }

  /** The {@code class} attribute is exactly this, in this order, such as {@code "btn ml-auto"}. */
  public ElementAssert hasClassName(String className) {
    isNotNull();
    if (!actual.className().equals(className)) {
      failWithMessage("Expected class <%s> but was <%s> on:%n%s", className, actual.className(), actual.outerHtml());
    }
    return this;
  }

  /** The element has each of these classes, among others. */
  public ElementAssert hasClass(String... classNames) {
    isNotNull();
    for (String className : classNames) {
      if (!actual.hasClass(className)) {
        failWithMessage("Expected class <%s> on:%n%s", className, actual.outerHtml());
      }
    }
    return this;
  }

  public ElementAssert hasAttribute(String name) {
    isNotNull();
    if (!actual.hasAttr(name)) {
      failWithMessage("Expected attribute <%s> on:%n%s", name, actual.outerHtml());
    }
    return this;
  }

  public ElementAssert hasAttribute(String name, String value) {
    hasAttribute(name);
    if (!actual.attr(name).equals(value)) {
      failWithMessage("Expected %s=<%s> but was <%s> on:%n%s", name, value, actual.attr(name), actual.outerHtml());
    }
    return this;
  }

  public ElementAssert hasNoAttribute(String... names) {
    isNotNull();
    for (String name : names) {
      if (actual.hasAttr(name)) {
        failWithMessage("Expected no attribute <%s> on:%n%s", name, actual.outerHtml());
      }
    }
    return this;
  }

  /** The element has exactly these attributes, in this order; their values are not checked. */
  public ElementAssert hasAttributeNames(String... names) {
    isNotNull();
    List<String> actualNames = actual.attributes().asList().stream().map(Attribute::getKey).toList();
    if (!actualNames.equals(Arrays.asList(names))) {
      failWithMessage("Expected attributes %s but were %s on:%n%s", Arrays.asList(names), actualNames,
          actual.outerHtml());
    }
    return this;
  }

  /** The element's text, whitespace normalised as jsoup's {@link Element#text()} does. */
  public ElementAssert hasText(String text) {
    isNotNull();
    if (!Objects.equals(actual.text(), text)) {
      failWithMessage("Expected text <%s> but was <%s> in:%n%s", text, actual.text(), actual.outerHtml());
    }
    return this;
  }

  /** The first element below this one matching the CSS query ({@code "> span"} for a direct child). */
  public ElementAssert element(String cssQuery) {
    isNotNull();
    Element element = actual.selectFirst(cssQuery);
    if (element == null) {
      failWithMessage("Expected an element matching <%s> in:%n%s", cssQuery, actual.outerHtml());
    }
    return new ElementAssert(element);
  }

  public ElementAssert hasNoElement(String cssQuery) {
    isNotNull();
    if (actual.selectFirst(cssQuery) != null) {
      failWithMessage("Expected no element matching <%s> in:%n%s", cssQuery, actual.outerHtml());
    }
    return this;
  }
}
