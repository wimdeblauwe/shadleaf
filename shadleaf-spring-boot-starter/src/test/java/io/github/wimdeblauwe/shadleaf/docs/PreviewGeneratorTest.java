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
import io.github.wimdeblauwe.shadleaf.test.FormModel;
import io.github.wimdeblauwe.shadleaf.test.LibraryComponents;
import io.github.wimdeblauwe.shadleaf.test.Rendered;
import io.github.wimdeblauwe.shadleaf.theme.ShadleafThemeScript;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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
 * A scenario's {@code renderSource} can show a photo from {@code previews/photos/} as {@code ${photos.<name>}}, a data:
 * URI, so the previews and the showcase checks never fetch an image.
 * {@code docs/scripts/sync.mjs} copies them into the docs project. Adding examples for a component means adding a
 * YAML file; every library component must have one, or be listed under the {@code parts} of its family's file.
 */
class PreviewGeneratorTest {

  static final String OUTPUT = "target/generated-docs";

  private static final Path OUTPUT_DIRECTORY = Path.of(OUTPUT);
  private static final Path PREVIEWS_DIRECTORY = Path.of("src", "test", "resources", "previews");
  private static final Path PHOTOS_DIRECTORY = PREVIEWS_DIRECTORY.resolve("photos");
  private static final Map<String, String> PHOTO_TYPES = Map.of("jpg", "image/jpeg", "png", "image/png",
      "svg", "image/svg+xml");
  private static final Path BUILT_ASSETS = Path.of("target", "classes", "META-INF", "resources", "shadleaf");
  private static final Path MANIFEST = Path.of("target", "classes").resolve(ShadleafAssets.MANIFEST_LOCATION);
  private static final Pattern STANDALONE_ENTRY = Pattern.compile("css/entries/shadleaf-([^.]+)\\.css");
  private static final String DEFAULT_SKIN = "vega";

  private final JsonMapper jsonMapper = JsonMapper.builder().build();
  private final ComponentRenderTester tester = ComponentRenderTester.create();
  private final Map<String, ComponentRenderTester> testersByRequest = new HashMap<>();
  private final Map<String, String> photos = loadPhotos();

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

    assertThat(skins).extracting(Skin::name).startsWith(DEFAULT_SKIN).contains("lyra");
    for (Skin skin : skins) {
      assertThat(OUTPUT_DIRECTORY.resolve(skin.css())).exists();
    }
    assertThat(scripts).containsOnlyKeys("bundled", "csp", "external");
  }

  private Preview render(Scenario scenario) {
    Rendered rendered;
    ComponentRenderTester tester = scenario.request() == null
        ? this.tester
        : testersByRequest.computeIfAbsent(scenario.request(),
            request -> ComponentRenderTester.builder().requestUri(request).build());
    try {
      if (scenario.form() == null) {
        rendered = tester.render(scenario.renderSource(), Map.of("photos", photos));
      } else {
        Map<String, Object> variables = new LinkedHashMap<>(scenario.form().variables());
        variables.put("photos", photos);
        rendered = tester.render(FormModel.wrap(scenario.renderSource()), variables);
      }
    } catch (RuntimeException e) {
      throw new AssertionError("Preview " + scenario.id() + " does not render: " + e.getMessage(), e);
    }
    assertThat(rendered.html()).as("preview %s", scenario.id()).isNotBlank();
    return new Preview(scenario.id(), scenario.component(), scenario.title(), scenario.description(),
        scenario.source(), rendered.html().strip(), rendered.normalizedHtml(), scenario.showcase());
  }

  /** Every photo in {@code previews/photos/}, by file name without extension, as a data: URI. */
  private static Map<String, String> loadPhotos() {
    Map<String, String> photos = new LinkedHashMap<>();
    try (Stream<Path> files = Files.list(PHOTOS_DIRECTORY)) {
      for (Path file : files.sorted().toList()) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String type = PHOTO_TYPES.get(name.substring(dot + 1));
        assertThat(type).as("type of %s", file).isNotNull();
        photos.put(name.substring(0, dot),
            "data:" + type + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(file)));
      }
    } catch (IOException e) {
      throw new AssertionError("Cannot read " + PHOTOS_DIRECTORY, e);
    }
    return photos;
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
            source.strip(), renderSource, formModel((Map<String, Object>) entry.get("form")),
            (Boolean) entry.getOrDefault("showcase", true), (String) entry.get("request")));
      }
    }
    assertThat(components).as("components with previews in %s", PREVIEWS_DIRECTORY)
        .containsExactlyElementsOf(registry.names());
    return scenarios;
  }

  /**
   * A scenario's {@code form}: {@code values} and {@code errors} (a message or a list of them), by field name, and
   * {@code globalErrors} (a message or a list of them) for the whole form. The
   * snippet then renders inside {@code th:object="${form}"}, so {@code th:field} works as in an application.
   */
  @SuppressWarnings("unchecked")
  private static @Nullable FormModel formModel(@Nullable Map<String, Object> form) {
    if (form == null) {
      return null;
    }
    Map<String, List<String>> errors = new LinkedHashMap<>();
    ((Map<String, Object>) form.getOrDefault("errors", Map.of())).forEach((field, messages) ->
        errors.put(field, messages instanceof List<?> list ? (List<String>) list : List.of((String) messages)));
    Object globalErrors = form.getOrDefault("globalErrors", List.of());
    return FormModel.of((Map<String, Object>) form.getOrDefault("values", Map.of()), errors,
        globalErrors instanceof List<?> list ? (List<String>) list : List.of((String) globalErrors));
  }

  private void write(String name, Object value) throws IOException {
    Files.writeString(OUTPUT_DIRECTORY.resolve(name),
        jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value) + "\n", StandardCharsets.UTF_8);
  }

  /**
   * @param source       the snippet shown to the reader
   * @param renderSource what is rendered, when it has to differ from the snippet (e.g. model variables replaced by
   *                     literals); the {@code source} otherwise
   * @param form         the form object {@code th:field} binds to, if the scenario has one
   * @param showcase     whether the scenario is on the showcase page too; {@code false} for one that would get in the
   *                     way of the other components' tests there (a toast shown when the page loads)
   * @param request      the path and query of the request it renders in ({@code /people?sort=name,desc}), for links
   *                     built from the request such as a table's sort links; {@code /} when absent
   */
  private record Scenario(String id, String component, @Nullable String title, @Nullable String description,
                          String source, String renderSource, @Nullable FormModel form, boolean showcase,
                          @Nullable String request) {

  }

  /**
   * @param scripts the script of each Alpine variant ({@code bundled}, {@code csp}, {@code external}), relative to
   *                the output directory
   */
  private record Previews(String version, List<Skin> skins, Map<String, String> scripts, List<Preview> scenarios) {

  }

  /**
   * @param css path relative to the output directory, e.g. {@code shadleaf/assets/shadleaf-lyra-abc.css}
   */
  private record Skin(String name, String css) {

  }

  /**
   * @param html           the rendered markup, as it goes into the preview frame
   * @param normalizedHtml one element per line, for the "rendered HTML" disclosure
   * @param showcase       whether the showcase page shows it
   */
  private record Preview(String id, String component, @Nullable String title, @Nullable String description,
                         String source, String html, String normalizedHtml, boolean showcase) {

  }

  private record ThemeScript(String cspHash, String script) {

  }
}
