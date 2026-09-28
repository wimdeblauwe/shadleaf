package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

/**
 * The {@code sl} dialect. Templates declare it as {@code xmlns:sl="https://shadleaf.dev/sl"}.
 * <p>
 * The prefix is fixed: the library's own templates use {@code sl:*} internally.
 * <p>
 * The dialect shares the standard dialect's precedence on purpose. Thymeleaf orders processors by dialect precedence
 * first, so a higher value would make every {@code th:*} processor, {@code th:text} included, run before the
 * component processor.
 */
public class ShadleafDialect extends AbstractProcessorDialect {

  public static final String PREFIX = "sl";
  public static final String NAMESPACE_URI = "https://shadleaf.dev/sl";
  private static final String NAME = "Shadleaf";

  private final ComponentRegistry registry;

  public ShadleafDialect(ComponentRegistry registry) {
    super(NAME, PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
    this.registry = registry;
  }

  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    return Set.of(
        new ComponentElementProcessor(dialectPrefix, registry),
        new AttrsAttributeProcessor(dialectPrefix),
        new SlotElementProcessor(dialectPrefix));
  }
}
