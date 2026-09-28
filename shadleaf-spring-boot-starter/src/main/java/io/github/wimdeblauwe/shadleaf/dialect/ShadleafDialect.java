package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.Set;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

/**
 * The {@code sl} dialect. Templates declare it as {@code xmlns:sl="https://shadleaf.dev/sl"}.
 * <p>
 * The prefix is fixed: the library's own templates use {@code sl:*} internally.
 */
public class ShadleafDialect extends AbstractProcessorDialect {

  public static final String PREFIX = "sl";
  public static final String NAMESPACE_URI = "https://shadleaf.dev/sl";
  private static final String NAME = "Shadleaf";

  public ShadleafDialect() {
    super(NAME, PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    // The component engine (sl:props, sl:attrs, sl:slot) arrives in M1.
    return Set.of();
  }
}
