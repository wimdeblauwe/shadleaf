package io.github.wimdeblauwe.shadleaf.dialect;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.thymeleaf.model.ICloseElementTag;
import org.thymeleaf.model.IComment;
import org.thymeleaf.model.IElementTag;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IOpenElementTag;
import org.thymeleaf.model.IProcessableElementTag;
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

  private final String dialectPrefix;
  private final IModel defaultSlot;
  private final Map<String, IModel> namedSlots;
  private final CallerScope callerScope;

  Slots(String dialectPrefix, IModel defaultSlot, Map<String, IModel> namedSlots, CallerScope callerScope) {
    this.dialectPrefix = dialectPrefix;
    this.defaultSlot = defaultSlot;
    this.namedSlots = Map.copyOf(namedSlots);
    this.callerScope = callerScope;
  }

  /** Whether the named slot was given non-blank content. */
  public boolean has(String name) {
    return isFilled(namedSlots.get(name));
  }

  /** Whether the default slot (the content outside any named slot) is non-blank. */
  public boolean hasDefault() {
    return isFilled(defaultSlot);
  }

  /**
   * Whether slot content counts as given. Content that is only an {@code <sl:slot>} passes a slot of the caller's on
   * ({@code <sl:sidebar-menu-button><sl:slot name="icon-start"><sl:slot name="icon-start"/></sl:slot>}): it counts
   * only when that slot of the caller's was given, or the passing slot's own fallback is not blank. Otherwise a
   * component that passes its slots on would make every one of them look given.
   */
  boolean isFilled(@Nullable IModel model) {
    if (!hasContent(model)) {
      return false;
    }
    PassedOn passedOn = passedOn(model);
    if (passedOn == null) {
      return true;
    }
    if (callerScope.slots() instanceof Slots caller
        && (passedOn.name() == null ? caller.hasDefault() : caller.has(passedOn.name()))) {
      return true;
    }
    return hasContent(passedOn.fallback());
  }

  /** The slot that {@code model} passes on, when it holds nothing but one {@code <sl:slot>} element. */
  private @Nullable PassedOn passedOn(IModel model) {
    String slotElement = elementName("slot");
    int start = -1;
    int end = -1;
    int depth = 0;
    for (int i = 0; i < model.size(); i++) {
      ITemplateEvent event = model.get(i);
      if (depth == 0 && isBlank(event)) {
        continue;
      }
      boolean slotTag = event instanceof IElementTag tag
          && tag.getElementCompleteName().toLowerCase(Locale.ROOT).equals(slotElement);
      if (depth == 0) {
        if (start >= 0 || !slotTag || event instanceof ICloseElementTag) {
          return null;
        }
        start = i;
        if (event instanceof IOpenElementTag) {
          depth = 1;
        } else {
          end = i;
        }
      } else if (slotTag && event instanceof IOpenElementTag) {
        depth++;
      } else if (slotTag && event instanceof ICloseElementTag && --depth == 0) {
        end = i;
      }
    }
    if (start < 0 || depth != 0 || !(model.get(start) instanceof IProcessableElementTag tag)) {
      return null;
    }
    String name = tag.getAttributeValue("name");
    IModel fallback = model.cloneModel();
    fallback.reset();
    for (int i = start + 1; i < end; i++) {
      fallback.add(model.get(i));
    }
    return new PassedOn(name == null || name.isBlank() ? null : name, fallback);
  }

  private static boolean isBlank(ITemplateEvent event) {
    return event instanceof IComment || (event instanceof IText text && text.getText().isBlank());
  }

  private record PassedOn(@Nullable String name, IModel fallback) {}

  /**
   * Whether the content passed in, in any slot and at any depth, holds a use of the component {@code name}, not
   * counting uses inside a nested {@code notInside} component.
   * <p>
   * For a parent that has to know about a part before the part renders: {@code sl:field} points its control's
   * {@code aria-describedby} at its description only when it has one, with
   * {@code slots.contains('field-description', 'field')} (a nested field's description is that field's). The content
   * is looked at as written: a part added by {@code th:replace} is not seen, and one with a false {@code th:if} is.
   */
  public boolean contains(String name, String... notInside) {
    String element = elementName(name);
    List<String> skipped = Arrays.stream(notInside).map(this::elementName).toList();
    if (contains(defaultSlot, element, skipped)) {
      return true;
    }
    for (IModel slot : namedSlots.values()) {
      if (contains(slot, element, skipped)) {
        return true;
      }
    }
    return false;
  }

  private String elementName(String name) {
    return (dialectPrefix + ":" + name).toLowerCase(Locale.ROOT);
  }

  private static boolean contains(IModel model, String element, List<String> skipped) {
    int skipDepth = 0;
    for (int i = 0; i < model.size(); i++) {
      if (!(model.get(i) instanceof IElementTag tag)) {
        continue;
      }
      String name = tag.getElementCompleteName().toLowerCase(Locale.ROOT);
      if (skipDepth > 0) {
        if (tag instanceof IOpenElementTag) {
          skipDepth++;
        } else if (tag instanceof ICloseElementTag) {
          skipDepth--;
        }
      } else if (name.equals(element) && !(tag instanceof ICloseElementTag)) {
        return true;
      } else if (skipped.contains(name) && tag instanceof IOpenElementTag) {
        skipDepth = 1;
      }
    }
    return false;
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
      if (!isBlank(model.get(i))) {
        return true;
      }
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
