/*
  sl:validate: a validation request sends the form's values, not its files. htmx 4 posts the whole form of a control
  inside one (new FormData(form)), so no hx-include can leave a file input out, and every keystroke would upload its
  files again. htmx:config:request comes after htmx built the body and set the hx-headers, before it encodes the body:
  a request that names its field in SL-Validate-Field gets the body without the files. A multipart form still posts
  multipart, without them; another form posts its values URL-encoded, as htmx would have, without a file's name as
  "[object File]".

  The response morphs the field (outerMorph), which would also take away what a script added to the server's markup:
  the input and the list box of a sl:combobox, the months of a sl:calendar, the button text and names of a
  sl:date-picker, and the attributes they set. Such a component marks itself data-enhanced. In htmx:before:swap, each
  one in the response is replaced by a copy of the one on the page, given the field's state from the response:
  aria-invalid and aria-describedby on its elements with an id (the controls th:field binds). The morph then changes
  only those, and the component follows them as it does any change of its control's attributes. Such a component
  names its parts after the field's label (aria-labelledby), giving the label an id when it has none: a label in the
  response gets the id its counterpart on the page has.

  Not an Alpine component: one listener on the document covers every form, and the entries call this whatever
  shadleaf.assets.alpine is. htmx 2 fires no such event; sl:validate needs htmx 4 (outerMorph, hx-partial) anyway.
*/
const HEADER = 'sl-validate-field';
const FIELD_STATE = ['aria-invalid', 'aria-describedby'];

function isValidation(headers) {
  return Object.keys(headers ?? {}).some(name => name.toLowerCase() === HEADER);
}

/** The body without its files, or the body itself when it holds none. */
export function withoutFiles(body) {
  if (!(body instanceof FormData)) {
    return body;
  }
  const entries = [...body.entries()];
  if (!entries.some(([, value]) => value instanceof File)) {
    return body;
  }
  const values = new FormData();
  entries.filter(([, value]) => !(value instanceof File)).forEach(([name, value]) => values.append(name, value));
  return values;
}

/** In the response for `target`, each enhanced component of the page, with the field's state from the response. */
export function keepEnhanced(target, fragment) {
  if (!(target instanceof Element) || !fragment?.querySelectorAll) {
    return;
  }
  // The outermost ones: a sl:date-picker's copy holds its sl:calendar.
  const enhanced = [...target.querySelectorAll('[data-enhanced]')]
      .filter(element => !target.contains(element.parentElement.closest('[data-enhanced]')));
  for (const current of enhanced) {
    const ids = [...current.querySelectorAll('[id]')].map(element => element.id);
    const counterpart = ids.map(id => fragment.querySelector(`[id="${CSS.escape(id)}"]`)).find(Boolean)
        ?.closest(`.${CSS.escape(current.classList[0])}`);
    if (!counterpart) {
      continue;
    }
    const copy = current.cloneNode(true);
    for (const element of counterpart.querySelectorAll('[id]')) {
      const kept = copy.querySelector(`[id="${CSS.escape(element.id)}"]`);
      for (const name of kept ? FIELD_STATE : []) {
        if (element.hasAttribute(name)) {
          kept.setAttribute(name, element.getAttribute(name));
        } else {
          kept.removeAttribute(name);
        }
      }
    }
    counterpart.replaceWith(copy);
  }
  for (const label of target.querySelectorAll('label[id][for]')) {
    fragment.querySelector(`label[for="${CSS.escape(label.htmlFor)}"]:not([id])`)?.setAttribute('id', label.id);
  }
}

export function watchValidationRequests() {
  if (document.slValidationRequests) {
    return;
  }
  document.slValidationRequests = true;
  document.addEventListener('htmx:config:request', event => {
    const request = event.detail?.ctx?.request;
    if (request && isValidation(request.headers)) {
      request.body = withoutFiles(request.body);
    }
  });
  document.addEventListener('htmx:before:swap', event => {
    const {ctx, tasks} = event.detail ?? {};
    if (ctx?.request && isValidation(ctx.request.headers)) {
      (tasks ?? []).forEach(task => keepEnhanced(task.target, task.fragment));
    }
  });
}
