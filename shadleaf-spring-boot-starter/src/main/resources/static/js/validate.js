/*
  sl:validate: a validation request sends the form's values, not its files. htmx 4 posts the whole form of a control
  inside one (new FormData(form)), so no hx-include can leave a file input out, and every keystroke would upload its
  files again. htmx:config:request comes after htmx built the body and set the hx-headers, before it encodes the body:
  a request that names its field in SL-Validate-Field gets the body without the files. A multipart form still posts
  multipart, without them; another form posts its values URL-encoded, as htmx would have, without a file's name as
  "[object File]".
  Not an Alpine component: one listener on the document covers every form, and the entries call this whatever
  shadleaf.assets.alpine is. htmx 2 fires no such event; sl:validate needs htmx 4 (outerMorph, hx-partial) anyway.
*/
const HEADER = 'sl-validate-field';

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
}
