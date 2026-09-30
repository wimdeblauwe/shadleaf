package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The basic form controls, {@code <sl:label>}, {@code <sl:input>}, {@code <sl:textarea>} and
 * {@code <sl:native-select>}, each rendered into {@code src/test/resources/approved/<name>.approved.html}. Their
 * behaviour with {@code th:field} is in {@link FormControlsBindingTest}.
 */
class FormControlsApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void label() {
    HtmlApproval.verifyRenders("label", tester, List.of(
        "<sl:label for=\"email\">Email</sl:label>",
        "<sl:label><input type=\"checkbox\" name=\"terms\"> Accept the terms</sl:label>",
        "<sl:label for=\"email\" class=\"ms-2\" id=\"email-label\">Email</sl:label>"));
  }

  @Test
  void input() {
    List<String> snippets = new ArrayList<>();
    for (String type : LibraryComponents.registry().get("input").prop("type").values()) {
      snippets.add("<sl:input type=\"%s\" name=\"x\"/>".formatted(type));
    }
    snippets.addAll(List.of(
        "<sl:input/>",
        "<sl:input type=\"email\" id=\"email\" name=\"email\" placeholder=\"you@example.com\" required/>",
        "<sl:input name=\"code\" disabled value=\"ABC\"/>",
        "<sl:input name=\"email\" aria-invalid=\"true\" aria-describedby=\"email-error\"/>",
        "<sl:input th:type=\"${'search'}\" name=\"q\" hx-get=\"/search\" hx-trigger=\"input changed delay:300ms\" class=\"w-64\"/>"));

    HtmlApproval.verifyRenders("input", tester, snippets);
  }

  @Test
  void textarea() {
    HtmlApproval.verifyRenders("textarea", tester, List.of(
        "<sl:textarea name=\"message\"/>",
        "<sl:textarea name=\"message\" placeholder=\"Type your message here.\" rows=\"4\"></sl:textarea>",
        "<sl:textarea name=\"message\">Initial text</sl:textarea>",
        "<sl:textarea name=\"message\" disabled aria-invalid=\"true\" class=\"min-h-32\"/>"));
  }

  @Test
  void nativeSelect() {
    List<String> snippets = new ArrayList<>();
    for (String size : LibraryComponents.registry().get("native-select").prop("size").values()) {
      snippets.add("""
          <sl:native-select size="%s" name="status">
            <option value="">Select status</option>
            <option value="todo">Todo</option>
          </sl:native-select>""".formatted(size));
    }
    snippets.addAll(List.of(
        """
            <sl:native-select id="department" name="department" class="w-full" required disabled>
              <optgroup label="Engineering">
                <option value="frontend">Frontend</option>
                <option value="backend">Backend</option>
              </optgroup>
            </sl:native-select>""",
        """
            <sl:native-select name="tags" multiple aria-invalid="true">
              <option>One</option>
              <option>Two</option>
            </sl:native-select>"""));

    HtmlApproval.verifyRenders("native-select", tester, snippets);
  }
}
