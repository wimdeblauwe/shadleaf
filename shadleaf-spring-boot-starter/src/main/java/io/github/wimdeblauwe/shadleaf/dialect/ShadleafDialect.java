package io.github.wimdeblauwe.shadleaf.dialect;

import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.icon.IconRegistry;
import io.github.wimdeblauwe.shadleaf.paging.Paging;
import io.github.wimdeblauwe.shadleaf.paging.PagingParameters;
import io.github.wimdeblauwe.shadleaf.sidebar.SidebarState;
import java.util.Set;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;
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
 * <p>
 * It also provides the expression object {@code #slIcons}, the {@link IconRegistry} that {@code <sl:icon>} inlines
 * its SVG from. An expression object rather than a bean reference, so it works without a Spring application context.
 * And {@code #slFields}, the {@link FieldBindings} through which {@code <sl:field>} shares its {@code th:field} with
 * its parts and control. And {@code #slPaging}, the {@link Paging} that builds the sort links of a table from the
 * request, with Spring Data's parameter names ({@link PagingParameters}). And {@code #slSidebar}, the
 * {@link SidebarState} that {@code sl:sidebar-provider} reads from its cookie.
 */
public class ShadleafDialect extends AbstractProcessorDialect implements IExpressionObjectDialect {

  public static final String PREFIX = "sl";
  public static final String NAMESPACE_URI = "https://shadleaf.dev/sl";
  public static final String ICONS_EXPRESSION_OBJECT = "slIcons";
  public static final String FIELDS_EXPRESSION_OBJECT = "slFields";
  public static final String PAGING_EXPRESSION_OBJECT = "slPaging";
  public static final String SIDEBAR_EXPRESSION_OBJECT = "slSidebar";
  private static final String NAME = "Shadleaf";

  private final ComponentRegistry registry;
  private final IExpressionObjectFactory expressionObjectFactory;

  /** With Spring Data's default parameter names. */
  public ShadleafDialect(ComponentRegistry registry, IconRegistry iconRegistry) {
    this(registry, iconRegistry, PagingParameters.defaults());
  }

  public ShadleafDialect(ComponentRegistry registry, IconRegistry iconRegistry, PagingParameters pagingParameters) {
    super(NAME, PREFIX, StandardDialect.PROCESSOR_PRECEDENCE);
    this.registry = registry;
    this.expressionObjectFactory = new IExpressionObjectFactory() {
      @Override
      public Set<String> getAllExpressionObjectNames() {
        return Set.of(ICONS_EXPRESSION_OBJECT, FIELDS_EXPRESSION_OBJECT, PAGING_EXPRESSION_OBJECT,
            SIDEBAR_EXPRESSION_OBJECT);
      }

      @Override
      public Object buildObject(IExpressionContext context, String expressionObjectName) {
        return switch (expressionObjectName) {
          case ICONS_EXPRESSION_OBJECT -> iconRegistry;
          case FIELDS_EXPRESSION_OBJECT -> new FieldBindings(context);
          case PAGING_EXPRESSION_OBJECT -> new Paging(context, pagingParameters);
          case SIDEBAR_EXPRESSION_OBJECT -> new SidebarState(context);
          default -> null;
        };
      }

      @Override
      public boolean isCacheable(String expressionObjectName) {
        return true;
      }
    };
  }

  @Override
  public IExpressionObjectFactory getExpressionObjectFactory() {
    return expressionObjectFactory;
  }

  @Override
  public Set<IProcessor> getProcessors(String dialectPrefix) {
    return Set.of(
        new ComponentElementProcessor(dialectPrefix, registry),
        new AttrsAttributeProcessor(dialectPrefix),
        new SlotElementProcessor(dialectPrefix));
  }
}
