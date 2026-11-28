package io.github.wimdeblauwe.shadleaf.docs;

import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.PropType;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.IsolatedRender;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * The styling hooks of the library's components: the classes, and the {@code data-*} and {@code aria-*} attributes
 * with their values, that an application's CSS can select to restyle a component. They are a public API, as the props
 * are: the docs list them under each component's attributes, and {@code styling-hooks.approved.json} turns a rename
 * or a removal into a failing test.
 * <p>
 * Nothing here is listed by hand. A hook is what both holds:
 * <ul>
 *   <li>the library renders it: every component alone, plain, with each value of each enum prop, each boolean prop
 *   on and its named slots filled, and every preview scenario of the docs;</li>
 *   <li>the library's own compiled CSS selects it, in any skin. An attribute that only configures a script
 *   ({@code data-search-url}) is no hook.</li>
 * </ul>
 * A component's root class is the class named after it ({@code .button}), or else the first it renders that starts
 * with its name ({@code .select-wrapper}); a component that only arranges others ({@code sl:alert-dialog-cancel}
 * renders a button) has none. A class belongs to the component whose root class is its longest prefix:
 * {@code .sidebar-menu-button-label} to {@code sl:sidebar-menu-button}, not to {@code sl:sidebar}. An attribute belongs to the first such class on its
 * element. A {@code data-*} attribute named after an enum prop is absent for the prop's default value.
 */
final class StylingHooks {

  // A rule's selector list, as in SkinCompletenessTest: the text before a "{" back to the previous "{", "}" or ";".
  private static final Pattern SELECTOR_LIST = Pattern.compile("(?<=^|[{};])\\s*([^@\\s{};][^{};]*?)\\s*\\{");
  private static final Pattern CLASS = Pattern.compile("\\.(-?[_a-zA-Z][\\w-]*)");
  private static final Pattern ATTRIBUTE = Pattern.compile("\\[\\s*((?:data|aria)-[\\w-]+)");
  private static final Pattern NAMED_SLOT = Pattern.compile("<sl:slot\\s+name=\"([^\"]+)\"");
  // A class and what follows it on the same element: attributes, pseudo-classes, more classes.
  private static final Pattern COMPOUND = Pattern.compile(
      "\\.(-?[_a-zA-Z][\\w-]*)((?:\\[[^\\]]*]|:{1,2}[\\w-]+|\\.-?[_a-zA-Z][\\w-]*)*)");
  private static final Pattern ATTRIBUTE_SELECTOR = Pattern.compile(
      "\\[\\s*((?:data|aria)-[\\w-]+)\\s*(?:([~|^$*]?)=\\s*[\"']?([^\"'\\]]*)[\"']?\\s*)?(?:\\s[is])?]");
  private static final Pattern PARENTHESES = Pattern.compile("\\([^()]*\\)");
  // A value worth listing: a keyword, not text such as a placeholder.
  private static final Pattern KEYWORD = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

  /**
   * @param name    the component, e.g. {@code button}
   * @param classes its classes, the root class first
   */
  record ComponentHooks(String name, List<ClassHook> classes) {

  }

  /**
   * @param name       the class, without the dot
   * @param elements   the elements it is rendered on
   * @param attributes the attributes selected on it
   */
  record ClassHook(String name, List<String> elements, List<AttributeHook> attributes) {

  }

  /**
   * @param name   the attribute, e.g. {@code data-variant}
   * @param prop   the enum or boolean prop it reflects, or {@code null} for a state the component sets itself
   * @param values the values it is rendered or selected with; empty when only its presence matters
   */
  record AttributeHook(String name, @Nullable String prop, List<String> values) {

  }

  private StylingHooks() {
  }

