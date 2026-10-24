import {expect, type Page} from '@playwright/test';

// Keyboard focus must be visible on every control. axe does not check this, and "the skin has a :focus-visible rule"
// is not enough: a rule can exist and draw nothing (an outline width with outline-style none). So this tabs through the
// page and compares each control's computed outline and box-shadow with its unfocused state. Used by focus.spec.ts on
// the showcase and by layouts.spec.ts on the layouts.

type Indicator = { outline: string; boxShadow: string };

/**
 * Boxes that draw the focus of the control inside them, as shadcn's input group does: the combobox's control around
 * its input (and chips). The indicator is read from the box.
 */
const FOCUS_BOXES = '.combobox-control';

/**
 * Tabs from the focus's current place through every control in `scope` (a selector), in document order, and expects
 * each one to get a visible focus indicator. `allowNone` for a scope that may have no controls at all (a badge's).
 */
export async function expectVisibleFocus(page: Page, scope: string, {allowNone = false} = {}) {
  const tabbable = await page.evaluate(({scope, focusBoxes}) => {
    const candidates = document.querySelectorAll<HTMLElement>(
        `${scope} :is(a[href], button, input:not([type=hidden]), select, textarea, summary, [tabindex])`);
    // :disabled, not the disabled property: a control in a disabled fieldset is disabled without the attribute.
    const disabled = (element: Element) => element.matches(':disabled');
    // Tab stops once per radio group: on its checked radio, or on its first one when none is checked.
    const radioTabStop = (radio: HTMLInputElement) => {
      const group = [...document.querySelectorAll<HTMLInputElement>(`input[type=radio][name="${radio.name}"]`)]
          .filter(other => !disabled(other));
      return radio === (group.find(other => other.checked) ?? group[0]);
    };
    return [...candidates]
        .filter(element => element.tabIndex >= 0 && !disabled(element))
        // The button of a customizable select shows its value; the select takes the focus.
        .filter(element => !element.matches('select > button'))
        // Not in a closed dialog or popover: dialog.spec.ts and popup.spec.ts tab through those open. The sidebar is
        // a popover too, shown in the page from 768 px (its closed panel on a phone is not visible, below).
        .filter(element => !element.closest('dialog:not([open]), [popover]:not(:popover-open, .sidebar)'))
        // Not in a closed details element (accordion, collapsible) or an inactive tab panel.
        .filter(element => element.checkVisibility())
        .filter(element => !(element instanceof HTMLInputElement && element.type === 'radio') || radioTabStop(element))
        .map((element, index) => {
          element.dataset.focusIndex = String(index);
          const style = getComputedStyle(element.matches(`:is(${focusBoxes}) > input`)
              ? element.closest(focusBoxes)! : element);
          return {
            label: element.outerHTML.slice(0, 120),
            unfocused: {outline: `${style.outlineStyle} ${style.outlineWidth} ${style.outlineColor}`,
              boxShadow: style.boxShadow},
          };
        });
  }, {scope, focusBoxes: FOCUS_BOXES});
  if (!allowNone) {
    expect(tabbable.length).toBeGreaterThan(0);
  }

  const invisible: string[] = [];
  const readFocused = () => page.evaluate(focusBoxes => {
    const element = document.activeElement as HTMLElement;
    const style = getComputedStyle(element.matches(`:is(${focusBoxes}) > input`) ? element.closest(focusBoxes)! : element);
    return {
      index: element.dataset.focusIndex,
      label: element.outerHTML.slice(0, 120),
      scrolls: element.scrollHeight > element.clientHeight || element.scrollWidth > element.clientWidth,
      focusVisible: element.matches(':focus-visible'),
      outlineVisible: style.outlineStyle !== 'none' && parseFloat(style.outlineWidth) > 0,
      indicator: {outline: `${style.outlineStyle} ${style.outlineWidth} ${style.outlineColor}`,
        boxShadow: style.boxShadow} as Indicator,
    };
  }, FOCUS_BOXES);

  for (let i = 0; i < tabbable.length; i++) {
    await page.keyboard.press('Tab');
    let focused = await readFocused();
    // A date or time input has a tab stop per segment (day, month, year): tab on until focus leaves it.
    for (let extra = 0; extra < 5 && i > 0 && focused.index === String(i - 1); extra++) {
      await page.keyboard.press('Tab');
      focused = await readFocused();
    }
    // A container that scrolls can be a tab stop the browser adds (Firefox always, so the keyboard can scroll it): it
    // needs a visible focus too. Then tab on.
    for (let extra = 0; extra < 3 && focused.index === undefined && focused.scrolls; extra++) {
      if (!focused.focusVisible || !(focused.outlineVisible || focused.indicator.boxShadow !== 'none')) {
        invisible.push(`${focused.label} (scroll container)\n    focused   ${JSON.stringify(focused.indicator)}`);
      }
      await page.keyboard.press('Tab');
      focused = await readFocused();
    }
    expect(focused.index, `Tab reaches control ${i}`).toBe(String(i));
    expect(focused.focusVisible).toBe(true);

    const before = tabbable[i].unfocused;
    const changed = focused.indicator.outline !== before.outline || focused.indicator.boxShadow !== before.boxShadow;
    const drawn = focused.outlineVisible || focused.indicator.boxShadow !== 'none';
    if (!changed || !drawn) {
      invisible.push(`${tabbable[i].label}\n    unfocused ${JSON.stringify(before)}\n    focused   ${
          JSON.stringify(focused.indicator)}`);
    }
  }
  expect(invisible, `no visible focus indicator on:\n${invisible.join('\n')}`).toEqual([]);
}
