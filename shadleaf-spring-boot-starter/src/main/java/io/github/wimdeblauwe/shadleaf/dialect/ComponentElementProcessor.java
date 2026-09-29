package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.AccessibleNameRule;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropCoercer;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.processor.element.IElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.processor.element.MatchingAttributeName;
import org.thymeleaf.processor.element.MatchingElementName;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Renders {@code <sl:NAME ...>} as the fragment {@code ~{sl/components/NAME :: NAME}}.
 * <p>
 * Before replacing the element it looks the component up in the {@link ComponentRegistry} and exposes three local
 * variables to the fragment:
 * <ul>
 *   <li>{@code props}: the declared props, bound from {@code th:<prop>} (evaluated here) or {@code <prop>} (a
 *       literal), coerced and defaulted. See {@link Props}.</li>
 *   <li>{@code attrs}: every attribute no prop consumed, for {@code sl:attrs} to pass through. A component without a
 *       {@code <sl:props>} block gets all its attributes here.</li>
 *   <li>{@code slots}: the content passed in, split into the default slot and the named slots. See {@link Slots}.</li>
 * </ul>
 * <p>
 * A prop has to be evaluated here rather than on the rendered element: its value decides what the fragment renders,
 * and by the time a forwarded attribute is evaluated those decisions are made.
 */
public class ComponentElementProcessor implements IElementModelProcessor {

  static final String PROPS_VARIABLE = "props";
  static final String ATTRS_VARIABLE = "attrs";
  static final String SLOTS_VARIABLE = "slots";

  /** {@code sl:*} elements that are part of the component machinery rather than components. */
  private static final Set<String> RESERVED_ELEMENTS = Set.of("slot", "props", "prop", "accessible-name");
  private static final String EXPRESSION_PREFIX = "th:";
  private static final List<String> ACCESSIBLE_NAME_ATTRIBUTES = List.of(
      "aria-label", "aria-labelledby", "th:aria-label", "th:aria-labelledby");

  private final String dialectPrefix;
  private final ComponentRegistry registry;
  private final int precedence;
  private final MatchingElementName matchingElementName;
  private final SlotContentSplitter slotContentSplitter;

