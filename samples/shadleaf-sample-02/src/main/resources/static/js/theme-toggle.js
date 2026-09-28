// The sample's own theme toggle; the library ships only the pre-paint script (sl/layout :: theme-script),
// which reads the same localStorage key.
const STORAGE_KEY = 'shadleaf-theme';

function apply(choice) {
  const dark = choice === 'dark' || (choice !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches);
  document.documentElement.classList.toggle('dark', dark);
  document.querySelectorAll('[data-theme-choice]').forEach(button => {
    button.setAttribute('aria-pressed', String(button.dataset.themeChoice === choice));
  });
}

document.addEventListener('click', event => {
  const button = event.target.closest('[data-theme-choice]');
  if (!button) {
    return;
  }
  const choice = button.dataset.themeChoice;
  if (choice === 'system') {
    localStorage.removeItem(STORAGE_KEY);
  } else {
    localStorage.setItem(STORAGE_KEY, choice);
  }
  apply(choice);
});

apply(localStorage.getItem(STORAGE_KEY) ?? 'system');
