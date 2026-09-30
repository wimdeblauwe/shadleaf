package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:select>} and its parts, rendered into {@code src/test/resources/approved/select.approved.html}. The
 * behaviour with {@code th:field} is in {@link SelectBindingTest}.
 */
class SelectApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void selectAndParts() {
    List<String> snippets = new ArrayList<>();
    for (String size : LibraryComponents.registry().get("select").prop("size").values()) {
      snippets.add("""
          <sl:select size="%s" name="fruit" aria-label="Fruit">
            <sl:select-item value="apple">Apple</sl:select-item>
          </sl:select>""".formatted(size));
    }
    snippets.addAll(List.of(
        """
            <sl:select id="fruit" name="fruit" placeholder="Select a fruit" class="w-full" required>
              <sl:select-group label="Fruits">
                <sl:select-item value="apple">Apple</sl:select-item>
                <sl:select-item value="banana" disabled>Banana</sl:select-item>
              </sl:select-group>
              <sl:select-separator/>
              <sl:select-group label="Vegetables">
                <sl:select-item value="carrot" selected>Carrot</sl:select-item>
              </sl:select-group>
            </sl:select>""",
        """
            <sl:select name="theme" aria-label="Theme" disabled aria-invalid="true" hx-post="/theme" hx-trigger="change">
              <sl:select-item value="light"><sl:icon name="sun"/>Light</sl:select-item>
            </sl:select>""",
        "<sl:select-item th:value=\"${'dark'}\">Dark</sl:select-item>",
        "<sl:select-group th:label=\"${'Fruits'}\" class=\"x\"><sl:select-item value=\"a\">A</sl:select-item></sl:select-group>",
        "<sl:select-separator/>"));

    HtmlApproval.verifyRenders("select", tester, snippets);
  }
}