  public ComponentElementProcessor(String dialectPrefix, ComponentRegistry registry) {
    this(dialectPrefix, registry, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  /** Allows tests to move the processor off the precedence it shares with {@code th:href} and friends. */
  ComponentElementProcessor(String dialectPrefix, ComponentRegistry registry, int precedence) {
    this.dialectPrefix = dialectPrefix;
    this.registry = registry;
    this.precedence = precedence;
    this.matchingElementName = MatchingElementName.forAllElementsWithPrefix(TemplateMode.HTML, dialectPrefix);
    this.slotContentSplitter = new SlotContentSplitter(dialectPrefix);
  }

  @Override
  public void process(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
    ITemplateEvent first = model.get(0);
    if (!(first instanceof IProcessableElementTag openTag)) {
      return;
    }
    try {
      doProcess(context, model, openTag, structureHandler);
    } catch (TemplateProcessingException e) {
      // What AbstractElementModelProcessor does, which this class cannot extend because it matches every sl:*
      // element: point the error at the component tag in the calling template.
      if (!e.hasTemplateName() && first.getTemplateName() != null) {
        e.setTemplateName(first.getTemplateName());
      }
      if (!e.hasLineAndCol() && first.getLine() != -1 && first.getCol() != -1) {
        e.setLineAndCol(first.getLine(), first.getCol());
      }
      throw e;
    }
  }

  private void doProcess(ITemplateContext context, IModel model, IProcessableElementTag openTag,
      IElementModelStructureHandler structureHandler) {

    String name = componentName(openTag);
    if (RESERVED_ELEMENTS.contains(name)) {
      return;
    }

    ComponentDefinition definition = registry.get(name);
    Map<String, @Nullable String> attributes = getAttributesAsMap(openTag);

    Map<String, @Nullable Object> values = new LinkedHashMap<>();
    for (PropDefinition prop : definition.props().values()) {
      values.put(prop.name(), bindProp(context, definition, prop, attributes));
    }
    checkAccessibleName(definition, values, attributes);
    // Read before this component sets its own: slot content gets these back (see SlotElementProcessor).
    Slots.CallerScope callerScope = new Slots.CallerScope(context.getVariable(PROPS_VARIABLE),
        context.getVariable(ATTRS_VARIABLE), context.getVariable(SLOTS_VARIABLE));
    // Set even when the component declares no props, so a component nested in another's slot never sees the outer
    // component's props.
    structureHandler.setLocalVariable(PROPS_VARIABLE, new Props(definition, values));
    structureHandler.setLocalVariable(ATTRS_VARIABLE, new Attrs(attributes));

    IModelFactory modelFactory = context.getModelFactory();
    SlotContentSplitter.SlotContent slotContent = slotContentSplitter.split(model, modelFactory);
    structureHandler.setLocalVariable(SLOTS_VARIABLE, new Slots(slotContent.defaultSlot(), slotContent.namedSlots(),
        callerScope));

    // Replace the element with a fragment call to the component template, e.g. <sl:button> ->
    // ~{sl/components/button :: button}.
    String fragmentExpression = "~{" + dialectPrefix + "/components/" + name + " :: " + name + "}";
    IOpenElementTag block = modelFactory.createOpenElementTag("th:block");
    block = modelFactory.setAttribute(block, "th:replace", fragmentExpression);

    model.reset();
    model.add(block);
    model.add(modelFactory.createCloseElementTag("th:block"));
  }

  /**
   * Binds one prop and removes the attributes it consumed from {@code attributes}, so they are not passed through.
   * {@code th:<prop>} wins over {@code <prop>}, as {@code th:href} does over {@code href}.
   */
  private static @Nullable Object bindProp(ITemplateContext context, ComponentDefinition definition,
      PropDefinition prop, Map<String, @Nullable String> attributes) {
    boolean hasExpression = attributes.containsKey(EXPRESSION_PREFIX + prop.name());
    boolean hasLiteral = attributes.containsKey(prop.name());
    String expression = attributes.remove(EXPRESSION_PREFIX + prop.name());
    String literal = attributes.remove(prop.name());

    Object raw;
    if (hasExpression) {
      raw = evaluate(context, definition, prop, expression);
    } else if (hasLiteral) {
      // A bare attribute (<sl:button disabled>) has no value.
      raw = literal == null ? "" : literal;
    } else {
      raw = null;
    }
    return PropCoercer.bind(definition.name(), prop, raw);
  }

  private static @Nullable Object evaluate(ITemplateContext context, ComponentDefinition definition,
      PropDefinition prop, @Nullable String expression) {
    if (expression == null || expression.isBlank()) {
      throw new ShadleafComponentException("th:%s on <sl:%s> has no expression."
          .formatted(prop.name(), definition.name()));
    }
    IStandardExpression parsed = StandardExpressions.getExpressionParser(context.getConfiguration())
        .parseExpression(context, expression);
    return parsed.execute(context);
  }

  private static void checkAccessibleName(ComponentDefinition definition, Map<String, @Nullable Object> values,
      Map<String, @Nullable String> attributes) {
    AccessibleNameRule rule = definition.accessibleNameRule();
    if (rule == null) {
      return;
    }
    Object propValue = rule.prop() == null ? null : values.get(rule.prop());
    if (!rule.appliesTo(propValue)) {
      return;
    }
    for (String attribute : ACCESSIBLE_NAME_ATTRIBUTES) {
      String value = attributes.get(attribute);
      if (value != null && !value.isBlank()) {
        return;
      }
    }
    String usage = rule.prop() == null
        ? "<sl:" + definition.name() + ">"
        : "<sl:%s %s=\"%s\">".formatted(definition.name(), rule.prop(), propValue);
    throw new ShadleafComponentException(
        "%s needs an accessible name: add aria-label=\"...\" or aria-labelledby=\"...\".".formatted(usage));
  }

  private static Map<String, @Nullable String> getAttributesAsMap(IProcessableElementTag openTag) {
    Map<String, @Nullable String> attrs = new LinkedHashMap<>();
    for (IAttribute attribute : openTag.getAllAttributes()) {
      attrs.put(attribute.getAttributeCompleteName(), attribute.getValue());
    }
    return attrs;
  }

  /** Derives the component name from the tag, e.g. {@code sl:button -> button}. */
  private String componentName(IProcessableElementTag openTag) {
    String complete = openTag.getElementCompleteName().toLowerCase(Locale.ROOT);
    String prefix = dialectPrefix + ":";
    return complete.startsWith(prefix) ? complete.substring(prefix.length()) : complete;
  }

  @Override
  public MatchingElementName getMatchingElementName() {
    return matchingElementName;
  }

  @Override
  public MatchingAttributeName getMatchingAttributeName() {
    return null;
  }

  @Override
  public TemplateMode getTemplateMode() {
    return TemplateMode.HTML;
  }

  @Override
  public int getPrecedence() {
    return precedence;
  }
}
