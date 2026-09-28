package io.github.wimdeblauwe.shadleaf.component;

import java.util.Set;
import java.util.TreeSet;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.IElementProcessor;
import org.thymeleaf.processor.element.MatchingAttributeName;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.standard.processor.StandardConditionalFixedValueTagProcessor;
import org.thymeleaf.standard.processor.StandardDOMEventAttributeTagProcessor;
import org.thymeleaf.standard.processor.StandardNonRemovableAttributeTagProcessor;
import org.thymeleaf.standard.processor.StandardRemovableAttributeTagProcessor;

/**
 * Names a prop cannot have.
 * <p>
 * A prop {@code foo} can be given as an expression with {@code th:foo}. If a standard processor gives {@code th:foo}
 * a meaning of its own ({@code th:text}, {@code th:if}, {@code th:each}, {@code th:href}, ...), that would be
 * ambiguous. The names are read from the processors of the standard dialect, so they follow Thymeleaf upgrades.
 * <p>
 * Plain attribute setters such as {@code th:size}, {@code th:type} or {@code th:disabled} are not reserved: whether
 * the standard processor sets {@code size} first or the component processor evaluates {@code th:size} itself, the
 * prop ends up with the same value.
 * <p>
 * {@code class} is reserved too: it is always passed through and merged with the component's own classes.
 */
final class ReservedPropNames {

  static final Set<String> NAMES = computeNames();

  private ReservedPropNames() {
  }

  static boolean isReserved(String propName) {
    return NAMES.contains(propName);
  }

  private static Set<String> computeNames() {
    Set<String> names = new TreeSet<>();
    String prefix = SpringStandardDialect.PREFIX;
    for (IProcessor processor : new SpringStandardDialect().getProcessors(prefix)) {
      if (processor instanceof IElementProcessor elementProcessor && !isPlainAttributeSetter(processor)) {
        MatchingAttributeName matching = elementProcessor.getMatchingAttributeName();
        AttributeName attributeName = matching == null ? null : matching.getMatchingAttributeName();
        if (attributeName != null && prefix.equals(attributeName.getPrefix())) {
          names.add(attributeName.getAttributeName());
        }
      }
    }
    names.add("class");
    return Set.copyOf(names);
  }

  private static boolean isPlainAttributeSetter(IProcessor processor) {
    return processor instanceof StandardNonRemovableAttributeTagProcessor
        || processor instanceof StandardRemovableAttributeTagProcessor
        || processor instanceof StandardConditionalFixedValueTagProcessor
        || processor instanceof StandardDOMEventAttributeTagProcessor;
  }
}
