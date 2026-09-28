package io.github.wimdeblauwe.shadleaf.dialect;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementModelProcessor;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import org.thymeleaf.standard.StandardDialect;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * Replaces {@code <sl:slot>} in a component template with the content the caller passed for that slot, or with the
 * slot's own body when the caller passed nothing (or only whitespace).
 * <p>
 * {@code <sl:slot>} is the default slot, {@code <sl:slot name="x">} a named one.
 */
public class SlotElementProcessor extends AbstractElementModelProcessor {

  private static final String NAME_ATTRIBUTE = "name";

  public SlotElementProcessor(String dialectPrefix) {
    super(TemplateMode.HTML, dialectPrefix, "slot", true, null, false, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  @Override
  protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
    IModel provided = getProvidedContent(context, getSlotName(model));
    IModel content = Slots.hasContent(provided) ? provided : getDefaultContent(model, context.getModelFactory());

    model.reset();
    for (int i = 0; i < content.size(); i++) {
      model.add(content.get(i));
    }
  }

  private static @Nullable IModel getProvidedContent(ITemplateContext context, @Nullable String name) {
    if (!(context.getVariable(ComponentElementProcessor.SLOTS_VARIABLE) instanceof Slots slots)) {
      return null;
    }
    return StringUtils.hasText(name) ? slots.named(name) : slots.defaultSlot();
  }

  private static IModel getDefaultContent(IModel model, IModelFactory modelFactory) {
    IModel defaultContent = modelFactory.createModel();
    for (int i = 1; i < model.size() - 1; i++) {
      defaultContent.add(model.get(i));
    }
    return defaultContent;
  }

  private static @Nullable String getSlotName(IModel model) {
    return (model.get(0) instanceof IProcessableElementTag tag)
        ? tag.getAttributeValue(NAME_ATTRIBUTE)
        : null;
  }
}
