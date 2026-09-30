package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.List;

/**
 * What {@code sl:form-errors} shows: the errors of the form object the enclosing {@code th:object} selects (the
 * global ones, or all of them), and the id of the summary. Built by {@link FieldBindings#form(boolean, boolean)}.
 */
public final class FormErrors {

  private final String id;
  private final List<String> errors;

  FormErrors(String id, List<String> errors) {
    this.id = id;
    this.errors = List.copyOf(errors);
  }

  /** The id of the summary: the form object's name plus {@code -errors} ({@code signup-errors}). */
  public String getId() {
    return id;
  }

  /** The error messages to show, in the binding's order: global errors first, each message once per field. */
  public List<String> getErrors() {
    return errors;
  }

  /** Whether there are errors to show. */
  public boolean isInvalid() {
    return !errors.isEmpty();
  }
}
