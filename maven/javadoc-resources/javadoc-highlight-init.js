/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

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

  const encodePlaygroundCode = (codeText) => {
    try {
      if (typeof TextEncoder !== 'undefined') {
        const bytes = new TextEncoder().encode(codeText);
        let binary = '';
        bytes.forEach((byte) => {
          binary += String.fromCharCode(byte);
        });
        return btoa(binary);
      }
    } catch (e) {
      // Fall through to the URI-encoding based fallback below.
    }
    return btoa(unescape(encodeURIComponent(codeText)));
  };

  const addPlaygroundLink = (pre, codeText) => {
    if (!codeText || pre.nextElementSibling?.classList.contains('cn1-open-playground-link')) {
      return;
    }

    const linkContainer = document.createElement('div');
    linkContainer.className = 'cn1-open-playground-link';
    linkContainer.style.margin = '6px 0 14px 0';

    const link = document.createElement('a');
    link.href = `${playgroundBase}${encodePlaygroundCode(codeText)}`;
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
      // On codenameone.com the page also loads cn1-snippet-runner.js, which
      // edits and runs the snippet in place. The downloadable javadocs.zip has
      // no Playground beside it, so it keeps the link to the hosted one.
      if (window.Cn1SnippetRunner) {
        window.Cn1SnippetRunner.attach(pre, target.textContent || '');
      } else {
        addPlaygroundLink(pre, target.textContent || '');
      }
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
