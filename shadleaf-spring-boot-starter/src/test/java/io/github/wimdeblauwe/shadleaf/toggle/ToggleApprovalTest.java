package io.github.wimdeblauwe.shadleaf.toggle;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:toggle>} and {@code <sl:toggle-group>} with its items, rendered into
 * {@code src/test/resources/approved/<name>.approved.html}. Their behaviour with {@code th:field} is in
 * {@link ToggleBindingTest}.
 */
class ToggleApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void toggle() {
    List<String> snippets = new ArrayList<>();
    for (String variant : LibraryComponents.registry().get("toggle").prop("variant").values()) {
      snippets.add("<sl:toggle variant=\"%s\" name=\"bold\" aria-label=\"Bold\"><sl:icon name=\"bold\"/></sl:toggle>"
          .formatted(variant));
    }
    for (String size : LibraryComponents.registry().get("toggle").prop("size").values()) {
      snippets.add("<sl:toggle size=\"%s\" name=\"italic\">Italic</sl:toggle>".formatted(size));
    }
    snippets.addAll(List.of(
        "<sl:toggle name=\"starred\" value=\"yes\" checked class=\"ms-2\" x-on:change=\"save\"><sl:icon name=\"star\"/> Star</sl:toggle>",
        "<sl:toggle name=\"pinned\" disabled aria-invalid=\"true\">Pin</sl:toggle>"));

    HtmlApproval.verifyRenders("toggle", tester, snippets);
  }

  @Test
  void toggleGroup() {
    List<String> snippets = new ArrayList<>();
    for (String type : LibraryComponents.registry().get("toggle-group").prop("type").values()) {
      snippets.add("""
          <sl:toggle-group type="%s" name="style" aria-label="Style">
            <sl:toggle-group-item value="bold" checked>Bold</sl:toggle-group-item>
            <sl:toggle-group-item value="italic">Italic</sl:toggle-group-item>
          </sl:toggle-group>""".formatted(type));
    }
    for (String variant : LibraryComponents.registry().get("toggle-group").prop("variant").values()) {
      snippets.add("""
          <sl:toggle-group variant="%s" name="view" aria-label="View">
            <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          </sl:toggle-group>""".formatted(variant));
    }
    for (String size : LibraryComponents.registry().get("toggle-group").prop("size").values()) {
      snippets.add("""
          <sl:toggle-group size="%s" name="view" aria-label="View">
            <sl:toggle-group-item value="list">List</sl:toggle-group-item>
          </sl:toggle-group>""".formatted(size));
    }
    for (String spacing : LibraryComponents.registry().get("toggle-group").prop("spacing").values()) {
      snippets.add("<sl:toggle-group spacing=\"%s\" name=\"view\" aria-label=\"View\"/>".formatted(spacing));
    }
    for (String orientation : LibraryComponents.registry().get("toggle-group").prop("orientation").values()) {
      snippets.add("<sl:toggle-group orientation=\"%s\" name=\"view\" aria-label=\"View\"/>".formatted(orientation));
    }
    snippets.addAll(List.of(
        """
            <sl:toggle-group disabled name="view" aria-labelledby="view-label" class="mt-2" id="view">
              <sl:toggle-group-item value="list" aria-label="List" class="ms-1"><sl:icon name="list"/></sl:toggle-group-item>
              <sl:toggle-group-item value="grid" name="other" disabled>Grid</sl:toggle-group-item>
            </sl:toggle-group>""",
        "<sl:toggle-group-item value=\"list\" aria-invalid=\"true\">Outside a group</sl:toggle-group-item>"));

    HtmlApproval.verifyRenders("toggle-group", tester, snippets);
  }
}
