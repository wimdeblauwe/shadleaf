package io.github.wimdeblauwe.shadleaf.docs;

import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.assertThat;

import io.github.wimdeblauwe.shadleaf.assets.AlpineVariant;
import io.github.wimdeblauwe.shadleaf.assets.AssetVariant;
import io.github.wimdeblauwe.shadleaf.assets.ShadleafAssets;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser;
import io.github.wimdeblauwe.shadleaf.assets.ViteManifestParser.ViteManifest;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.metadata.ComponentMetadata;
import io.github.wimdeblauwe.shadleaf.metadata.WebTypes;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.FileSystemUtils;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates everything the docs site ({@code docs/}) derives from the library, into {@value #OUTPUT}:
 * <ul>
 *   <li>{@code previews.json}: every scenario in {@code src/test/resources/previews/*.yaml}, rendered through
 *   {@link ComponentRenderTester}, the same harness the component tests use, so a preview cannot show markup the
 *   library does not emit;</li>
 *   <li>{@code shadleaf/assets/*.css}: the standalone bundle of every skin, found through the Vite manifest;</li>
 *   <li>{@code shadleaf/assets/*.js}: the script of every {@link AlpineVariant}, with the chunks it imports, for the
 *   showcase and the behaviour tests;</li>
 *   <li>{@code components.json} and {@code web-types.json}: the registry export behind the attribute tables, and the
 *   IDE metadata offered for download;</li>
 *   <li>{@code theme-script.json}: the theme script and its CSP hash, for the CSP page.</li>
 * </ul>
 * {@code docs/scripts/sync.mjs} copies them into the docs project. Adding examples for a component means adding a
 * YAML file; every library component must have one, or be listed under the {@code parts} of its family's file.
 */
class PreviewGeneratorTest {

  static final String OUTPUT = "target/generated-docs";

  private static final Path OUTPUT_DIRECTORY = Path.of(OUTPUT);
  private static final Path PREVIEWS_DIRECTORY = Path.of("src", "test", "resources", "previews");
  private static final Path BUILT_ASSETS = Path.of("target", "classes", "META-INF", "resources", "shadleaf");
  private static final Path MANIFEST = Path.of("target", "classes").resolve(ShadleafAssets.MANIFEST_LOCATION);
  private static final Pattern STANDALONE_ENTRY = Pattern.compile("css/entries/shadleaf-([^.]+)\\.css");
  private static final String DEFAULT_SKIN = "vega";

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void generatesDocsArtifacts() throws IOException {
    ComponentRegistry registry = LibraryComponents.registry();
    List<Scenario> scenarios = loadScenarios(registry);

    FileSystemUtils.deleteRecursively(OUTPUT_DIRECTORY);
    Files.createDirectories(OUTPUT_DIRECTORY);

    ViteManifest manifest = new ViteManifestParser(jsonMapper).parse(new FileSystemResource(MANIFEST));
    List<Skin> skins = copySkins(manifest);
    Map<String, String> scripts = copyScripts(manifest);
    List<Preview> previews = scenarios.stream().map(this::render).toList();
    String version = System.getProperty("shadleaf.version", ComponentMetadata.libraryVersion());

    write("previews.json", new Previews(version, skins, scripts, previews));
    ComponentMetadata metadata = ComponentMetadata.of(registry, version);
    metadata.write(jsonMapper, OUTPUT_DIRECTORY.resolve("components.json"));
    WebTypes.write(metadata, jsonMapper, OUTPUT_DIRECTORY.resolve("web-types.json"));
    ShadleafThemeScript themeScript = new ShadleafThemeScript("cspNonce");
    write("theme-script.json", new ThemeScript(themeScript.getCspHash(), themeScript.getContent()));

    assertThat(skins).extracting(Skin::name).startsWith(DEFAULT_SKIN).contains("flat");
    for (Skin skin : skins) {
      assertThat(OUTPUT_DIRECTORY.resolve(skin.css())).exists();
    }
    assertThat(scripts).containsOnlyKeys("bundled", "csp", "external");
  }

  private Preview render(Scenario scenario) {
    Rendered rendered;
    try {
      rendered = tester.render(scenario.renderSource());
    } catch (RuntimeException e) {
      throw new AssertionError("Preview " + scenario.id() + " does not render: " + e.getMessage(), e);
    }
    assertThat(rendered.html()).as("preview %s", scenario.id()).isNotBlank();
    return new Preview(scenario.id(), scenario.component(), scenario.title(), scenario.description(),
        scenario.source(), rendered.html().strip(), rendered.normalizedHtml());
  }

  /** The standalone bundle of every skin, default first: the embedded ones carry no reset, so they cannot preview. */
  private List<Skin> copySkins(ViteManifest manifest) throws IOException {
    Set<String> names = new TreeSet<>();
    for (String key : manifest.entries().keySet()) {
      Matcher matcher = STANDALONE_ENTRY.matcher(key);
      if (matcher.matches()) {
        names.add(matcher.group(1));
      }
    }
    List<Skin> skins = new ArrayList<>();
    for (String name : names) {
      String file = manifest.getEntry(AssetVariant.STANDALONE.cssEntry(name)).file();
      copyAsset(file);
      Skin skin = new Skin(name, "shadleaf/" + file);
      skins.add(name.equals(DEFAULT_SKIN) ? 0 : skins.size(), skin);
    }
    return skins;
  }

  /** The script of every Alpine variant, keyed by its property value, and the chunks the scripts import. */
  private Map<String, String> copyScripts(ViteManifest manifest) throws IOException {
    Map<String, String> scripts = new LinkedHashMap<>();
    for (AlpineVariant alpine : AlpineVariant.values()) {
      ViteManifestParser.ViteManifestEntry entry = manifest.getEntry(alpine.jsEntry());
      copyAsset(entry.file());
      for (String chunk : entry.imports()) {
        String chunkFile = manifest.getEntry(chunk).file();
        if (!Files.exists(OUTPUT_DIRECTORY.resolve("shadleaf").resolve(chunkFile))) {
          copyAsset(chunkFile);
        }
      }
      scripts.put(alpine.name().toLowerCase(Locale.ROOT), "shadleaf/" + entry.file());
    }
    return scripts;
  }

  private static void copyAsset(String file) throws IOException {
    Path target = OUTPUT_DIRECTORY.resolve("shadleaf").resolve(file);
    Files.createDirectories(target.getParent());
    Files.copy(BUILT_ASSETS.resolve(file), target);
  }

  @SuppressWarnings("unchecked")
  private static List<Scenario> loadScenarios(ComponentRegistry registry) throws IOException {
    List<Path> files;
    try (Stream<Path> entries = Files.list(PREVIEWS_DIRECTORY)) {
      files = entries.filter(path -> path.getFileName().toString().endsWith(".yaml")).sorted().toList();
    }
    List<Scenario> scenarios = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    Set<String> components = new TreeSet<>();
    for (Path file : files) {
      Map<String, Object> document;
      try (InputStream in = Files.newInputStream(file)) {
        document = new Yaml().load(in);
      }
      String component = (String) document.get("component");
      assertThat(registry.names()).as("component of %s", file).contains(component);
      components.add(component);
      // A component family (card, card-header, ...) shares one file, which lists the other members as its parts.
      for (String part : (List<String>) document.getOrDefault("parts", List.of())) {
        assertThat(registry.names()).as("part of %s", file).contains(part);
        components.add(part);
      }
      for (Map<String, Object> entry : (List<Map<String, Object>>) document.get("scenarios")) {
        String id = component + "--" + entry.get("id");
        assertThat(ids.add(id)).as("scenario id %s is unique", id).isTrue();
        String source = (String) entry.get("source");
        assertThat(source).as("source of %s", id).isNotBlank();
        String renderSource = (String) entry.getOrDefault("renderSource", source);
        scenarios.add(new Scenario(id, component, (String) entry.get("title"), (String) entry.get("description"),
            source.strip(), renderSource));
      }
    }
    assertThat(components).as("components with previews in %s", PREVIEWS_DIRECTORY)
        .containsExactlyElementsOf(registry.names());
    return scenarios;
  }

  private void write(String name, Object value) throws IOException {
    Files.writeString(OUTPUT_DIRECTORY.resolve(name),
        jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n", StandardCharsets.UTF_8);
  }

  /**
   * @param source       the snippet shown to the reader
   * @param renderSource what is rendered, when it has to differ from the snippet (e.g. model variables replaced by
   *                     literals); the {@code source} otherwise
   */
  private record Scenario(String id, String component, @Nullable String title, @Nullable String description,
                          String source, String renderSource) {

  }

  /**
   * @param scripts the script of each Alpine variant ({@code bundled}, {@code csp}, {@code external}), relative to
   *                the output directory
   */
  private record Previews(String version, List<Skin> skins, Map<String, String> scripts, List<Preview> scenarios) {

  }

  /**
   * @param css path relative to the output directory, e.g. {@code shadleaf/assets/shadleaf-flat-abc.css}
   */
  private record Skin(String name, String css) {

  }

  /**
   * @param html           the rendered markup, as it goes into the preview frame
   * @param normalizedHtml one element per line, for the "rendered HTML" disclosure
   */
  private record Preview(String id, String component, @Nullable String title, @Nullable String description,
                         String source, String html, String normalizedHtml) {

  }

  private record ThemeScript(String cspHash, String script) {

  }
}
