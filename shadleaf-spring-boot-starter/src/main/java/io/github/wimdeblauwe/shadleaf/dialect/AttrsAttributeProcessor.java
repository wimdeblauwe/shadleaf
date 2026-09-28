package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.Map;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * {@code sl:attrs} on the root element of a component template: copies every attribute of the component tag that
 * no prop consumed onto this element. {@code class} is appended to the element's own classes; any other attribute
 * replaces the element's value.
 * <p>
 * Attributes are copied verbatim, {@code th:*} ones included. They are evaluated here, on the rendered element, by
 * the standard processors that run after this one. That is why {@code th:text}, {@code th:href} or
 * {@code th:hx-post} on a component tag simply work.
 */
public class AttrsAttributeProcessor extends AbstractAttributeTagProcessor {

  private static final String ATTR_NAME = "attrs";
  private static final String CLASS_ATTRIBUTE = "class";

  // After th:attr (700), so the template's own attributes are in place and a passed attribute can override them, but
  // before th:classappend (1100), th:text (1300) and the default attribute processor (Integer.MAX_VALUE), so the
  // th:* attributes it copies are still evaluated on this element.
  static final int PRECEDENCE = 750;

  public AttrsAttributeProcessor(String dialectPrefix) {
    super(TemplateMode.HTML, dialectPrefix, null, false, ATTR_NAME, true, PRECEDENCE, true);
  }

  @Override
  protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
      String attributeValue, IElementTagStructureHandler structureHandler) {
    if (!(context.getVariable(ComponentElementProcessor.ATTRS_VARIABLE) instanceof Map<?, ?> attrs)) {
      return;
    }
    for (Map.Entry<?, ?> entry : attrs.entrySet()) {
      String name = String.valueOf(entry.getKey());
      String value = entry.getValue() == null ? null : String.valueOf(entry.getValue());
      if (name.equals(CLASS_ATTRIBUTE)) {
        String existing = tag.getAttributeValue(CLASS_ATTRIBUTE);
        boolean hasExisting = existing != null && !existing.isBlank();
        boolean hasValue = value != null && !value.isBlank();
        structureHandler.setAttribute(CLASS_ATTRIBUTE,
            hasExisting && hasValue ? existing + " " + value : hasExisting ? existing : value);
      } else {
        structureHandler.setAttribute(name, value);
      }
    }
  }
}
