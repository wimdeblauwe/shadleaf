(function () {
  var root = document.documentElement;
  try {
    var theme = localStorage.getItem('shadleaf-theme');
    var dark = theme === 'dark' || (theme !== 'light' && window.matchMedia('(prefers-color-scheme: dark)').matches);
    root.classList.toggle('dark', dark);
  } catch (e) {
  }
  // Paper is light: a dark page prints in the light theme, and is dark again afterwards.
  var darkBeforePrint = false;
  window.addEventListener('beforeprint', function () {
    darkBeforePrint = root.classList.contains('dark');
    root.classList.remove('dark');
  });
  window.addEventListener('afterprint', function () {
    if (darkBeforePrint) {
      root.classList.add('dark');
    }
  });
})();
