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
 * <p>
 * Content the caller provided is evaluated in the caller's scope for {@code props}, {@code attrs} and {@code slots}:
 * a library template that passes {@code ${props.title}} into another component's slot means its own props, not the
 * receiving component's. This also lets a template pass its own slot on ({@code <sl:x><sl:slot/></sl:x>}), which
 * would otherwise resolve to itself forever. The fallback content belongs to the receiving template and keeps its
 * scope. Other local variables of the receiving template still hide the caller's, so component templates give their
 * {@code th:with} names an {@code sl} prefix.
 */
public class SlotElementProcessor extends AbstractElementModelProcessor {

  private static final String NAME_ATTRIBUTE = "name";

  public SlotElementProcessor(String dialectPrefix) {
    super(TemplateMode.HTML, dialectPrefix, "slot", true, null, false, StandardDialect.PROCESSOR_PRECEDENCE);
  }

  @Override
  protected void doProcess(ITemplateContext context, IModel model, IElementModelStructureHandler structureHandler) {
    Slots slots = context.getVariable(ComponentElementProcessor.SLOTS_VARIABLE) instanceof Slots s ? s : null;
    IModel provided = getProvidedContent(slots, getSlotName(model));
    IModel content;
    if (slots != null && Slots.hasContent(provided)) {
      content = provided;
      restoreCallerScope(slots.callerScope(), structureHandler);
    } else {
      content = getDefaultContent(model, context.getModelFactory());
    }

    model.reset();
    for (int i = 0; i < content.size(); i++) {
      model.add(content.get(i));
    }
  }

  private static void restoreCallerScope(Slots.CallerScope caller, IElementModelStructureHandler structureHandler) {
    structureHandler.setLocalVariable(ComponentElementProcessor.PROPS_VARIABLE, caller.props());
    structureHandler.setLocalVariable(ComponentElementProcessor.ATTRS_VARIABLE, caller.attrs());
    structureHandler.setLocalVariable(ComponentElementProcessor.SLOTS_VARIABLE, caller.slots());
  }

  private static @Nullable IModel getProvidedContent(@Nullable Slots slots, @Nullable String name) {
    if (slots == null) {
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
