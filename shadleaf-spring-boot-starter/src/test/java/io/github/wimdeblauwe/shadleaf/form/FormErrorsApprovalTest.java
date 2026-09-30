package io.github.wimdeblauwe.shadleaf.form;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:form-errors>}, rendered into {@code src/test/resources/approved/form-errors.approved.html} with two
 * global errors and a field error: the default title, a title of its own, all errors, and content of its own. The binding's
 * behaviour is in {@link FormErrorsBindingTest}.
 */
class FormErrorsApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void formErrors() {
    FormModel form = FormModel.of(Map.of(), Map.of("email", List.of("is taken")),
        List.of("The passwords do not match.", "The service is not available."));
    HtmlApproval.verifyRenders("form-errors", tester, List.of(
        FormModel.wrap("<sl:form-errors/>"),
        FormModel.wrap("<sl:form-errors title=\"Could not sign you up\" level=\"2\"/>"),
        FormModel.wrap("<sl:form-errors show=\"all\"/>"),
        FormModel.wrap("<sl:form-errors>The payment service did not answer. Try again in a minute.</sl:form-errors>")),
        form.variables());
  }
}
