package io.github.wimdeblauwe.shadleaf.component;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.attoparser.ParseException;
import org.attoparser.config.ParseConfiguration;
import org.attoparser.simple.AbstractSimpleMarkupHandler;
import org.attoparser.simple.SimpleMarkupParser;
import org.jspecify.annotations.Nullable;

/**
 * Reads the {@code <sl:props>} block of a component template into a {@link ComponentDefinition}.
 *
 * <pre>{@code
 * <sl:props>
 *   <sl:prop name="variant" default="primary" values="primary outline">Visual style.</sl:prop>
 *   <sl:prop name="disabled" type="boolean">Disable the button.</sl:prop>
 *   <sl:accessible-name required-when="size=icon"/>
 * </sl:props>
 * }</pre>
 * <p>
 * Uses attoparser, the parser Thymeleaf itself is built on, so reading the schema adds no dependency. Every mistake in
 * the block is an error: a component's schema is its public API, and a silently ignored typo in it is a bug that
 * surfaces far away.
 */
public final class PropsParser {

  private static final String PROPS_ELEMENT = "sl:props";
  private static final String PROP_ELEMENT = "sl:prop";
  private static final String ACCESSIBLE_NAME_ELEMENT = "sl:accessible-name";
  private static final Set<String> PROP_ATTRIBUTES = Set.of("name", "type", "default", "values");
  private static final Pattern PROP_NAME = Pattern.compile("[a-z][a-z0-9]*(-[a-z0-9]+)*");
  private static final Pattern WHITESPACE = Pattern.compile("\\s+");
  private static final ParseConfiguration PARSE_CONFIGURATION = parseConfiguration();

  private PropsParser() {
  }

  /**
   * @param componentName the component name, e.g. {@code button}
   * @param template      the full component template
   * @param source        where the template was read from, for error messages
   */
  public static ComponentDefinition parse(String componentName, Reader template, String source) {
    Handler handler = new Handler(componentName, source);
    try {
      new SimpleMarkupParser(PARSE_CONFIGURATION).parse(template, handler);
    } catch (ParseException e) {
      if (e.getCause() instanceof ShadleafComponentException componentException) {
        throw componentException;
      }
      throw new ShadleafComponentException("Could not parse component template " + source + ": " + e.getMessage(), e);
    }
    return handler.result();
  }

  public static ComponentDefinition parse(String componentName, String template, String source) {
    return parse(componentName, new StringReader(template), source);
  }

  private static ParseConfiguration parseConfiguration() {
    ParseConfiguration configuration = ParseConfiguration.htmlConfiguration();
    // Thymeleaf parses with AUTO_CLOSE, which is harmless for rendering because <sl:props> never renders. Here it
    // would be fatal: markup such as <code> in a prop description makes the parser close <head>, and with it the
    // <sl:props> block, halfway through. Without balancing, the events arrive exactly as written.
    configuration.setElementBalancing(ParseConfiguration.ElementBalancing.NO_BALANCING);
    configuration.setCaseSensitive(false);
    configuration.setNoUnmatchedCloseElementsRequired(false);
    configuration.setUniqueRootElementPresence(ParseConfiguration.UniqueRootElementPresence.NOT_VALIDATED);
    return configuration;
  }

  /** Reads the template from {@code reader} and closes it. */
  static ComponentDefinition parseAndClose(String componentName, Reader reader, String source) throws IOException {
    try (reader) {
      return parse(componentName, reader, source);
    }
  }

  private static final class Handler extends AbstractSimpleMarkupHandler {

    private final String componentName;
    private final String source;

    private boolean seenProps;
    private boolean inProps;
    private @Nullable Map<String, String> currentProp;
    private final StringBuilder currentDescription = new StringBuilder();
    private final List<PropDefinition> props = new ArrayList<>();
    private @Nullable Map<String, String> accessibleName;

    private Handler(String componentName, String source) {
      this.componentName = componentName;
      this.source = source;
    }

    ComponentDefinition result() {
      if (!seenProps) {
        return ComponentDefinition.undeclared(componentName, source);
      }
      AccessibleNameRule rule = accessibleName == null ? null : accessibleNameRule(accessibleName);
      return ComponentDefinition.declared(componentName, props, rule, source);
    }

    @Override
    public void handleOpenElement(String elementName, Map<String, String> attributes, int line, int col) {
      openElement(elementName, attributes, false);
    }

    @Override
    public void handleStandaloneElement(String elementName, Map<String, String> attributes, boolean minimized,
        int line, int col) {
      openElement(elementName, attributes, true);
    }

    @Override
    public void handleCloseElement(String elementName, int line, int col) {
      String name = normalize(elementName);
      if (name.equals(PROPS_ELEMENT)) {
        inProps = false;
      } else if (name.equals(PROP_ELEMENT) && currentProp != null) {
        finishProp();
      }
    }

    @Override
    public void handleText(char[] buffer, int offset, int len, int line, int col) {
      if (currentProp != null) {
        currentDescription.append(buffer, offset, len);
      }
    }

    private void openElement(String elementName, @Nullable Map<String, String> attributes, boolean standalone) {
      String name = normalize(elementName);
      Map<String, String> attrs = attributes == null ? Map.of() : attributes;
      if (name.equals(PROPS_ELEMENT)) {
        if (seenProps) {
          throw error("more than one <sl:props> block");
        }
        seenProps = true;
        inProps = !standalone;
        return;
      }
      if (!inProps) {
        return;
      }
      if (currentProp != null) {
        // Markup inside a prop's description, e.g. <code>: only its text is kept.
        return;
      }
      switch (name) {
        case PROP_ELEMENT -> {
          currentProp = attrs;
          currentDescription.setLength(0);
          if (standalone) {
            finishProp();
          }
        }
        case ACCESSIBLE_NAME_ELEMENT -> {
          if (accessibleName != null) {
            throw error("more than one <sl:accessible-name>");
          }
          accessibleName = attrs;
        }
        default -> throw error("unexpected <" + elementName + ">; only <sl:prop> and <sl:accessible-name> are allowed");
      }
    }

