package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:input-group>} and its parts, rendered into {@code src/test/resources/approved/input-group.approved.html}.
 * Its behaviour with {@code th:field} is in {@link InputGroupBindingTest}.
 */
class InputGroupApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void inputGroup() {
    List<String> snippets = new ArrayList<>();
    for (String align : LibraryComponents.registry().get("input-group-addon").prop("align").values()) {
      snippets.add("""
          <sl:input-group>
            <sl:input-group-input name="q" aria-label="Search"/>
            <sl:input-group-addon align="%s"><sl:icon name="search"/></sl:input-group-addon>
          </sl:input-group>""".formatted(align));
    }
    for (String size : LibraryComponents.registry().get("input-group-button").prop("size").values()) {
      snippets.add("<sl:input-group-button size=\"%s\" aria-label=\"Copy\"><sl:icon name=\"copy\"/></sl:input-group-button>"
          .formatted(size));
    }
    for (String type : LibraryComponents.registry().get("input-group-input").prop("type").values()) {
      snippets.add("<sl:input-group-input type=\"%s\" name=\"x\"/>".formatted(type));
    }
    snippets.addAll(List.of(
        "<sl:input-group class=\"w-64\" id=\"price\"/>",
        "<sl:input-group-addon align=\"block-start\" border>Script</sl:input-group-addon>",
        "<sl:input-group-addon align=\"block-end\" border>0/280</sl:input-group-addon>",
        "<sl:input-group-addon border>Ignored on an inline addon</sl:input-group-addon>",
        "<sl:input-group-text>USD</sl:input-group-text>",
        "<sl:input-group-button variant=\"default\" size=\"sm\" type=\"submit\" class=\"ms-1\">Post</sl:input-group-button>",
        "<sl:input-group-button loading>Check</sl:input-group-button>",
        "<sl:input-group-input name=\"code\" value=\"ABC\" disabled readonly aria-invalid=\"true\"/>",
        "<sl:input-group-textarea name=\"message\" rows=\"3\" placeholder=\"Write a comment...\">Hi</sl:input-group-textarea>",
        "<sl:input-group-textarea name=\"message\" disabled aria-invalid=\"true\"/>"));

    HtmlApproval.verifyRenders("input-group", tester, snippets);
  }
}
