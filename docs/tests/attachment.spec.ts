import {expect, test, type Page} from '@playwright/test';
import {combinations, openShowcase} from './showcase';

// sl:attachment: no script, so this checks what the stylesheet does. On the showcase, in every skin and theme: the
// trigger covers the card behind the actions, Tab reaches the actions and then the trigger, whose focus ring is drawn
// around the card, and a labelled group takes the focus with a ring its edge fade does not hide. Once: the group
// scrolls and snaps to an attachment, Tab brings an attachment off screen into view, the arrow keys scroll a labelled group, the trigger opens its dialog while the
// actions do not, and the title shimmers while uploading, but not for users who ask for reduced motion.

const scenario = (page: Page, id: string) => page.locator(`[data-scenario="${id}"]`);

for (const {skin, theme} of combinations) {
  test.describe(`${skin}, ${theme}`, () => {
    test.beforeEach(async ({page}) => openShowcase(page, skin, theme));

    test('the trigger covers the card behind its actions', async ({page}) => {
      const attachment = scenario(page, 'attachment--trigger').locator('.attachment');
      await attachment.scrollIntoViewIfNeeded();
      const card = (await attachment.boundingBox())!;
      const trigger = (await attachment.locator('.attachment-trigger').boundingBox())!;
      expect(trigger.width).toBeGreaterThanOrEqual(card.width - 4);
      expect(trigger.height).toBeGreaterThanOrEqual(card.height - 4);

      // What a click on the title and on an action hits.
      const hit = (selector: string) => attachment.locator(selector).evaluate(element => {
        const box = element.getBoundingClientRect();
        const target = document.elementFromPoint(box.x + box.width / 2, box.y + box.height / 2)!;
        return target.closest('.attachment-trigger, .attachment-action')?.className ?? target.className;
      });
      expect(await hit('.attachment-title')).toContain('attachment-trigger');
      expect(await hit('.attachment-action >> nth=0')).toContain('attachment-action');
    });

    test('Tab reaches the actions, then the trigger, whose ring is around the card', async ({page}) => {
      const attachment = scenario(page, 'attachment--trigger').locator('.attachment');
      const actions = attachment.locator('.attachment-action');
      const trigger = attachment.locator('.attachment-trigger');
      const borderColor = () => attachment.evaluate(element => getComputedStyle(element).borderTopColor);
      const unfocused = await borderColor();

      await actions.first().focus();
      await page.keyboard.press('Tab');
      await expect(actions.nth(1)).toBeFocused();
      await page.keyboard.press('Tab');
      await expect(trigger).toBeFocused();

      expect(await trigger.evaluate(element => element.matches(':focus-visible'))).toBe(true);
      expect(await trigger.evaluate(element => getComputedStyle(element).boxShadow)).not.toBe('none');
      expect(await borderColor()).not.toBe(unfocused);
      // The ring follows the card's corners.
      expect(await trigger.evaluate(element => getComputedStyle(element).borderTopLeftRadius))
          .toBe(await attachment.evaluate(element => getComputedStyle(element).borderTopLeftRadius));
    });

    test('a labelled group shows its focus ring, which the edge fade does not hide', async ({page}) => {
      const group = scenario(page, 'attachment--group-label').locator('.attachment-group');
      const style = () => group.evaluate(element => {
        const style = getComputedStyle(element);
        return {mask: style.maskImage, boxShadow: style.boxShadow};
      });
      const rest = await style();
      expect(rest.mask).not.toBe('none');

      await page.keyboard.press('Tab');
      await group.focus();
      const focused = await style();
      expect(await group.evaluate(element => element.matches(':focus-visible'))).toBe(true);
      expect(focused.boxShadow).not.toBe('none');
      expect(focused.mask).toBe('none');
    });
  });
}

