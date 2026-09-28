package io.github.wimdeblauwe.shadleaf.dev;

import java.util.Map;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.FileTemplateResolver;

/**
 * Reads the library's templates uncached from {@code shadleaf.dev.templates-path}, so edits show up without a
 * rebuild, except the ones the application overrides with its own copy: those resolve from the classpath as usual.
 */
public class ShadleafDevTemplateResolver extends FileTemplateResolver {

  private final ApplicationTemplateOverrides overrides;

  public ShadleafDevTemplateResolver(ApplicationTemplateOverrides overrides) {
    this.overrides = overrides;
  }

  @Override
  protected boolean computeResolvable(IEngineConfiguration configuration, String ownerTemplate, String template,
      Map<String, Object> templateResolutionAttributes) {
    return super.computeResolvable(configuration, ownerTemplate, template, templateResolutionAttributes)
        && !overrides.isOverridden(template);
  }
}
