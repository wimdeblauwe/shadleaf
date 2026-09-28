package io.github.wimdeblauwe.shadleaf.assets;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.component.PropDefinition;
import io.github.wimdeblauwe.shadleaf.component.PropType;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.nodes.Element;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Every skin styles every value the component templates can render: for each enum prop that a component renders as a
 * {@code data-*} attribute, each compiled bundle has a rule selecting that value, and the component has a
 * {@code :focus-visible} rule.
 * <p>
 * The expected selectors come from rendering the component, not from a list kept here: {@code variant="outline"}
 * renders {@code data-variant="outline"} and needs {@code .btn[data-variant="outline"]}, while the default renders no
 * attribute and needs {@code .btn:not([data-variant])}.
 * <p>
 * This checks that the rules exist, not that they draw anything. A rule that compiles but is invisible, like M2's
 * flat focus ring, is only caught in a browser.
 */
class SkinCompletenessTest {

  private static final ComponentRenderTester RENDERER = ComponentRenderTester.create();
  private static final ComponentRegistry REGISTRY = new ComponentRegistry(List.of(
      new ClasspathComponentDefinitionSource(SkinCompletenessTest.class.getClassLoader())));

  // A rule's selector list: the text before a "{" back to the previous "{", "}" or ";", skipping at-rules. A selector
  // list may span lines; it never contains those three characters.
  private static final Pattern SELECTOR_LIST = Pattern.compile("(?<=^|[{};])\\s*([^@\\s{};][^{};]*?)\\s*\\{");

  static List<Arguments> requiredSelectors() throws IOException {
    List<Arguments> arguments = new ArrayList<>();
    for (String bundle : bundles()) {
      for (String name : libraryComponents()) {
        ComponentDefinition definition = REGISTRY.get(name);
        String rootClass = null;
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
        if (rootClass != null) {
          arguments.add(Arguments.of(bundle, rootClass + ":focus-visible"));
        }
      }
    }
    return arguments;
  }

  @ParameterizedTest(name = "{0} has a rule for {1}")
  @MethodSource("requiredSelectors")
  void everySkinStylesEveryRenderedValue(String bundle, String selector) throws IOException {
    Set<String> selectorLists = selectorLists(bundle(bundle));

    assertThat(selectorLists)
        .as("rules in %s", bundle)
        .anyMatch(selectorList -> selectorList.contains(selector));
  }

  /** The library's own components; the {@code test-*} ones only exist on the test classpath. */
  private static List<String> libraryComponents() {
    return REGISTRY.names().stream()
        .filter(name -> !name.startsWith("test-"))
        .filter(name -> REGISTRY.get(name).declared())
        .toList();
  }

  private static Element renderRoot(String component, String prop, String value) {
    // aria-label satisfies any accessible-name rule; name satisfies <sl:icon>.
    return RENDERER.render("<sl:%s %s=\"%s\" aria-label=\"x\" name=\"x\">x</sl:%s>"
        .formatted(component, prop, value, component)).root();
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