test.describe('behaviour', () => {
  test.beforeEach(async ({page}) => openShowcase(page, 'vega', 'light'));

  test('the group scrolls and snaps to an attachment', async ({page}) => {
    const group = scenario(page, 'attachment--group').locator('.attachment-group');
    expect(await group.evaluate(element => element.scrollWidth > element.clientWidth)).toBe(true);

    // Scroll part of the way to the second attachment: it snaps to the start of one.
    await group.evaluate(element => element.scrollBy({left: 60, behavior: 'instant'}));
    await expect.poll(() => group.evaluate(element => {
      const padding = parseFloat(getComputedStyle(element).scrollPaddingInlineStart) || 0;
      const starts = [...element.children].map(child =>
          (child as HTMLElement).getBoundingClientRect().left - element.getBoundingClientRect().left
          + element.scrollLeft - padding);
      return Math.min(...starts.map(start => Math.abs(start - element.scrollLeft)));
    })).toBeLessThan(1.5);
    expect(await group.evaluate(element => element.scrollLeft)).toBeGreaterThan(0);
  });

  test('Tab scrolls an attachment off screen into view through its action', async ({page}) => {
    const group = scenario(page, 'attachment--group').locator('.attachment-group');
    const last = group.locator('.attachment-action').last();
    await last.focus();
    await expect.poll(() => group.evaluate(element => element.scrollLeft)).toBeGreaterThan(0);
    await expect(last).toBeInViewport();
  });

  test('the arrow keys scroll a labelled group', async ({page}) => {
    const group = scenario(page, 'attachment--group-label').locator('.attachment-group');
    await expect(group).toHaveAttribute('role', 'group');
    await expect(group).toHaveAccessibleName('Reference photos');
    await group.focus();
    await page.keyboard.press('ArrowRight');
    await expect.poll(() => group.evaluate(element => element.scrollLeft)).toBeGreaterThan(0);
  });

  test('the trigger opens its dialog, an action does not', async ({page}) => {
    const attachment = scenario(page, 'attachment--trigger').locator('.attachment');
    const dialog = page.locator('#research-summary-preview');

    await attachment.getByRole('button', {name: 'Copy link to research-summary.pdf'}).click();
    await expect(dialog).not.toBeVisible();

    // The title lies under the trigger: a click on it is the trigger's (force skips Playwright's check for that).
    await attachment.locator('.attachment-title').click({force: true});
    await expect(dialog).toBeVisible();
    expect(await dialog.evaluate(element => element.matches(':modal'))).toBe(true);
  });

  test('a trigger link and a download action are links', async ({page}) => {
    await expect(scenario(page, 'attachment--image').getByRole('link', {name: 'Open dunes.png'}))
        .toHaveAttribute('href', '/files/dunes.png');
    await expect(scenario(page, 'attachment--download').getByRole('link', {name: 'Download sales-dashboard.pdf'}))
        .toHaveAttribute('download', '');
  });

  test('an uploading or processing attachment is busy and its title shimmers', async ({page}) => {
    // The suite runs with reduced motion (playwright.config.ts); the shimmer is for users who have not asked for it.
    await page.emulateMedia({reducedMotion: 'no-preference'});
    const states = scenario(page, 'attachment--states');
    for (const state of ['uploading', 'processing']) {
      const attachment = states.locator(`.attachment[data-state="${state}"]`);
      await expect(attachment).toHaveAttribute('aria-busy', 'true');
      expect(await attachment.locator('.attachment-title').evaluate(element => getComputedStyle(element).animationName))
          .toBe('sl-attachment-shimmer');
    }
    expect(await states.locator('.attachment[data-state="error"] .attachment-title')
        .evaluate(element => getComputedStyle(element).animationName)).toBe('none');
  });

  test('the title does not shimmer for users who ask for reduced motion', async ({page}) => {
    const title = scenario(page, 'attachment--states').locator('.attachment[data-state="uploading"] .attachment-title');
    const style = await title.evaluate(element => {
      const style = getComputedStyle(element);
      return {animation: style.animationName, fill: style.webkitTextFillColor, color: style.color};
    });
    expect(style.animation).toBe('none');
    expect(style.fill).toBe(style.color);
  });

  test('the error state is told in text, in a legible destructive colour', async ({page}) => {
    const states = scenario(page, 'attachment--states');
    const color = (state: string) => states.locator(`.attachment[data-state="${state}"] .attachment-description`)
        .evaluate(element => getComputedStyle(element).color);
    expect(await color('error')).not.toBe(await color('idle'));
    await expect(states.locator('.attachment[data-state="error"]')).not.toHaveAttribute('role', /.+/);
  });
});
