package io.github.wimdeblauwe.shadleaf.test;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jspecify.annotations.Nullable;

/**
 * The output of one {@link ComponentRenderTester} render: the HTML exactly as Thymeleaf wrote it, and a jsoup
 * document parsed from it for {@link ShadleafAssertions}. The document is parsed on first use, so a render that only
 * needs the HTML (the performance test) does not pay for it.
 */
public final class Rendered {

  private final String html;
  private @Nullable Document document;

  Rendered(String html) {
    this.html = html;
  }

  /** The HTML exactly as rendered, whitespace included. */
  public String html() {
    return html;
  }

  /**
   * The parsed fragment. Changes to it, such as removing SVG paths, show up in {@link #normalizedHtml()}, not in
   * {@link #html()}.
   */
  public Document document() {
    if (document == null) {
      document = parse(html);
    }
    return document;
  }

  /**
   * jsoup up to 1.23 parses a {@code select} by the old HTML rules, which drop everything in it but options, option
   * groups and text: the {@code button} of a customizable select, an {@code hr}, the icons in an option. Browsers
   * keep them now (jsoup 1.24 will too). So the select is parsed under another name, where jsoup keeps its content,
   * and renamed back.
   */
  private static Document parse(String html) {
    Document parsed = Jsoup.parseBodyFragment(html
        .replaceAll("<select(?=[\\s>])", "<" + SELECT_STAND_IN)
        .replace("</select>", "</" + SELECT_STAND_IN + ">"));
    parsed.select(SELECT_STAND_IN).forEach(select -> select.tagName("select"));
    return parsed;
  }

  private static final String SELECT_STAND_IN = "sl-parsed-select";

  /** The first rendered element: for a single component, its root element. */
  public Element root() {
    Element body = document().body();
    if (body.childrenSize() == 0) {
      throw new AssertionError("Nothing rendered but text:%n%s".formatted(html));
    }
    return body.child(0);
  }

  public Elements select(String cssQuery) {
    return document().body().select(cssQuery);
  }

  /**
   * The fragment with one element or text per line, indented by two spaces, attributes in rendered order. Text is
   * trimmed and its whitespace collapsed, and whitespace-only text is dropped, so the template's formatting does not
   * show up; an element holding only text stays on one line. This is the form approval files store.
   * <p>
   * Dropping whitespace is safe for the flex containers components render, where whitespace between items does not
   * render; it would hide a missing space between inline elements in running text.
   */
  public String normalizedHtml() {
    StringBuilder out = new StringBuilder();
    for (Node node : document().body().childNodes()) {
      print(node, 0, out);
    }
    return out.toString().stripTrailing();
  }

  private static void print(Node node, int depth, StringBuilder out) {
    String indent = "  ".repeat(depth);
    if (node instanceof TextNode text) {
      String normalized = text.text().strip().replaceAll("\\s+", " ");
      if (!normalized.isEmpty()) {
        out.append(indent).append(Entities.escape(normalized)).append('\n');
      }
    } else if (node instanceof Comment comment) {
      out.append(indent).append("<!--").append(comment.getData()).append("-->\n");
    } else if (node instanceof Element element) {
      String open = "<" + element.tagName() + element.attributes().html() + ">";
      String close = "</" + element.tagName() + ">";
      if (element.tag().isEmpty()) {
        out.append(indent).append(open).append('\n');
      } else if (element.childNodes().stream().allMatch(TextNode.class::isInstance)) {
        String text = element.text().strip();
        out.append(indent).append(open).append(Entities.escape(text)).append(close).append('\n');
      } else {
        out.append(indent).append(open).append('\n');
        for (Node child : element.childNodes()) {
          print(child, depth + 1, out);
        }
        out.append(indent).append(close).append('\n');
      }
    }
  }

  @Override
  public String toString() {
    return html;
  }
}
