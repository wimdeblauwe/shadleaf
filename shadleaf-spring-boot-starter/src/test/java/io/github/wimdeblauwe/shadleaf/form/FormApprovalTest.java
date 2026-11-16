package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:form>}, rendered into {@code src/test/resources/approved/form.approved.html}: a plain form, and one that
 * validates as the user types, with a field, a group and a field that depends on another. The behaviour is in
 * {@link FormValidateTest}.
 */
class FormApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void form() {
    FormModel form = FormModel.of(Map.of("title", "", "start", "10:00", "end", "09:30", "catering", "none"),
        Map.of("end", List.of("The meeting must end after it starts.")));
    HtmlApproval.verifyRenders("form", tester, List.of(
        """
            <sl:form th:object="${form}" th:action="@{/bookings}" method="post" novalidate>
              <sl:field th:field="*{title}"><sl:field-label>Title</sl:field-label><sl:input/></sl:field>
            </sl:form>""",
        """
            <sl:form th:object="${form}" th:action="@{/bookings}" th:validate="@{/bookings/validate}" method="post">
              <sl:field th:field="*{title}"><sl:field-label>Title</sl:field-label><sl:input/></sl:field>
              <sl:field th:field="*{start}"><sl:field-label>Start</sl:field-label><sl:input type="time"/></sl:field>
              <sl:field th:field="*{end}" depends-on="start">
                <sl:field-label>End</sl:field-label><sl:input type="time"/><sl:field-error/>
              </sl:field>
              <sl:field-set th:field="*{catering}">
                <sl:field-legend>Catering</sl:field-legend>
                <sl:radio-group>
                  <sl:field orientation="horizontal">
                    <sl:radio-group-item value="none"/><sl:field-label>None</sl:field-label>
                  </sl:field>
                  <sl:field orientation="horizontal">
                    <sl:radio-group-item value="lunch"/><sl:field-label>Lunch</sl:field-label>
                  </sl:field>
                </sl:radio-group>
              </sl:field-set>
              <sl:field orientation="horizontal">
                <sl:input sl:validate="false" th:field="*{title}" type="search"/>
              </sl:field>
            </sl:form>"""),
        form.variables());
  }
}
