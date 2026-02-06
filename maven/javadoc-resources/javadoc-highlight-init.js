document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('pre code').forEach((block) => {
    const className = block.className || '';
    if (className.includes('language-java') || className.includes('lang-java')) {
      if (window.Cn1JavaDocHighlight) {
        window.Cn1JavaDocHighlight.highlightElement(block);
      }
    }
  });
});
