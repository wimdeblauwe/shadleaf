package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The form controls, {@code <sl:label>}, {@code <sl:input>}, {@code <sl:textarea>}, {@code <sl:native-select>},
 * {@code <sl:checkbox>}, {@code <sl:radio-group>} and {@code <sl:switch>}, each rendered into {@code src/test/resources/approved/<name>.approved.html}. Their
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
        "<sl:input name=\"reference\" value=\"ORD-2026-0042\" readonly/>",
        "<sl:input name=\"email\" value=\"wim@\" readonly aria-invalid=\"true\"/>",
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
        "<sl:textarea name=\"message\" disabled aria-invalid=\"true\" class=\"min-h-32\"/>",
        "<sl:textarea name=\"notes\" readonly>Leave it at the back door.</sl:textarea>"));
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

  @Test
  void checkbox() {
    HtmlApproval.verifyRenders("checkbox", tester, List.of(
        "<sl:checkbox name=\"terms\"/>",
        "<sl:checkbox id=\"terms\" name=\"terms\" value=\"yes\" checked required/>",
        "<sl:checkbox name=\"terms\" disabled aria-invalid=\"true\" class=\"ms-2\"/>",
        "<sl:label><sl:checkbox name=\"newsletter\"/>Send me the newsletter</sl:label>"));
  }

  @Test
  void radioGroup() {
    List<String> snippets = new ArrayList<>();
    for (String orientation : LibraryComponents.registry().get("radio-group").prop("orientation").values()) {
      snippets.add("""
          <sl:radio-group orientation="%s" aria-label="Plan">
            <sl:label><sl:radio-group-item name="plan" value="free" checked/>Free</sl:label>
            <sl:label><sl:radio-group-item name="plan" value="pro"/>Pro</sl:label>
          </sl:radio-group>""".formatted(orientation));
    }
    snippets.addAll(List.of(
        "<sl:radio-group aria-labelledby=\"plan-legend\" class=\"mt-2\"/>",
        "<sl:radio-group-item name=\"plan\" value=\"free\" disabled aria-invalid=\"true\" class=\"ms-2\"/>"));

    HtmlApproval.verifyRenders("radio-group", tester, snippets);
  }

  @Test
  void switchComponent() {
    List<String> snippets = new ArrayList<>();
    for (String size : LibraryComponents.registry().get("switch").prop("size").values()) {
      snippets.add("<sl:switch size=\"%s\" name=\"notifications\"/>".formatted(size));
    }
    snippets.addAll(List.of(
        "<sl:switch id=\"airplane-mode\" name=\"airplaneMode\" checked/>",
        "<sl:switch name=\"airplaneMode\" disabled aria-invalid=\"true\" class=\"ms-2\"/>"));

    HtmlApproval.verifyRenders("switch", tester, snippets);
  }
}