    private void finishProp() {
      Map<String, String> attrs = currentProp;
      currentProp = null;
      String description = WHITESPACE.matcher(currentDescription).replaceAll(" ").trim();
      props.add(propDefinition(attrs, description));
    }

    private PropDefinition propDefinition(Map<String, String> attrs, String description) {
      String name = attrs.get("name");
      if (name == null || name.isBlank()) {
        throw error("<sl:prop> without a name");
      }
      if (!PROP_NAME.matcher(name).matches()) {
        throw error("prop name '" + name + "' must be lower-case kebab-case, e.g. 'stroke-width'");
      }
      if (ReservedPropNames.isReserved(name)) {
        throw error("prop name '" + name + "' is reserved: th:" + name
            + " is a standard Thymeleaf attribute, so th:" + name + " could not set the prop");
      }
      if (props.stream().anyMatch(prop -> prop.name().equals(name))) {
        throw error("prop '" + name + "' is declared twice");
      }
      for (String attribute : attrs.keySet()) {
        if (!PROP_ATTRIBUTES.contains(attribute)) {
          throw error("unknown attribute '" + attribute + "' on prop '" + name + "'; allowed are "
              + String.join(", ", PROP_ATTRIBUTES.stream().sorted().toList()));
        }
      }

      List<String> values = parseValues(name, attrs.get("values"));
      PropType type = propType(name, attrs.get("type"), values);

      Object defaultValue = null;
      String defaultAttribute = attrs.get("default");
      if (defaultAttribute != null) {
        try {
          defaultValue = PropCoercer.coerce(componentName, name, type, values, defaultAttribute);
        } catch (ShadleafComponentException e) {
          throw error("invalid default for prop '" + name + "': " + e.getMessage());
        }
      } else if (type == PropType.BOOLEAN) {
        defaultValue = Boolean.FALSE;
      }
      return new PropDefinition(name, type, defaultValue, values, description);
    }

    private List<String> parseValues(String propName, @Nullable String valuesAttribute) {
      if (valuesAttribute == null) {
        return List.of();
      }
      List<String> values = Arrays.stream(WHITESPACE.split(valuesAttribute.trim()))
          .filter(value -> !value.isEmpty())
          .toList();
      if (values.isEmpty()) {
        throw error("prop '" + propName + "' has an empty values attribute");
      }
      if (new LinkedHashSet<>(values).size() != values.size()) {
        throw error("prop '" + propName + "' lists a value twice in '" + valuesAttribute + "'");
      }
      return values;
    }

    private PropType propType(String propName, @Nullable String typeAttribute, List<String> values) {
      if (typeAttribute == null) {
        return values.isEmpty() ? PropType.STRING : PropType.ENUM;
      }
      PropType type = switch (typeAttribute) {
        case "string" -> values.isEmpty() ? PropType.STRING : PropType.ENUM;
        case "enum" -> PropType.ENUM;
        case "boolean" -> PropType.BOOLEAN;
        case "number" -> PropType.NUMBER;
        default -> throw error("prop '" + propName + "' has unknown type '" + typeAttribute
            + "'; use string, boolean or number, or list the legal values with values=\"...\"");
      };
      if (type == PropType.ENUM && values.isEmpty()) {
        throw error("prop '" + propName + "' is an enum but declares no values=\"...\"");
      }
      if ((type == PropType.BOOLEAN || type == PropType.NUMBER) && !values.isEmpty()) {
        throw error("prop '" + propName + "' is of type " + typeAttribute + " and cannot declare values");
      }
      return type;
    }

    private AccessibleNameRule accessibleNameRule(Map<String, String> attrs) {
      for (String attribute : attrs.keySet()) {
        if (!attribute.equals("required-when")) {
          throw error("unknown attribute '" + attribute + "' on <sl:accessible-name>; allowed is required-when");
        }
      }
      String requiredWhen = attrs.get("required-when");
      if (requiredWhen == null) {
        return AccessibleNameRule.always();
      }
      int equals = requiredWhen.indexOf('=');
      if (equals < 0) {
        throw error("required-when=\"" + requiredWhen + "\" must look like required-when=\"size=icon icon-sm\"");
      }
      String propName = requiredWhen.substring(0, equals).trim();
      List<String> values = Arrays.stream(WHITESPACE.split(requiredWhen.substring(equals + 1).trim()))
          .filter(value -> !value.isEmpty())
          .toList();
      PropDefinition prop = props.stream().filter(p -> p.name().equals(propName)).findFirst()
          .orElseThrow(() -> error("<sl:accessible-name> refers to undeclared prop '" + propName + "'"));
      if (values.isEmpty()) {
        throw error("<sl:accessible-name required-when=\"" + requiredWhen + "\"> lists no values");
      }
      if (prop.type() == PropType.ENUM) {
        for (String value : values) {
          if (!prop.values().contains(value)) {
            throw error("<sl:accessible-name> refers to value '" + value + "', which is not a legal value of prop '"
                + propName + "' (" + String.join(", ", prop.values()) + ")");
          }
        }
      }
      return new AccessibleNameRule(propName, Set.copyOf(values));
    }

    private ShadleafComponentException error(String message) {
      String sentence = message.endsWith(".") ? message : message + ".";
      return new ShadleafComponentException("Invalid <sl:props> in " + source + ": " + sentence);
    }

    private static String normalize(String elementName) {
      return elementName.toLowerCase(Locale.ROOT);
    }
  }
}
