// A component's styling hooks (styling-hooks.json) in the three groups the docs show them in, and each group as an
// empty stylesheet to copy: shared by StylingHooks.astro and the Markdown output (markdown.ts).
import type {Component, StylingHooks} from './generated';

export type StylingHooksGroups = {
  /** The classes, the root class first, with the elements they are on. */
  parts: { name: string; elements: string[] }[];
  /** The attributes that reflect a prop; absent for the prop's default value. */
  props: { prop: string; selector: string; defaultSelector: string; values: string[]; defaultValue: string;
    hook: string; attribute: string }[];
  /** The attributes the component or its script sets; no values when only their presence matters. */
  states: { selector: string; values: string[]; hook: string; attribute: string }[];
};

export function stylingHooksGroups(hooks: StylingHooks, component: Component): StylingHooksGroups {
  const defaultOf = (prop: string) => component.props.find(p => p.name === prop)?.defaultValue ?? 'default';
  return {
    parts: hooks.classes.map(hook => ({name: hook.name, elements: hook.elements})),
    props: hooks.classes.flatMap(hook => hook.attributes.filter(a => a.prop).map(a => ({
      prop: a.prop!,
      selector: `.${hook.name}[${a.name}="…"]`,
      defaultSelector: `.${hook.name}:not([${a.name}])`,
      values: a.values,
      defaultValue: defaultOf(a.prop!),
      hook: hook.name,
      attribute: a.name,
    }))),
    states: hooks.classes.flatMap(hook => hook.attributes.filter(a => !a.prop).map(a => ({
      selector: a.values.length === 0 ? `.${hook.name}[${a.name}]` : `.${hook.name}[${a.name}="…"]`,
      values: a.values,
      hook: hook.name,
      attribute: a.name,
    }))),
  };
}

/** A group as an empty stylesheet: a rule per selector, every value spelled out. */
export function stylingHooksCss(groups: StylingHooksGroups, kind: 'parts' | 'props' | 'states'): string {
  const rule = (selector: string) => `${selector} {\n}`;
  if (kind === 'parts') {
    return groups.parts.map(part => rule(`.${part.name}`)).join('\n\n');
  }
  if (kind === 'props') {
    return groups.props.map(prop => [
      `/* ${prop.prop}="${prop.defaultValue}", the default: no attribute */\n` + rule(prop.defaultSelector),
      ...prop.values.map(value => rule(`.${prop.hook}[${prop.attribute}='${value}']`)),
    ].join('\n\n')).join('\n\n');
  }
  return groups.states.map(state => state.values.length === 0
      ? rule(state.selector)
      : state.values.map(value => rule(`.${state.hook}[${state.attribute}='${value}']`)).join('\n\n'))
      .join('\n\n');
}
