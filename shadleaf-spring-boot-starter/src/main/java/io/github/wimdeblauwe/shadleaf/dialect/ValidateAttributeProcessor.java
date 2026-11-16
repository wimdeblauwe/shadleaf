package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * {@code sl:validate="@{/bookings/validate}"} on a form control: validates its field on the server while the user
 * edits it, with htmx 4. It writes, unless the author wrote the attribute in any spelling ({@code hx-x},
 * {@code data-hx-x}, {@code th:hx-x}, {@code hx:x}):
 * <ul>
 *   <li>{@code hx-post}: the URL, unless the control has another request attribute;</li>
 *   <li>{@code hx-trigger}: {@code input changed delay:200ms} while typing, {@code change} for a control that is picked
 *   (select, checkbox, radio button, date and time inputs, color, range, hidden);</li>
 *   <li>{@code hx-include="closest form"}: the whole form, so rules over several fields see every value;</li>
 *   <li>{@code hx-swap="outerMorph"}: morphing keeps focus, caret and the typed value;</li>
 *   <li>inside a {@code sl:field} (or {@code sl:field-set}) with {@code th:field}: {@code hx-target} (the field),
 *   {@code hx-select} (the same field in the response, by {@code data-validate-field}) and {@code hx-headers}, which
 *   names the field in {@value FieldBindings#VALIDATE_FIELD_HEADER}, for fields that {@code depends-on} it.</li>
 * </ul>
 * Shadleaf's script takes the form's files out of a request with that header ({@code validate.js}), so a validation
 * request never uploads them.
 * A file input gets none of them, and {@code sl:validate="false"} opts a control out of the validation its
 * {@code sl:form} gives it. On a {@code sl:} component the attribute is left for the component, which passes it to
 * its control (through {@code sl:attrs}, before this processor), where this processor runs and knows the element.
 * The {@code hx-headers} value is HCON ({@code SL-Validate-Field:'email'}): the value of an attribute a processor sets
 * is written as it is, so it cannot hold {@code "}.
 */
public class ValidateAttributeProcessor extends AbstractAttributeTagProcessor {

  static final String ATTR_NAME = "validate";

  // After sl:attrs (750), which copies the attribute from a component onto its control.
  static final int PRECEDENCE = 800;

  private static final String TRIGGER_TYPING = "input changed delay:200ms";
  private static final String TRIGGER_CHANGE = "change";
  private static final Set<String> PICKED_INPUT_TYPES = Set.of("checkbox", "radio", "date", "datetime-local",
      "month", "week", "time", "color", "range", "hidden");
  private static final List<String> REQUEST_ATTRIBUTES = List.of("hx-get", "hx-post", "hx-put", "hx-patch",
      "hx-delete", "hx-action");

  public ValidateAttributeProcessor(String dialectPrefix) {
    super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, PRECEDENCE, false);
  }

  @Override
  protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
      String attributeValue, IElementTagStructureHandler structureHandler) {
    String element = tag.getElementCompleteName().toLowerCase(Locale.ROOT);
    if (element.startsWith(getDialectPrefix() + ":")) {
      return;
    }
    structureHandler.removeAttribute(attributeName);
    String url = Objects.toString(resolve(context, attributeValue), "").trim();
    if (url.equals("false")) {
      return;
    }
    if (url.isEmpty()) {
      throw new ShadleafComponentException(
          "sl:validate needs the URL of the validation endpoint, such as sl:validate=\"@{/bookings/validate}\"");
    }
    String type = element.equals("input") ? Objects.toString(tag.getAttributeValue("type"), "text")
        .toLowerCase(Locale.ROOT) : null;
    if ("file".equals(type)) {
      return;
    }

    if (REQUEST_ATTRIBUTES.stream().noneMatch(name -> has(tag, name))) {
      structureHandler.setAttribute("hx-post", url);
    }
    boolean picked = element.equals("select") || (type != null && PICKED_INPUT_TYPES.contains(type));
    setUnlessPresent(tag, structureHandler, "hx-trigger", picked ? TRIGGER_CHANGE : TRIGGER_TYPING);
    setUnlessPresent(tag, structureHandler, "hx-include", "closest form");
    setUnlessPresent(tag, structureHandler, "hx-swap", "outerMorph");
    if (context.getVariable("slField") instanceof FieldBinding field && field.getValidateField() != null) {
      String name = escape(field.getValidateField());
      setUnlessPresent(tag, structureHandler, "hx-target", "closest ." + field.getValidateTarget());
      setUnlessPresent(tag, structureHandler, "hx-select", "[data-validate-field='" + name + "']");
      setUnlessPresent(tag, structureHandler, "hx-headers", FieldBindings.VALIDATE_FIELD_HEADER + ":'" + name + "'");
    }
  }

  /** The URL: a Thymeleaf expression ({@code @{/validate}}) evaluated, anything else as it is. */
  private static @Nullable Object resolve(ITemplateContext context, @Nullable String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim();
    boolean expression = trimmed.endsWith("}") && trimmed.length() > 2 && "$*#@~".indexOf(trimmed.charAt(0)) >= 0
        && trimmed.charAt(1) == '{';
    if (!expression) {
      return trimmed;
    }
    return StandardExpressions.getExpressionParser(context.getConfiguration())
        .parseExpression(context, trimmed)
        .execute(context);
  }

  private static String escape(String value) {
    return value.replace("\\", "\\\\").replace("'", "\\'");
  }

  private static void setUnlessPresent(IProcessableElementTag tag, IElementTagStructureHandler structureHandler,
      String name, String value) {
    if (!has(tag, name)) {
      structureHandler.setAttribute(name, value);
    }
  }

  /**
   * Whether the author wrote the htmx attribute {@code name} ({@code hx-target}) in any spelling: {@code hx-target},
   * {@code data-hx-target}, {@code th:hx-target}, or htmx-spring-boot's {@code hx:target}.
   */
  private static boolean has(IProcessableElementTag tag, String name) {
    return tag.hasAttribute(name)
        || tag.hasAttribute("data-" + name)
        || tag.hasAttribute("th:" + name)
        || tag.hasAttribute("hx:" + name.substring("hx-".length()));
  }
}
