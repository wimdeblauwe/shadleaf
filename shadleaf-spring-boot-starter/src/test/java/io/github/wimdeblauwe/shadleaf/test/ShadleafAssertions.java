package io.github.wimdeblauwe.shadleaf.test;

import io.github.wimdeblauwe.shadleaf.component.ShadleafComponentException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jsoup.nodes.Element;

/**
 * AssertJ entry point for rendered components. It extends {@link Assertions}, so one static import covers both:
 * <pre>{@code
 * import static io.github.wimdeblauwe.shadleaf.test.ShadleafAssertions.*;
 *
 * assertThat(tester.render("<sl:button variant='outline'>Save</sl:button>")).root()
 *     .hasTag("button")
 *     .hasClassName("btn")
 *     .hasAttribute("data-variant", "outline")
 *     .hasNoAttribute("variant");
 * assertThatRenderFailure(() -> tester.render("<sl:button size='icon'><sl:icon name='x'/></sl:button>"))
 *     .hasMessageContaining("aria-label");
 * }</pre>
 */
public class ShadleafAssertions extends Assertions {

  protected ShadleafAssertions() {
  }

  public static RenderedAssert assertThat(Rendered actual) {
    return new RenderedAssert(actual);
  }

  public static ElementAssert assertThat(Element actual) {
    return new ElementAssert(actual);
  }

  /**
   * Expects the render to fail with a {@link ShadleafComponentException} and asserts on that exception. Thymeleaf
   * wraps it in a {@code TemplateProcessingException}; this finds it in the cause chain.
   */
  public static AbstractThrowableAssert<?, ? extends Throwable> assertThatRenderFailure(ThrowingCallable render) {
    Throwable thrown = catchThrowable(render);
    if (thrown == null) {
      throw new AssertionError("Expected the render to fail with a ShadleafComponentException, but it succeeded");
    }
    for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
      if (cause instanceof ShadleafComponentException) {
        return Assertions.assertThat(cause);
      }
    }
    throw new AssertionError("Expected the render to fail with a ShadleafComponentException, but got:", thrown);
  }
}
