package io.github.wimdeblauwe.shadleaf.item;

import io.github.wimdeblauwe.shadleaf.test.ComponentRenderTester;
import io.github.wimdeblauwe.shadleaf.test.HtmlApproval;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@code <sl:item>} and its parts, alone and composed, rendered into
 * {@code src/test/resources/approved/item.approved.html}.
 */
class ItemApprovalTest {

  private final ComponentRenderTester tester = ComponentRenderTester.create();

  @Test
  void itemAndParts() {
    HtmlApproval.verifyRenders("item", tester, List.of(
        "<sl:item>Content</sl:item>",
        "<sl:item variant=\"default\">Content</sl:item>",
        "<sl:item variant=\"outline\">Content</sl:item>",
        "<sl:item variant=\"muted\">Content</sl:item>",
        "<sl:item size=\"default\">Content</sl:item>",
        "<sl:item size=\"sm\">Content</sl:item>",
        "<sl:item size=\"xs\">Content</sl:item>",
        "<sl:item as=\"div\">Content</sl:item>",
        "<sl:item as=\"a\" href=\"/dashboard\">Content</sl:item>",
        "<sl:item-media>Media</sl:item-media>",
        "<sl:item-media variant=\"default\">Media</sl:item-media>",
        "<sl:item-media variant=\"icon\"><sl:icon name=\"inbox\"/></sl:item-media>",
        "<sl:item-media variant=\"image\"><img src=\"/covers/1\" alt=\"\"></sl:item-media>",
        "<sl:item-content>Content</sl:item-content>",
        "<sl:item-title>Title</sl:item-title>",
        "<sl:item-title level=\"3\">Title</sl:item-title>",
        "<sl:item-description>Description</sl:item-description>",
        "<sl:item-actions>Actions</sl:item-actions>",
        "<sl:item-header>Header</sl:item-header>",
        "<sl:item-footer>Footer</sl:item-footer>",
        "<sl:item-separator/>",
        "<sl:item-group>Items</sl:item-group>",
        "<sl:item-group layout=\"list\">Items</sl:item-group>",
        "<sl:item-group layout=\"grid\">Items</sl:item-group>",
        """
            <sl:item variant="outline">
              <sl:item-media variant="icon"><sl:icon name="shield-alert"/></sl:item-media>
              <sl:item-content>
                <sl:item-title>Security alert</sl:item-title>
                <sl:item-description>New login detected from an unknown device.</sl:item-description>
              </sl:item-content>
              <sl:item-actions>
                <sl:button size="sm" variant="outline">Review</sl:button>
              </sl:item-actions>
            </sl:item>""",
        """
            <sl:item-group>
              <sl:item th:variant="${'outline'}" th:size="${'sm'}">
                <sl:item-content><sl:item-title>[[${'Inbox'}]]</sl:item-title></sl:item-content>
              </sl:item>
              <sl:item-separator/>
              <sl:item as="a" th:href="@{/settings}">
                <sl:item-content><sl:item-title>Settings</sl:item-title></sl:item-content>
              </sl:item>
            </sl:item-group>"""));
  }
}
