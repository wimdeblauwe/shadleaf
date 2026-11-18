package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.util.unit.DataSize;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.spring6.context.IThymeleafBindStatus;
import org.thymeleaf.spring6.naming.SpringContextVariableNames;
import org.thymeleaf.spring6.util.DetailedError;
import org.thymeleaf.spring6.util.FieldUtils;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.standard.expression.VariableExpression;
import org.thymeleaf.util.StringUtils;
import org.thymeleaf.web.IWebRequest;

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
 * <p>
 * {@code sl:form-errors} reads the errors of the form object through {@link #form(boolean, boolean)}. With
 * {@code autofocus} and nothing to show, it leaves the focus to the first control with errors, which
 * {@link #control(Attrs, Object)} gives {@code autofocus}. That state lives in this object: Thymeleaf builds one per
 * template execution (the expression object is cacheable), and the form renders top to bottom, so the summary always
 * decides before the controls below it ask.
 * <p>
 * Validating as the user types: inside a {@code sl:form} with {@code validate} (which sets
 * {@value #FORM_VALIDATE_VARIABLE}), or when a control in the field has {@code sl:validate} itself, a field with
 * {@code th:field} knows the request parameter name it is validated under ({@link FieldBinding#getValidateField()}).
 * {@link #control(Attrs, Object)} then gives its controls {@code sl:validate} with the form's URL, which
 * {@link ValidateAttributeProcessor} turns into htmx attributes. While a validation request renders (it carries the
 * {@value #VALIDATE_FIELD_HEADER} header), {@link #affected(Object, String)} tells a field with {@code depends-on}
 * whether to wrap itself in an {@code <hx-partial>}, and no control takes {@code autofocus}.
 */
public final class FieldBindings {

  private static final String FIELD_ATTRIBUTE = "th:field";
  private static final String ID_ATTRIBUTE = "id";
  private static final String DESCRIBED_BY_ATTRIBUTE = "aria-describedby";
  private static final String AUTOFOCUS_ATTRIBUTE = "autofocus";
  private static final String DISABLED_ATTRIBUTE = "disabled";
  private static final String VALUE_ATTRIBUTE = "value";
  private static final String EXPRESSION_PREFIX = "th:";

  /** The request header with which a validated control names its field: {@code SL-Validate-Field: email}. */
  public static final String VALIDATE_FIELD_HEADER = "SL-Validate-Field";
  /** The local variable {@code sl:form} sets to its {@code validate} URL. */
  public static final String FORM_VALIDATE_VARIABLE = "slFormValidate";
  static final String VALIDATE_ATTRIBUTE = ShadleafDialect.PREFIX + ":" + ValidateAttributeProcessor.ATTR_NAME;

  private final IExpressionContext context;
  /** The form objects whose first control with errors takes {@code autofocus}, by name ({@code signup}). */
  private final Set<String> focusFirstInvalid = new HashSet<>();

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
    return field(attrs, controlId, enclosing, hasDescription, hasError, null);
  }

  /**
   * The binding of a {@code sl:field}, which knows whether its controls are validated as the user types: inside a
   * {@code sl:form} with {@code validate}, or when a control in its content has {@code sl:validate}. One option of a
   * validated group is validated with the group.
   *
   * @param slots the field's content, {@code null} when no control in it can have {@code sl:validate}
   */
  public FieldBinding field(Attrs attrs, @Nullable String controlId, @Nullable Object enclosing,
      boolean hasDescription, boolean hasError, @Nullable Slots slots) {
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
    FieldBinding binding = binding(field, id, id, errors, hasDescription, hasError, false);
    if (option) {
      String groupField = ((FieldBinding) enclosing).getValidateField();
      return groupField == null ? binding : binding.validated(groupField, false);
    }
    return field != null && validating(slots) ? binding.validated(parameterName(field), true) : binding;
  }

  /**
   * The binding of a {@code sl:field-set}: its {@code th:field} is shared by the controls inside it (radio buttons,
   * checkboxes), which keep Spring's numbered ids. The description and error describe the fieldset.
   */
  public FieldBinding fieldSet(Attrs attrs, boolean hasDescription, boolean hasError) {
    return fieldSet(attrs, hasDescription, hasError, null);
  }

  /**
   * The binding of a {@code sl:field-set}, which knows whether its controls are validated as the user types, as
   * {@link #field(Attrs, String, Object, boolean, boolean, Slots)} does.
   *
   * @param slots the fieldset's content, {@code null} when no control in it can have {@code sl:validate}
   */
  public FieldBinding fieldSet(Attrs attrs, boolean hasDescription, boolean hasError, @Nullable Slots slots) {
    String field = attrs.get(FIELD_ATTRIBUTE);
    if (StringUtils.isEmptyOrWhitespace(field)) {
      return binding(null, null, null, List.of(), hasDescription, hasError, true);
    }
    FieldBinding binding = binding(field, null, idFromField(field), errors(field), hasDescription, hasError, true);
    return validating(slots) ? binding.validated(parameterName(field), true) : binding;
  }

  /**
   * Whether a field or fieldset with {@code depends-on} is swapped along with the field a validation request is
   * rendered for: the request names one of {@code dependsOn} in its {@value #VALIDATE_FIELD_HEADER} header, not this
   * field, and the request's parameter of this field is not blank. A field the user has not reached yet is left
   * alone, so it does not show errors such as "must not be blank". The field it was not swapped with, the main swap,
   * must not be wrapped either: htmx takes partials out of the response before it selects the main swap's content.
   *
   * @param binding   the field's {@code slField}
   * @param dependsOn the names of the fields this one depends on, separated by spaces or commas
   */
  public boolean affected(@Nullable Object binding, @Nullable String dependsOn) {
    if (!(binding instanceof FieldBinding field) || field.getValidateMarker() == null
        || StringUtils.isEmptyOrWhitespace(dependsOn)) {
      return false;
    }
    IWebRequest request = request();
    String validated = request == null ? null : request.getHeaderValue(VALIDATE_FIELD_HEADER);
    String name = field.getValidateMarker();
    if (validated == null || validated.equals(name) || !dependsOn(dependsOn).contains(validated)) {
      return false;
    }
    String[] values = request.getParameterValues(name);
    return values != null && Arrays.stream(values).anyMatch(value -> !StringUtils.isEmptyOrWhitespace(value));
  }

  /** The field names of a {@code depends-on}: separated by spaces, commas or both. */
  public static List<String> dependsOn(@Nullable String dependsOn) {
    if (StringUtils.isEmptyOrWhitespace(dependsOn)) {
      return List.of();
    }
    return Arrays.stream(dependsOn.trim().split("[\\s,]+")).filter(name -> !name.isEmpty()).toList();
  }

  /**
   * Whether the page renders for a validation request: one with the {@value #VALIDATE_FIELD_HEADER} header, sent by
   * a control with {@code sl:validate}.
   */
  public boolean isValidationRequest() {
    IWebRequest request = request();
    return request != null && request.getHeaderValue(VALIDATE_FIELD_HEADER) != null;
  }

  /**
   * The attributes of a control: its own, plus, inside a field, the field's {@code th:field} when it has none. The
   * first control in a {@code sl:field} also gets the field's {@code id}, unless it has one, and the field's
   * {@code aria-describedby} ids after any it was given. A {@code th:aria-describedby} is left alone: it cannot be
   * merged before it is evaluated. The first control with errors in a form whose {@code sl:form-errors autofocus}
   * showed nothing gets {@code autofocus}, skipping a control with a {@code disabled} attribute, which cannot take
   * focus. A {@code th:disabled}, or a disabled fieldset around the control, is only known after this ran.
   *
   * @param binding the {@code slField} variable, {@code null} outside a field
   */
  public Attrs control(Attrs attrs, @Nullable Object binding) {
    if (!(binding instanceof FieldBinding) && focusFirstInvalid.isEmpty()) {
      return attrs;
    }
    Map<String, @Nullable String> values = new LinkedHashMap<>(attrs);
    if (binding instanceof FieldBinding field) {
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
          values.put(DESCRIBED_BY_ATTRIBUTE,
              StringUtils.isEmptyOrWhitespace(own) ? describedBy : own + " " + describedBy);
        }
      }
    }
    if (binding instanceof FieldBinding field && field.getValidateField() != null
        && !values.containsKey(VALIDATE_ATTRIBUTE) && formValidateUrl() != null) {
      values.put(VALIDATE_ATTRIBUTE, formValidateUrl());
    }
    if (!focusFirstInvalid.isEmpty() && !values.containsKey(DISABLED_ATTRIBUTE) && invalid(values)) {
      String form = boundObjectName();
      if (form != null && focusFirstInvalid.remove(form) && !has(values, AUTOFOCUS_ATTRIBUTE)) {
        values.put(AUTOFOCUS_ATTRIBUTE, AUTOFOCUS_ATTRIBUTE);
      }
    }
    return new Attrs(values);
  }

  /**
   * The errors of the form object the enclosing {@code th:object} selects, for {@code sl:form-errors}: the global
   * errors, and with {@code includeFieldErrors} the field errors after them, in the binding's order. A message shows
   * once per field (or once among the global errors), so two fields with the same message both keep it.
   *
   * @param autofocus whether the summary takes {@code autofocus}: when there is nothing to show, the first control
   *                  with errors below it takes it instead
   * @throws ShadleafComponentException outside a {@code th:object}, where there is no form object to read
   */
  public FormErrors form(boolean includeFieldErrors, boolean autofocus) {
    String form = boundObjectName();
    if (form == null) {
      throw new ShadleafComponentException(
          "sl:form-errors must be inside an element with th:object: it shows that form object's errors");
    }
    String id = FieldUtils.idFromName(form) + "-errors";
    List<DetailedError> errors = includeFieldErrors
        ? FieldUtils.detailedErrors(context)
        : FieldUtils.globalDetailedErrors(context);
    Set<List<String>> seen = new HashSet<>();
    List<String> messages = new ArrayList<>();
    for (DetailedError error : errors) {
      String field = error.isGlobal() ? "" : error.getFieldName();
      if (seen.add(List.of(field, String.valueOf(error.getMessage())))) {
        messages.add(error.getMessage());
      }
    }
    if (autofocus && messages.isEmpty() && !isValidationRequest()) {
      focusFirstInvalid.add(form);
    }
    return new FormErrors(id, messages);
  }

  /**
   * The values the inputs of a control will render with, read before they render: for {@code sl:date-picker}, which
   * shows them on its button. Without content, the control's own input: its {@code th:field} (or, inside a
   * {@code sl:field}, the field's), {@code th:value} or {@code value}. With content, the same for each {@code sl:input}
   * and {@code input} in it, in order. The value of a {@code th:field} is the one Spring writes into the input; an input
   * without any of the three has {@code ""}.
   *
   * @param binding the {@code slField} variable, {@code null} outside a field
   */
  public List<String> values(Attrs attrs, @Nullable Object binding, Slots slots) {
    if (!slots.hasDefault()) {
      String field = attrs.get(FIELD_ATTRIBUTE);
      if (StringUtils.isEmptyOrWhitespace(field) && binding instanceof FieldBinding enclosing && !enclosing.isGroup()) {
        field = enclosing.getField();
      }
      return List.of(value(field, attrs.get(EXPRESSION_PREFIX + VALUE_ATTRIBUTE), attrs.get(VALUE_ATTRIBUTE)));
    }
    List<String> values = new ArrayList<>();
    IModel content = slots.defaultSlot();
    for (int i = 0; i < content.size(); i++) {
      if (content.get(i) instanceof IProcessableElementTag tag && isInput(tag)) {
        values.add(value(tag.getAttributeValue(FIELD_ATTRIBUTE), tag.getAttributeValue(EXPRESSION_PREFIX + VALUE_ATTRIBUTE),
            tag.getAttributeValue(VALUE_ATTRIBUTE)));
      }
    }
    return values;
  }

  /**
   * The id of the control a template renders for {@code attrs}, which a {@code <label for>} next to it points at: its
   * {@code id} (or {@code th:id}, evaluated), the id {@code th:field} gives it ({@code files}), or else a new one with
   * the given prefix ({@code sl-file-upload-1}). Pass the attributes {@link #control(Attrs, Object)} returned, so the
   * id of an enclosing {@code sl:field} counts.
   */
  public String controlId(Attrs attrs, String prefix) {
    String id = attrs.get(ID_ATTRIBUTE);
    if (!StringUtils.isEmptyOrWhitespace(id)) {
      return id;
    }
    String idExpression = attrs.get(EXPRESSION_PREFIX + ID_ATTRIBUTE);
    if (!StringUtils.isEmptyOrWhitespace(idExpression)) {
      return Objects.toString(StandardExpressions.getExpressionParser(context.getConfiguration())
          .parseExpression(context, idExpression).execute(context), "");
    }
    String field = attrs.get(FIELD_ATTRIBUTE);
    if (!StringUtils.isEmptyOrWhitespace(field)) {
      return idFromField(field);
    }
    return prefix + nextSequence(prefix);
  }

  /**
   * The number of bytes of a size written as Spring Boot writes {@code spring.servlet.multipart.max-file-size}
   * ({@code 10MB}, {@code 500KB}, {@code 1024}), for {@code sl:file-upload}'s {@code max-size}; {@code null} for none.
   *
   * @throws ShadleafComponentException when the size cannot be read
   */
  public @Nullable Long maxSize(@Nullable String size) {
    if (StringUtils.isEmptyOrWhitespace(size)) {
      return null;
    }
    try {
      return DataSize.parse(size.trim()).toBytes();
    } catch (IllegalArgumentException e) {
      throw new ShadleafComponentException(
          "max-size=\"" + size + "\" is no size: write it as Spring Boot does, such as 10MB or 500KB", e);
    }
  }

  private String value(@Nullable String field, @Nullable String valueExpression, @Nullable String value) {
    if (!StringUtils.isEmptyOrWhitespace(field)) {
      return Objects.toString(FieldUtils.getBindStatus(context, field).getDisplayValue(), "");
    }
    if (!StringUtils.isEmptyOrWhitespace(valueExpression)) {
      Object evaluated = StandardExpressions.getExpressionParser(context.getConfiguration())
          .parseExpression(context, valueExpression).execute(context);
      return Objects.toString(evaluated, "");
    }
    return Objects.toString(value, "");
  }

  private boolean isInput(IProcessableElementTag tag) {
    String name = tag.getElementCompleteName().toLowerCase(Locale.ROOT);
    return name.equals("input") || name.endsWith(":input");
  }

  /** Whether a control's {@code th:field} (as {@link #control} left it) has errors: its {@code aria-invalid}. */
  public boolean invalid(Map<String, ?> attrs) {
    return attrs.get(FIELD_ATTRIBUTE) instanceof String field
        && !field.isBlank()
        && FieldUtils.hasErrors(context, field);
  }

  private @Nullable String boundObjectName() {
    return context.getVariable(SpringContextVariableNames.SPRING_BOUND_OBJECT_EXPRESSION)
        instanceof VariableExpression boundObject ? boundObject.getExpression() : null;
  }

  /**
   * Whether the controls of a field are validated as the user types: its form has a {@code validate} URL, or a
   * control in its content has {@code sl:validate} (other than {@code false}).
   */
  private boolean validating(@Nullable Slots slots) {
    if (formValidateUrl() != null) {
      return true;
    }
    if (slots == null) {
      return false;
    }
    IModel content = slots.defaultSlot();
    for (int i = 0; i < content.size(); i++) {
      if (content.get(i) instanceof IProcessableElementTag tag && tag.hasAttribute(VALIDATE_ATTRIBUTE)
          && !"false".equals(tag.getAttributeValue(VALIDATE_ATTRIBUTE))) {
        return true;
      }
    }
    return false;
  }

  /** The {@code validate} URL of the enclosing {@code sl:form}, {@code null} outside one or without it. */
  private @Nullable String formValidateUrl() {
    Object url = context.getVariable(FORM_VALIDATE_VARIABLE);
    return url == null || url.toString().isBlank() ? null : url.toString();
  }

  /** The request parameter name of a {@code th:field} ({@code *{address.street}} is {@code address.street}). */
  private String parameterName(String field) {
    String name = FieldUtils.getBindStatus(context, field).getExpression();
    return name == null ? "" : name;
  }

  private @Nullable IWebRequest request() {
    return context instanceof IWebContext web ? web.getExchange().getRequest() : null;
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