  /**
   * @param templates   the directory holding the components' templates, for their named slots
   * @param stylesheets the compiled CSS of every bundle
   * @param examples    rendered pages that use the components, such as the docs' previews
   */
  static List<ComponentHooks> collect(ComponentRegistry registry, Path templates, Collection<String> stylesheets,
      Collection<Document> examples) {
    Set<String> styledClasses = new HashSet<>();
    Set<String> styledAttributes = new HashSet<>();
    // The attributes selected on a class itself, by class: .sidebar-provider[data-state="collapsed"], whether or not
    // the server renders that value (a script may set it).
    Map<String, Map<String, Set<String>>> selectedOnClass = new HashMap<>();
    for (String css : stylesheets) {
      Matcher selectorList = SELECTOR_LIST.matcher(css.replaceAll("(?s)/\\*.*?\\*/", ""));
      while (selectorList.find()) {
        String selector = selectorList.group(1);
        CLASS.matcher(selector).results().forEach(match -> styledClasses.add(match.group(1)));
        ATTRIBUTE.matcher(selector).results().forEach(match -> styledAttributes.add(match.group(1)));
        // Without :not(...), :has(...) and :where(...): what is inside them is about another element, or negated.
        String outer = selector;
        for (String previous = null; !outer.equals(previous); ) {
          previous = outer;
          outer = PARENTHESES.matcher(outer).replaceAll("");
        }
        Matcher compound = COMPOUND.matcher(outer);
        while (compound.find()) {
          Matcher attribute = ATTRIBUTE_SELECTOR.matcher(compound.group(2));
          while (attribute.find()) {
            Set<String> values = selectedOnClass.computeIfAbsent(compound.group(1), name -> new HashMap<>())
                .computeIfAbsent(attribute.group(1), name -> new TreeSet<>());
            // Only an exact value: [data-position^="top"] names no value of its own.
            if (attribute.group(3) != null && attribute.group(2).isEmpty()) {
              values.add(attribute.group(3));
            }
          }
        }
      }
    }

    IsolatedRender renderer = new IsolatedRender(registry, ComponentRenderTester.create());
    List<String> components = registry.names().stream()
        .filter(name -> !name.startsWith("test-"))
        .filter(name -> registry.get(name).declared())
        .sorted()
        .toList();
    Map<String, String> componentByRootClass = new LinkedHashMap<>();
    Map<String, String> prefixedRootClasses = new LinkedHashMap<>();
    List<Element> elements = new ArrayList<>();
    for (String component : components) {
      List<Element> rendered = renderVariations(renderer, registry.get(component), namedSlots(templates, component));
      List<Element> all = rendered.stream().flatMap(root -> root.getAllElements().stream()).toList();
      if (all.stream().anyMatch(element -> element.hasClass(component))) {
        componentByRootClass.put(component, component);
      } else {
        all.stream().flatMap(element -> element.classNames().stream())
            .filter(className -> className.startsWith(component + "-"))
            .findFirst()
            .ifPresent(className -> prefixedRootClasses.put(component, className));
      }
      elements.addAll(all);
    }
    // .select-wrapper is sl:select's root class, unless a component is called select-wrapper.
    prefixedRootClasses.forEach((component, className) -> {
      if (!components.contains(className)) {
        componentByRootClass.putIfAbsent(className, component);
      }
    });
    examples.forEach(example -> elements.addAll(example.getAllElements()));

    // The longest root class first, so .sidebar-menu-button wins over .sidebar.
    List<String> rootClasses = componentByRootClass.keySet().stream()
        .sorted(Comparator.comparingInt(String::length).reversed())
        .toList();
    Map<String, Map<String, Collected>> byComponent = new TreeMap<>();
    for (Element element : elements) {
      Collected owner = null;
      for (String className : element.classNames()) {
        if (!styledClasses.contains(className)) {
          continue;
        }
        String root = rootClasses.stream()
            .filter(candidate -> className.equals(candidate) || className.startsWith(candidate + "-"))
            .findFirst()
            .orElse(null);
        if (root == null) {
          continue;
        }
        Collected collected = byComponent.computeIfAbsent(componentByRootClass.get(root), name -> new TreeMap<>())
            .computeIfAbsent(className, name -> new Collected());
        collected.elements.add(element.tagName());
        if (owner == null) {
          owner = collected;
        }
      }
      if (owner == null) {
        continue;
      }
      for (Attribute attribute : element.attributes()) {
        if (styledAttributes.contains(attribute.getKey())) {
          owner.attributes.computeIfAbsent(attribute.getKey(), name -> new TreeSet<>()).add(attribute.getValue());
          owner.rendered.computeIfAbsent(attribute.getKey(), name -> new TreeSet<>()).add(attribute.getValue());
        }
      }
    }
    byComponent.values().forEach(classes -> classes.forEach((className, collected) ->
        selectedOnClass.getOrDefault(className, Map.of()).forEach((attribute, values) ->
            collected.attributes.computeIfAbsent(attribute, name -> new TreeSet<>()).addAll(values))));

    List<ComponentHooks> hooks = new ArrayList<>();
    for (String component : components) {
      ComponentDefinition definition = registry.get(component);
      Map<String, Collected> classes = byComponent.getOrDefault(component, Map.of());
      String rootClass = componentByRootClass.entrySet().stream()
          .filter(entry -> entry.getValue().equals(component))
          .map(Map.Entry::getKey)
          .findFirst()
          .orElse(null);
      List<ClassHook> classHooks = new ArrayList<>();
      classes.entrySet().stream()
          .sorted(Comparator.comparing((Map.Entry<String, Collected> entry) -> !entry.getKey().equals(rootClass))
              .thenComparing(Map.Entry::getKey))
          .forEach(entry -> classHooks.add(new ClassHook(entry.getKey(), List.copyOf(entry.getValue().elements),
              attributeHooks(definition, entry.getValue()))));
      hooks.add(new ComponentHooks(component, classHooks));
    }
    return hooks;
  }

