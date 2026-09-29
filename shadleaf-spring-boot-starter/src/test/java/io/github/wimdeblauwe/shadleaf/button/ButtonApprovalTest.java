package io.github.wimdeblauwe.shadleaf.button;

import io.github.wimdeblauwe.shadleaf.component.ClasspathComponentDefinitionSource;
import io.github.wimdeblauwe.shadleaf.component.ComponentDefinition;
import io.github.wimdeblauwe.shadleaf.component.ComponentRegistry;
import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every variant × size of {@code <sl:button>}, and its states, rendered into one approved file:
 * {@code src/test/resources/approved/button.approved.html}. A change to the markup the button emits is a change to
 * that file, reviewed like any other. See {@link HtmlApproval} for how to accept a deliberate change.
 * <p>
 * The variants and sizes come from the button's {@code <sl:props>}, so a new value fails this test until its markup
 * is approved. Icon paths are left out: they belong to the lucide catalogue, not to the button.
 */
class ButtonApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void variantsSizesAndStates() {
    ComponentDefinition button = new ComponentRegistry(List.of(
        new ClasspathComponentDefinitionSource(getClass().getClassLoader()))).get("button");

    List<String> snippets = new ArrayList<>();
    for (String variant : button.prop("variant").values()) {
      for (String size : button.prop("size").values()) {
        snippets.add(size.startsWith("icon")
            ? "<sl:button variant=\"%s\" size=\"%s\" aria-label=\"Add\"><sl:icon name=\"plus\"/></sl:button>"
            .formatted(variant, size)
            : "<sl:button variant=\"%s\" size=\"%s\">Save</sl:button>".formatted(variant, size));
      }
    }
    snippets.addAll(List.of(
        "<sl:button>Save</sl:button>",
        "<sl:button type=\"submit\">Save</sl:button>",
        "<sl:button type=\"reset\">Reset</sl:button>",
        "<sl:button disabled>Save</sl:button>",
        "<sl:button loading>Save</sl:button>",
        "<sl:button size=\"icon\" loading aria-label=\"Delete\"><sl:icon name=\"trash\"/></sl:button>",
        "<sl:button variant=\"destructive\"><sl:slot name=\"icon-start\"><sl:icon name=\"trash\"/></sl:slot>Delete</sl:button>",
        "<sl:button loading><sl:slot name=\"icon-start\"><sl:icon name=\"save\"/></sl:slot>Save</sl:button>",
        "<sl:button>Next<sl:slot name=\"icon-end\"><sl:icon name=\"arrow-right\"/></sl:slot></sl:button>",
        "<sl:button class=\"ml-auto\" id=\"save\" hx-post=\"/orders\" x-on:click=\"open = false\">Save</sl:button>",
        "<sl:button as=\"a\" href=\"/orders\">Orders</sl:button>",
        "<sl:button as=\"a\" variant=\"outline\" size=\"sm\" href=\"/orders\">Orders</sl:button>",
        "<sl:button as=\"a\" disabled href=\"/orders\">Orders</sl:button>",
        "<sl:button as=\"a\" loading href=\"/orders\">Orders</sl:button>"));

    HtmlApproval.verifyRenders("button", tester, snippets);
  }
}
