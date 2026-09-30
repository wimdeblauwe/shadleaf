package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The field family, {@code <sl:field>} and its parts, rendered into
 * {@code src/test/resources/approved/field.approved.html}, bound to a {@link FormModel} with an error on
 * {@code email}, {@code terms}, {@code plan} and {@code toppings}. The binding's behaviour is in
 * {@link FieldBindingTest}.
 */
class FieldApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void field() {
    List<String> snippets = new ArrayList<>();
    for (String orientation : LibraryComponents.registry().get("field").prop("orientation").values()) {
      snippets.add("""
          <sl:field th:field="*{terms}" orientation="%s">
            <sl:switch/>
            <sl:field-label>Notifications</sl:field-label>
          </sl:field>""".formatted(orientation));
    }
    for (String variant : LibraryComponents.registry().get("field-legend").prop("variant").values()) {
      snippets.add("<sl:field-set><sl:field-legend variant=\"%s\">Address</sl:field-legend></sl:field-set>"
          .formatted(variant));
    }
    snippets.addAll(List.of(
        """
            <sl:field th:field="*{name}">
              <sl:field-label>Name</sl:field-label>
              <sl:input autocomplete="name"/>
              <sl:field-description>As on your passport.</sl:field-description>
              <sl:field-error/>
            </sl:field>""",
        """
            <sl:field th:field="*{email}" class="mt-4">
              <sl:field-label>Email</sl:field-label>
              <sl:input type="email" aria-describedby="email-hint"/>
              <sl:field-description>We never share it.</sl:field-description>
              <sl:field-error/>
            </sl:field>""",
        """
            <sl:field th:field="*{terms}" orientation="horizontal">
              <sl:checkbox/>
              <sl:field-content>
                <sl:field-label>I accept the terms</sl:field-label>
                <sl:field-description>Read them first.</sl:field-description>
                <sl:field-error/>
              </sl:field-content>
            </sl:field>""",
        """
            <sl:field-group>
              <sl:field-set th:field="*{plan}">
                <sl:field-legend variant="label">Plan</sl:field-legend>
                <sl:radio-group>
                  <sl:label><sl:radio-group-item value="free"/>Free</sl:label>
                  <sl:label><sl:radio-group-item value="pro"/>Pro</sl:label>
                </sl:radio-group>
                <sl:field-error/>
              </sl:field-set>
              <sl:field-set th:field="*{toppings}">
                <sl:field-legend variant="label">Toppings</sl:field-legend>
                <sl:field-description>Pick any.</sl:field-description>
                <sl:field orientation="horizontal">
                  <sl:checkbox value="cheese"/>
                  <sl:field-label>Cheese</sl:field-label>
                </sl:field>
                <sl:field orientation="horizontal">
                  <sl:checkbox value="olives"/>
                  <sl:field-label>Olives</sl:field-label>
                </sl:field>
                <sl:field-error>Pick one or two.</sl:field-error>
              </sl:field-set>
            </sl:field-group>""",
        """
            <sl:field for="query">
              <sl:field-label>Search</sl:field-label>
              <sl:input type="search" name="q"/>
              <sl:field-error>Type at least two characters.</sl:field-error>
            </sl:field>"""));

    FormModel form = FormModel.of(
        Map.of("name", "Wim", "email", "nope", "terms", false, "plan", "", "toppings", List.of()),
        Map.of("email", List.of("must be a valid email address", "is already registered"),
            "terms", List.of("must be accepted"),
            "plan", List.of("must not be blank"),
            "toppings", List.of("pick at least one")));
    HtmlApproval.verifyRenders("field", tester, snippets.stream().map(FormModel::wrap).toList(), form.variables());
  }
}
