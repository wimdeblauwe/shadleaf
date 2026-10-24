/*
  preact/compat sets props on SVG elements with setAttribute and keeps their case. SVG attribute names are case
  sensitive, so Recharts' tabIndex={0} on the chart's <svg> became an attribute the browser ignores: the chart could
  not be focused, and its accessibility layer's arrow keys never reached it.
*/
import {options} from 'preact';

const previous = options.vnode;
options.vnode = vnode => {
  const props = vnode.props;
  if (typeof vnode.type === 'string' && props && 'tabIndex' in props) {
    props.tabindex = props.tabIndex;
    delete props.tabIndex;
  }
  previous?.(vnode);
};
