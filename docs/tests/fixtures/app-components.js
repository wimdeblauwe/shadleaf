// The test-only Alpine component, registered the way the Alpine.js guide tells an application to: on alpine:init, in
// a script that runs before the Shadleaf script. Its markup (alpine.spec.ts) only uses expressions the CSP build
// accepts, and the component counts its initialisations so a second Alpine would show up.
document.addEventListener('alpine:init', () => {
  window.Alpine.data('testDisclosure', () => ({
    open: false,
    init() {
      window.testDisclosureInits = (window.testDisclosureInits ?? 0) + 1;
    },
    toggle() {
      this.open = !this.open;
    },
    get expanded() {
      return this.open ? 'true' : 'false';
    }
  }));
});
