package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.ITemplateEvent;
import org.thymeleaf.model.IText;

/**
 * The content passed into one use of a component, available in its template as {@code slots}.
 * <p>
 * {@code ${slots.has('icon-start')}} lets a template emit a wrapper element only when there is something to wrap.
 * Content that is only whitespace or comments counts as absent, both here and for {@code <sl:slot>}'s fallback
 * content.
 * <p>
 * It also remembers the caller's {@code props}, {@code attrs} and {@code slots}: the values those names had where the
 * component was used. Slot content is evaluated inside the component's template, where the component's own variables
 * would hide them, so {@link SlotElementProcessor} puts the caller's back for the content the caller provided.
 */
public final class Slots {

  private final IModel defaultSlot;
  private final Map<String, IModel> namedSlots;
  private final CallerScope callerScope;

  Slots(IModel defaultSlot, Map<String, IModel> namedSlots, CallerScope callerScope) {
    this.defaultSlot = defaultSlot;
    this.namedSlots = Map.copyOf(namedSlots);
    this.callerScope = callerScope;
  }

  /** Whether the named slot was given non-blank content. */
  public boolean has(String name) {
    return hasContent(namedSlots.get(name));
  }

  /** Whether the default slot (the content outside any named slot) is non-blank. */
  public boolean hasDefault() {
    return hasContent(defaultSlot);
  }

  IModel defaultSlot() {
    return defaultSlot;
  }

  @Nullable IModel named(String name) {
    return namedSlots.get(name);
  }

  CallerScope callerScope() {
    return callerScope;
  }

  static boolean hasContent(@Nullable IModel model) {
    if (model == null) {
      return false;
    }
    for (int i = 0; i < model.size(); i++) {
      ITemplateEvent event = model.get(i);
      if (event instanceof IComment) {
        continue;
      }
      if (event instanceof IText text && text.getText().isBlank()) {
        continue;
      }
      return true;
    }
    return false;
  }

  /**
   * The values of {@code props}, {@code attrs} and {@code slots} where the component was used: an enclosing
   * component's in a library template, {@code null} (or the application's own variables of that name) in an
   * application template.
   */
  record CallerScope(@Nullable Object props, @Nullable Object attrs, @Nullable Object slots) {}
}
