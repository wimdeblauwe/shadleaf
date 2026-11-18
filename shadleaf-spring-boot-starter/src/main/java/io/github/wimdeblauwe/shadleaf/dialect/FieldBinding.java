package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * What {@code sl:field} or {@code sl:field-set} tells its parts, available to them as {@code slField}: the
 * {@code th:field} they share, the ids that tie the label, the control, the description and the error together, and
 * the binding's error messages. Built by {@link FieldBindings}.
 * <p>
 * The parent sets it with {@code th:with} around its {@code <sl:slot/>}. Slot content sees the receiving template's
 * local variables, so every part and control in the field reads it without the author repeating the field.
 */
public final class FieldBinding {

  private final @Nullable String field;
  private final @Nullable String id;
  private final @Nullable String descriptionId;
  private final @Nullable String errorId;
  private final List<String> errors;
  private final @Nullable String describedBy;
  private final boolean group;
  private final @Nullable String validateField;
  private final boolean validateContainer;
  private boolean claimed;

  FieldBinding(@Nullable String field, @Nullable String id, @Nullable String descriptionId, @Nullable String errorId,
      List<String> errors, @Nullable String describedBy, boolean group) {
    this(field, id, descriptionId, errorId, errors, describedBy, group, null, false);
  }

  private FieldBinding(@Nullable String field, @Nullable String id, @Nullable String descriptionId,
      @Nullable String errorId, List<String> errors, @Nullable String describedBy, boolean group,
      @Nullable String validateField, boolean validateContainer) {
    this.field = field;
    this.id = id;
    this.descriptionId = descriptionId;
    this.errorId = errorId;
    this.errors = List.copyOf(errors);
    this.describedBy = describedBy;
    this.group = group;
    this.validateField = validateField;
    this.validateContainer = validateContainer;
  }

  /**
   * This binding, validated as the user types: under the request parameter name {@code validateField}
   * ({@code email}, {@code address.street}), the one the validation request names in its {@code SL-Validate-Field}
   * header. {@code container} tells whether this field or fieldset is the element htmx swaps, or one option of the
   * {@code sl:field-set} that is.
   */
  FieldBinding validated(String validateField, boolean container) {
    return new FieldBinding(field, id, descriptionId, errorId, errors, describedBy, group, validateField, container);
  }

  /** The unevaluated {@code th:field} expression ({@code *{email}}), or {@code null} for a field without one. */
  public @Nullable String getField() {
    return field;
  }

  /** The id of the control, which the label's {@code for} points at; {@code null} for a group. */
  public @Nullable String getId() {
    return id;
  }

  /** The id of the description ({@code email-description}), or {@code null} when there is nothing to base it on. */
  public @Nullable String getDescriptionId() {
    return descriptionId;
  }

  /** The id of the error message ({@code email-error}), or {@code null} when there is nothing to base it on. */
  public @Nullable String getErrorId() {
    return errorId;
  }

  /** The binding's error messages for this field, each once, in order. Empty for one option of a group. */
  public List<String> getErrors() {
    return errors;
  }

  /** Whether the binding has errors for this field. */
  public boolean isInvalid() {
    return !errors.isEmpty();
  }

  /**
   * The ids for the control's (or, for a group, the fieldset's) {@code aria-describedby}: the description's when the
   * field has one, the error's when it has one and the binding has errors (or the field has no binding, so the
   * error shows its own content). {@code null} when there is neither.
   */
  public @Nullable String getDescribedBy() {
    return describedBy;
  }

  /** Whether this is a {@code sl:field-set}'s binding, shared by several controls (radio buttons, checkboxes). */
  public boolean isGroup() {
    return group;
  }

  /**
   * The request parameter name the controls of this field are validated under while the user types
   * ({@code email}), or {@code null} when they are not: outside a {@code sl:form} with {@code validate}, without a
   * control with {@code sl:validate}, or without {@code th:field}.
   */
  public @Nullable String getValidateField() {
    return validateField;
  }

  /**
   * The value of this field's (or fieldset's) {@code data-validate-field}, which a validation response is selected
   * by: {@link #getValidateField()} on the element htmx swaps, {@code null} on an option of a group, which the
   * group's fieldset is swapped for.
   */
  public @Nullable String getValidateMarker() {
    return validateContainer ? validateField : null;
  }

  /**
   * The class of the element a validated control swaps: {@code field}, or {@code field-set} for a group and its
   * options.
   */
  public String getValidateTarget() {
    return group || !validateContainer ? "field-set" : "field";
  }

  /**
   * Gives the id and {@code aria-describedby} to the first control that asks, so a second control in the same field
   * does not repeat the id.
   */
  boolean claim() {
    if (claimed) {
      return false;
    }
    claimed = true;
    return true;
  }
}
