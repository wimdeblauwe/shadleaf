package io.github.wimdeblauwe.shadleaf.test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MapBindingResult;

/**
 * A form object for a snippet with {@code th:field}, without writing a backing class: a map of field values and a
 * {@link MapBindingResult} holding the given field errors and global errors. {@link #wrap(String)} puts the snippet inside
 * {@code th:object="${form}"}.
 * <p>
 * For approvals and docs previews. Tests of the binding itself use a real bean, as an application does.
 */
public final class FormModel {

  public static final String NAME = "form";

  private final Map<String, Object> values;
  private final Map<String, List<String>> errors;
  private final List<String> globalErrors;

  private FormModel(Map<String, Object> values, Map<String, List<String>> errors, List<String> globalErrors) {
    this.values = new LinkedHashMap<>(values);
    this.errors = new LinkedHashMap<>(errors);
    this.globalErrors = List.copyOf(globalErrors);
  }

  /**
   * @param values the field values, by field name
   * @param errors the error messages, by field name
   */
  public static FormModel of(Map<String, ?> values, Map<String, List<String>> errors) {
    return of(values, errors, List.of());
  }

  /**
   * @param values       the field values, by field name
   * @param errors       the error messages, by field name
   * @param globalErrors the messages of errors that belong to the whole form, as {@code reject(...)} adds them
   */
  public static FormModel of(Map<String, ?> values, Map<String, List<String>> errors, List<String> globalErrors) {
    return new FormModel(new LinkedHashMap<>(values), errors, globalErrors);
  }

  /** The variables to render the wrapped snippet with. */
  public Map<String, Object> variables() {
    MapBindingResult bindingResult = new MapBindingResult(values, NAME);
    errors.forEach((field, messages) -> messages.forEach(message ->
        bindingResult.rejectValue(field, "Invalid", message)));
    globalErrors.forEach(message -> bindingResult.reject("Invalid", message));
    Map<String, Object> variables = new HashMap<>();
    variables.put(NAME, values);
    variables.put(BindingResult.MODEL_KEY_PREFIX + NAME, bindingResult);
    return variables;
  }

  /** The snippet inside a {@code th:block} that selects the form object. */
  public static String wrap(String snippet) {
    return "<th:block th:object=\"${" + NAME + "}\">" + snippet + "</th:block>";
  }
}
