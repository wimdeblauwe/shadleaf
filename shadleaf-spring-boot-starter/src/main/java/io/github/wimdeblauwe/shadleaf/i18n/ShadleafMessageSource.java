package io.github.wimdeblauwe.shadleaf.i18n;

import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;
import org.springframework.context.HierarchicalMessageSource;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * The components' built-in strings ({@code sl.*} keys) from {@code shadleaf/messages.properties}.
 * <p>
 * It is never the application's message source: it sits at the end of that source's parent chain, so the application
 * rewords or translates a string by defining the same key. See {@link ShadleafMessageSourcePostProcessor}.
 */
public class ShadleafMessageSource extends ResourceBundleMessageSource {

  public static final String BASENAME = "shadleaf/messages";

  public ShadleafMessageSource() {
    setBasename(BASENAME);
    setDefaultEncoding(StandardCharsets.UTF_8.name());
    setFallbackToSystemLocale(false);
  }

  /**
   * Makes a new {@code ShadleafMessageSource} the last parent of {@code messageSource}, unless one is already in its
   * chain.
   *
   * @return whether {@code messageSource} now falls back to the Shadleaf strings
   */
  public static boolean attachTo(MessageSource messageSource) {
    MessageSource current = messageSource;
    while (true) {
      if (current instanceof ShadleafMessageSource) {
        return true;
      }
      if (!(current instanceof HierarchicalMessageSource hierarchical)) {
        return false;
      }
      @Nullable MessageSource parent = hierarchical.getParentMessageSource();
      if (parent == null) {
        hierarchical.setParentMessageSource(new ShadleafMessageSource());
        return true;
      }
      current = parent;
    }
  }
}