  private static List<AttributeHook> attributeHooks(ComponentDefinition definition, Collected collected) {
    List<AttributeHook> hooks = new ArrayList<>();
    collected.attributes.forEach((name, values) -> {
      PropDefinition prop = name.startsWith("data-") ? definition.prop(name.substring("data-".length())) : null;
      if (prop != null && prop.type() != PropType.ENUM && prop.type() != PropType.BOOLEAN) {
        prop = null;
      }
      // Text such as a placeholder is no value to select on: the attribute is listed as present, with any value.
      List<String> ordered = new ArrayList<>(values.stream().filter(value -> KEYWORD.matcher(value).matches())
          .toList());
      if (prop != null && prop.type() == PropType.ENUM) {
        // In the order the template declares them, without the default unless it is rendered: the attribute is
        // absent for it (props.unlessDefault), even where a skin also selects it.
        List<String> declared = prop.values();
        String defaultValue = String.valueOf(prop.defaultValue());
        if (!collected.rendered.getOrDefault(name, Set.of()).contains(defaultValue)) {
          ordered.remove(defaultValue);
        }
        ordered.sort(Comparator.comparingInt(value -> declared.contains(value) ? declared.indexOf(value)
            : declared.size()));
      }
      hooks.add(new AttributeHook(name, prop == null ? null : prop.name(), ordered));
    });
    return hooks;
  }

  /**
   * The component plain, empty and with its named slots filled, with each value of each enum prop and with each boolean
   * prop on. The plain render and the enum values must render (SkinCompletenessTest renders them too); a boolean or a
   * filled slot may need more than an isolated render gives, so those that fail are left out.
   */
  private static List<Element> renderVariations(IsolatedRender renderer, ComponentDefinition definition,
      List<String> slots) {
    String filledSlots = slots.stream().map("<sl:slot name=\"%s\">x</sl:slot>"::formatted)
        .reduce("", String::concat) + "x";
    List<Element> rendered = new ArrayList<>(renderer.render(definition.name(), Map.of(), "x"));
    rendered.addAll(tryRender(renderer, definition.name(), Map.of(), filledSlots));
    rendered.addAll(tryRender(renderer, definition.name(), Map.of(), ""));
    for (PropDefinition prop : definition.props().values()) {
      if (prop.type() == PropType.ENUM) {
        for (String value : prop.values()) {
          rendered.addAll(renderer.render(definition.name(), Map.of(prop.name(), value), "x"));
          rendered.addAll(tryRender(renderer, definition.name(), Map.of(prop.name(), value), filledSlots));
        }
      } else if (prop.type() == PropType.BOOLEAN) {
        rendered.addAll(tryRender(renderer, definition.name(), Map.of(prop.name(), "true"), filledSlots));
      }
    }
    return rendered;
  }

  private static List<Element> tryRender(IsolatedRender renderer, String component, Map<String, String> props,
      String content) {
    try {
      return renderer.render(component, props, content);
    } catch (RuntimeException e) {
      return List.of();
    }
  }

  private static List<String> namedSlots(Path templates, String component) {
    try {
      String template = Files.readString(templates.resolve(component + ".html"));
      return NAMED_SLOT.matcher(template).results().map(match -> match.group(1)).distinct().toList();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static final class Collected {

    private final Set<String> elements = new TreeSet<>();
    private final Map<String, TreeSet<String>> attributes = new TreeMap<>();
    // The values seen in rendered markup, as opposed to only in the CSS's selectors.
    private final Map<String, Set<String>> rendered = new HashMap<>();
  }
}
