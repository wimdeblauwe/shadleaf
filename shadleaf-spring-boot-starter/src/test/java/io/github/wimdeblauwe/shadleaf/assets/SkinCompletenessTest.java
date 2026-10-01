package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.PropType;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every skin styles every value the component templates can render: for each enum prop that a component renders as a
 * {@code data-*} attribute, each compiled bundle has a rule selecting that value, and a component that can render a
 * focusable element (a link, a button, a form control or anything with a {@code tabindex}) has a
 * {@code :focus-visible} rule for it. A card or an alert takes no focus, so it needs none. A form control also needs a
 * {@code :disabled} rule, and one that takes {@code readonly} (a textarea, a text-like input) a {@code [readonly]}
 * rule. The focusable element can sit inside the root: the checkbox's {@code .checkbox} in its wrapper.
 * <p>
 * The expected selectors come from rendering the component, not from a list kept here: {@code variant="outline"}
 * renders {@code data-variant="outline"} and needs {@code .btn[data-variant="outline"]}, while the default renders no
 * attribute and needs {@code .btn:not([data-variant])}.
 * <p>
 * This checks that the rules exist, not that they draw anything. A rule that compiles but is invisible, such as an
 * outline width with {@code outline-style: none}, is only caught in a browser.
 */
class SkinCompletenessTest {

  private static final ComponentRenderTester RENDERER = ComponentRenderTester.create();
  private static final FormModel FORM = FormModel.of(Map.of(), Map.of());
  private static final ComponentRegistry REGISTRY = new ComponentRegistry(List.of(
      new ClasspathComponentDefinitionSource(SkinCompletenessTest.class.getClassLoader())));

  private static final Set<String> FOCUSABLE_ELEMENTS = Set.of("a", "button", "input", "select", "textarea",
      "summary");
  private static final Set<String> FORM_CONTROLS = Set.of("input", "select", "textarea");
  // Input types that ignore readonly.
  /** The page an object prop ({@code th:page}) gets: page 2 of 3, so every pagination link is there. */
  private static final Page<String> SAMPLE_PAGE = new PageImpl<>(List.of("x"), PageRequest.of(1, 1), 3);

  private static final Set<String> WITHOUT_READONLY = Set.of("checkbox", "radio", "file", "range", "color", "hidden");

  // A rule's selector list: the text before a "{" back to the previous "{", "}" or ";", skipping at-rules. A selector
  // list may span lines; it never contains those three characters.
  private static final Pattern SELECTOR_LIST = Pattern.compile("(?<=^|[{};])\\s*([^@\\s{};][^{};]*?)\\s*\\{");

  static List<Arguments> requiredSelectors() throws IOException {
    List<Arguments> arguments = new ArrayList<>();
    for (String bundle : bundles()) {
      for (String name : libraryComponents()) {
        ComponentDefinition definition = REGISTRY.get(name);
        String rootClass = null;
        // Focusable elements by their selector (the first class), from the plain render and every enum value.
        Map<String, Element> focusable = new LinkedHashMap<>();
        Element plain = renderRoot(name, null, null);
        if (plain == null) {
          // A root that renders no element of its own (sl:dropdown-menu): nothing to style.
          continue;
        }
        addFocusable(plain, focusable);
        for (PropDefinition prop : definition.props().values()) {
          if (prop.type() != PropType.ENUM) {
            continue;
          }
          String attribute = "data-" + prop.name();
          List<String> selectors = new ArrayList<>();
          boolean rendersAttribute = false;
          for (String value : prop.values()) {
            Element root = renderRoot(name, prop.name(), value);
            rootClass = "." + root.classNames().iterator().next();
            addFocusable(root, focusable);
            if (root.hasAttr(attribute)) {
              rendersAttribute = true;
              selectors.add("%s[%s=\"%s\"]".formatted(rootClass, attribute, root.attr(attribute)));
            } else {
              selectors.add("%s:not([%s])".formatted(rootClass, attribute));
            }
          }
          if (rendersAttribute) {
            selectors.forEach(selector -> arguments.add(Arguments.of(bundle, selector)));
          }
        }
        focusable.forEach((selector, element) -> {
          arguments.add(Arguments.of(bundle, selector + ":focus-visible"));
          if (FORM_CONTROLS.contains(element.tagName())) {
            arguments.add(Arguments.of(bundle, selector + ":disabled"));
          }
          if (takesReadonly(element)) {
            arguments.add(Arguments.of(bundle, selector + "[readonly]"));
          }
        });
      }
    }
    return arguments;
  }

