document.addEventListener('DOMContentLoaded', () => {
  const highlighter = window.Cn1JavaDocHighlight;
  const playgroundBase = 'https://www.codenameone.com/playground-app/?code=';
  if (!highlighter) {
    return;
  }

  const shouldHighlight = (className) =>
    className === '' || className.includes('language-') || className.includes('lang-');

  const isJavaSnippet = (className) => {
    if (!className) {
      return false;
    }
    return className.includes('language-java') || className.includes('lang-java');
  };

  const addPlaygroundLink = (pre, codeText) => {
    if (!codeText || pre.nextElementSibling?.classList.contains('cn1-open-playground-link')) {
      return;
    }

    const linkContainer = document.createElement('div');
    linkContainer.className = 'cn1-open-playground-link';
    linkContainer.style.margin = '6px 0 14px 0';

    const link = document.createElement('a');
    link.href = `${playgroundBase}${encodeURIComponent(codeText)}`;
    link.target = '_blank';
    link.rel = 'noopener noreferrer';
    link.textContent = 'Open in playground';

    linkContainer.appendChild(link);
    pre.insertAdjacentElement('afterend', linkContainer);
  };

  document.querySelectorAll('pre').forEach((pre) => {
    const code = pre.querySelector('code');
    const target = code || pre;
    const className = target.className || '';
    if (shouldHighlight(className)) {
      highlighter.highlightElement(target);
    }
    if (isJavaSnippet(className)) {
      addPlaygroundLink(pre, target.textContent || '');
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
