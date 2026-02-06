document.addEventListener('DOMContentLoaded', () => {
  const highlighter = window.Cn1JavaDocHighlight;
  if (!highlighter) {
    return;
  }

  const shouldHighlight = (className) =>
    className === '' || className.includes('language-') || className.includes('lang-');

  document.querySelectorAll('pre').forEach((pre) => {
    const code = pre.querySelector('code');
    const target = code || pre;
    if (shouldHighlight(target.className || '')) {
      highlighter.highlightElement(target);
    }
  });

  document.querySelectorAll('code').forEach((code) => {
    if (code.closest('pre')) {
      return;
    }
    if (shouldHighlight(code.className || '')) {
      highlighter.highlightElement(code);
    }
  });
});
