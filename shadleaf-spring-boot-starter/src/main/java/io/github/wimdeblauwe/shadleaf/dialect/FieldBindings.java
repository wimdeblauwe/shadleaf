package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.spring6.context.IThymeleafBindStatus;
import org.thymeleaf.spring6.util.FieldUtils;
import org.thymeleaf.util.StringUtils;

/**
 * The expression object {@code #slFields}: how {@code sl:field}, {@code sl:field-set} and the form controls share a
 * {@code th:field}.
 * <p>
 * The field reads its {@code th:field} (still the unevaluated {@code *{email}}) and asks Spring's binding for the
 * field's name and errors, then derives the ids its parts use. A control inside it takes the field's
 * {@code th:field}, {@code id} and {@code aria-describedby} through {@link #control(Attrs, Object)}, unless it was
 * given its own.
 * <p>
 * The id is the one {@code th:field} gives a text input ({@code FieldUtils.idFromName}: {@code email},
 * {@code address.street}), set explicitly on the control, which Spring respects. So a checkbox in a field is
 * {@code terms}, not Spring's numbered {@code terms1}, and the label can point at it. One option of a group (a field
 * without {@code th:field} inside a {@code sl:field-set} with one) takes the next number from the same sequence Spring
 * numbers checkboxes and radio buttons with: {@code toppings1}, {@code toppings2}.
 */
public final class FieldBindings {

  private static final String FIELD_ATTRIBUTE = "th:field";
  private static final String ID_ATTRIBUTE = "id";
  private static final String DESCRIBED_BY_ATTRIBUTE = "aria-describedby";
  private static final String EXPRESSION_PREFIX = "th:";

  private final IExpressionContext context;

  FieldBindings(IExpressionContext context) {
    this.context = context;
  }

  /**
   * The binding of a {@code sl:field}.
   *
   * @param attrs          the field's attributes, which hold its {@code th:field}, if any
   * @param controlId      the field's {@code for} prop: the control's id, for a field without {@code th:field}
   * @param enclosing      the enclosing {@code slField}; a field without {@code th:field} inside a group with one is
   *                       one of its options
   * @param hasDescription whether the field has a {@code sl:field-description}
   * @param hasError       whether the field has a {@code sl:field-error}
   */
  public FieldBinding field(Attrs attrs, @Nullable String controlId, @Nullable Object enclosing,
      boolean hasDescription, boolean hasError) {
    String field = attrs.get(FIELD_ATTRIBUTE);
    boolean option = false;
    if (StringUtils.isEmptyOrWhitespace(field) && enclosing instanceof FieldBinding group && group.isGroup()) {
      field = group.getField();
      option = field != null;
    }
    if (StringUtils.isEmptyOrWhitespace(field)) {
      field = null;
    }

    String id = StringUtils.isEmptyOrWhitespace(controlId) ? null : controlId;
    if (id == null && field != null) {
      String base = idFromField(field);
      id = option ? base + nextSequence(base) : base;
    }
    List<String> errors = field == null || option ? List.of() : errors(field);
    return binding(field, id, id, errors, hasDescription, hasError, false);
  }

  /**
   * The binding of a {@code sl:field-set}: its {@code th:field} is shared by the controls inside it (radio buttons,
   * checkboxes), which keep Spring's numbered ids. The description and error describe the fieldset.
   */
  public FieldBinding fieldSet(Attrs attrs, boolean hasDescription, boolean hasError) {
    String field = attrs.get(FIELD_ATTRIBUTE);
    if (StringUtils.isEmptyOrWhitespace(field)) {
      return binding(null, null, null, List.of(), hasDescription, hasError, true);
    }
    return binding(field, null, idFromField(field), errors(field), hasDescription, hasError, true);
  }

  /**
   * The attributes of a control inside a field: its own, plus the field's {@code th:field} when it has none. The
   * first control in a {@code sl:field} also gets the field's {@code id}, unless it has one, and the field's
   * {@code aria-describedby} ids after any it was given. A {@code th:aria-describedby} is left alone: it cannot be
   * merged before it is evaluated.
   *
   * @param binding the {@code slField} variable, {@code null} outside a field
   */
  public Attrs control(Attrs attrs, @Nullable Object binding) {
    if (!(binding instanceof FieldBinding field)) {
      return attrs;
    }
    Map<String, @Nullable String> values = new LinkedHashMap<>(attrs);
    if (field.getField() != null && !attrs.containsKey(FIELD_ATTRIBUTE)) {
      values.put(FIELD_ATTRIBUTE, field.getField());
    }
    if (!field.isGroup() && field.claim()) {
      if (field.getId() != null && !has(attrs, ID_ATTRIBUTE)) {
        values.put(ID_ATTRIBUTE, field.getId());
      }
      String describedBy = field.getDescribedBy();
      if (describedBy != null && !attrs.containsKey(EXPRESSION_PREFIX + DESCRIBED_BY_ATTRIBUTE)) {
        String own = attrs.get(DESCRIBED_BY_ATTRIBUTE);
        values.put(DESCRIBED_BY_ATTRIBUTE, StringUtils.isEmptyOrWhitespace(own) ? describedBy : own + " " + describedBy);
      }
    }
    return new Attrs(values);
  }

  /** Whether a control's {@code th:field} (as {@link #control} left it) has errors: its {@code aria-invalid}. */
  public boolean invalid(Map<String, ?> attrs) {
    return attrs.get(FIELD_ATTRIBUTE) instanceof String field
        && !field.isBlank()
        && FieldUtils.hasErrors(context, field);
  }

  private static FieldBinding binding(@Nullable String field, @Nullable String id, @Nullable String base,
      List<String> errors, boolean hasDescription, boolean hasError, boolean group) {
    String descriptionId = base == null ? null : base + "-description";
    String errorId = base == null ? null : base + "-error";
    List<String> describedBy = new ArrayList<>();
    if (hasDescription && descriptionId != null) {
      describedBy.add(descriptionId);
    }
    // Without a binding, a sl:field-error only renders with content of its own, so it is there to point at.
    if (hasError && errorId != null && (field == null || !errors.isEmpty())) {
      describedBy.add(errorId);
    }
    return new FieldBinding(field, id, descriptionId, errorId, errors,
        describedBy.isEmpty() ? null : String.join(" ", describedBy), group);
  }

  private String idFromField(String field) {
    IThymeleafBindStatus bindStatus = FieldUtils.getBindStatus(context, field);
    String name = bindStatus.getExpression();
    return FieldUtils.idFromName(name == null ? "" : name);
  }

  private int nextSequence(String id) {
    if (!(context instanceof ITemplateContext templateContext)) {
      throw new IllegalStateException("#slFields needs a template context to number ids");
    }
    return templateContext.getIdentifierSequences().getAndIncrementIDSeq(id);
  }

  private List<String> errors(String field) {
    return List.copyOf(new LinkedHashSet<>(FieldUtils.errors(context, field)));
  }

  private static boolean has(Map<String, ?> attrs, String name) {
    return attrs.containsKey(name) || attrs.containsKey(EXPRESSION_PREFIX + name);
  }
}