  @ParameterizedTest(name = "{0} has a rule for {1}")
  @MethodSource("requiredSelectors")
  void everySkinStylesEveryRenderedValue(String bundle, String selector) throws IOException {
    Set<String> selectorLists = selectorLists(bundle(bundle));

    assertThat(selectorLists.stream().anyMatch(selectorList -> selectorList.contains(selector)))
        .as("%s has a rule whose selector contains %s", bundle, selector)
        .isTrue();
  }

  /** The library's own components; the {@code test-*} ones only exist on the test classpath. */
  private static List<String> libraryComponents() {
    return REGISTRY.names().stream()
        .filter(name -> !name.startsWith("test-"))
        .filter(name -> REGISTRY.get(name).declared())
        .toList();
  }

  private static void addFocusable(Element root, Map<String, Element> focusable) {
    for (Element element : root.getAllElements()) {
      if (isFocusable(element) && !element.classNames().isEmpty()) {
        focusable.putIfAbsent("." + element.classNames().iterator().next(), element);
      }
    }
  }

  private static boolean isFocusable(Element element) {
    if (element.tagName().equals("input") && element.attr("type").equals("hidden")) {
      return false;
    }
    return FOCUSABLE_ELEMENTS.contains(element.tagName()) || element.hasAttr("tabindex");
  }

  private static boolean takesReadonly(Element element) {
    return element.tagName().equals("textarea")
        || element.tagName().equals("input") && !WITHOUT_READONLY.contains(element.attr("type"));
  }

  /** The rendered root element, or {@code null} for a component that renders none of its own. */
  private static @Nullable Element renderRoot(String component, @Nullable String prop, @Nullable String value) {
    // aria-label satisfies any accessible-name rule; name satisfies <sl:icon>; every other required prop gets "x" (a
    // number "1"); an object prop gets the page of SAMPLE_PAGE (the only object props are th:page).
    // Inside a form object, for the components that read one (sl:form-errors).
    StringBuilder propAttribute = new StringBuilder(prop == null ? "" : "%s=\"%s\" ".formatted(prop, value));
    REGISTRY.get(component).props().values().stream()
        .filter(PropDefinition::required)
        .filter(required -> !required.name().equals(prop) && !required.name().equals("name"))
        .forEach(required -> propAttribute.append("%s=\"%s\" ".formatted(required.name(),
            required.type() == PropType.NUMBER ? "1" : "x")));
    REGISTRY.get(component).props().values().stream()
        .filter(object -> object.type() == PropType.OBJECT && !object.name().equals(prop))
        .forEach(object -> propAttribute.append("th:%s=\"${samplePage}\" ".formatted(object.name())));
    Map<String, Object> variables = new LinkedHashMap<>(FORM.variables());
    variables.put("samplePage", SAMPLE_PAGE);
    Elements rendered = RENDERER.render(FormModel.wrap("<sl:%s %saria-label=\"x\" name=\"x\">x</sl:%s>"
        .formatted(component, propAttribute, component)), variables).document().body().children();
    return rendered.isEmpty() ? null : rendered.first();
  }

  private static Set<String> selectorLists(String css) {
    String withoutComments = css.replaceAll("(?s)/\\*.*?\\*/", "");
    Matcher matcher = SELECTOR_LIST.matcher(withoutComments);
    Set<String> selectorLists = new HashSet<>();
    while (matcher.find()) {
      selectorLists.add(matcher.group(1).replaceAll("\\s+", " "));
    }
    return selectorLists;
  }

  private static List<String> bundles() throws IOException {
    return manifest().entries().keySet().stream()
        .filter(key -> key.startsWith("css/entries/"))
        .sorted()
        .collect(Collectors.toList());
  }

  private static String bundle(String entry) throws IOException {
    String file = manifest().getEntry(entry).file();
    try (InputStream inputStream = new ClassPathResource("META-INF/resources/shadleaf/" + file).getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static ViteManifest manifest() throws IOException {
    return new ViteManifestParser(JsonMapper.builder().build())
        .parse(new ClassPathResource(ShadleafAssets.MANIFEST_LOCATION));
  }
}
