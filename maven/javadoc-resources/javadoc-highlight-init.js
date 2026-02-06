document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('pre code').forEach((block) => {
    const className = block.className || '';
    const shouldHighlight =
      className === '' ||
      className.includes('language-') ||
      className.includes('lang-');

    if (shouldHighlight && window.Cn1JavaDocHighlight) {
      window.Cn1JavaDocHighlight.highlightElement(block);
    }
  });
});
