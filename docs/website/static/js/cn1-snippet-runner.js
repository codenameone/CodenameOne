// "Try it" for every Java snippet on a page: the developer guide, the API
// reference and the blog. Clicking it swaps the static block for the Playground
// in inline mode -- an editor holding the snippet and a live preview -- so the
// reader can change the code and watch it run where they read it. The code is
// compiled and run in the browser (scripts/cn1playground); nothing is sent to a
// server, and nothing of the Playground loads until the first click.
//
// One frame serves the whole page. Opening a second snippet closes the first and
// hands the new code to the running frame instead of booting another VM. A frame
// cannot be moved in the DOM without reloading it, so it lives in a layer of its
// own at the end of <body> and is positioned over a placeholder that takes the
// snippet's place in the text.
(function () {
  'use strict';

  if (window.Cn1SnippetRunner) {
    return;
  }

  var PLAYGROUND_BASE = window.CN1_PLAYGROUND_BASE || '/playground-app/';
  var FRAME_HEIGHT = 'min(75vh, 620px)';

  var frame = null;
  var frameReady = false;
  var frameCode = null; // the snippet the frame was last given
  var active = null; // { pre, slot, code, status, openLink }

  function encode(code) {
    var bytes = new TextEncoder().encode(code);
    var binary = '';
    for (var i = 0; i < bytes.length; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  function playgroundUrl(code, inline) {
    return PLAYGROUND_BASE + '?' + (inline ? 'mode=inline&' : '') + 'code=' + encode(code);
  }

  // The text a reader sees, without the decorations renderers put inside the
  // block: asciidoctor's callout markers and line-number gutters.
  function snippetText(codeEl) {
    var clone = codeEl.cloneNode(true);
    var decorations = clone.querySelectorAll('.conum, .ln, .lnt, .hl-line-number');
    for (var i = 0; i < decorations.length; i++) {
      decorations[i].remove();
    }
    return (clone.textContent || '').replace(/\s+$/, '');
  }

  function injectStyles() {
    if (document.getElementById('cn1-snippet-runner-styles')) {
      return;
    }
    var style = document.createElement('style');
    style.id = 'cn1-snippet-runner-styles';
    style.textContent = [
      '.cn1-try-bar{display:flex;gap:12px;align-items:center;margin:6px 0 14px 0;font-size:0.9em}',
      '.cn1-try-bar button{cursor:pointer;border:1px solid currentColor;background:transparent;color:inherit;',
      'border-radius:6px;padding:3px 12px;font:inherit}',
      '.cn1-try-bar button:hover{opacity:0.8}',
      '.cn1-try-bar .cn1-try-status{opacity:0.75;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0}',
      '.cn1-try-slot{width:100%;height:' + FRAME_HEIGHT + ';border-radius:8px;background:rgba(127,127,127,0.08)}',
      '.cn1-try-frame{position:absolute;border:0;border-radius:8px;z-index:5;background:transparent}'
    ].join('');
    document.head.appendChild(style);
  }

  function ensureFrame(code) {
    if (frame) {
      return false;
    }
    frame = document.createElement('iframe');
    frame.className = 'cn1-try-frame';
    frame.title = 'Codename One Playground';
    frame.setAttribute('allow', 'clipboard-write');
    frame.src = playgroundUrl(code, true);
    frameCode = code;
    document.body.appendChild(frame);
    return true;
  }

  function placeFrame() {
    if (!frame || !active) {
      return;
    }
    var r = active.slot.getBoundingClientRect();
    frame.style.left = (r.left + window.scrollX) + 'px';
    frame.style.top = (r.top + window.scrollY) + 'px';
    frame.style.width = r.width + 'px';
    frame.style.height = r.height + 'px';
  }

  function setStatus(text) {
    if (active) {
      active.status.textContent = text;
    }
  }

  function close() {
    if (!active) {
      return;
    }
    active.slot.remove();
    active.pre.style.display = '';
    active.bar.closeButton.hidden = true;
    active.bar.tryButton.hidden = false;
    setStatus('');
    active = null;
    if (frame) {
      // Kept loaded for the next snippet, just out of sight.
      frame.style.top = '-10000px';
    }
  }

  function open(pre, bar, code) {
    if (active && active.pre === pre) {
      return;
    }
    close();
    var slot = document.createElement('div');
    slot.className = 'cn1-try-slot';
    pre.style.display = 'none';
    pre.insertAdjacentElement('afterend', slot);
    bar.tryButton.hidden = true;
    bar.closeButton.hidden = false;
    active = { pre: pre, slot: slot, code: code, status: bar.status, bar: bar, sourceUrl: '' };

    if (!ensureFrame(code) && frameReady) {
      // The frame is already up: hand it the new snippet.
      sendCode(code);
    }
    setStatus(frameReady ? 'Compiling...' : 'Loading the Playground...');
    placeFrame();
    slot.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }

  function sendCode(code) {
    frameCode = code;
    frame.contentWindow.postMessage({ type: 'cn1-playground-inline-code', code: code }, '*');
  }

  function onMessage(evt) {
    if (!frame || evt.source !== frame.contentWindow) {
      return;
    }
    var data = evt.data || {};
    if (data.type === 'cn1-playground-ui-ready') {
      frameReady = true;
      setStatus('Compiling...');
      // A snippet opened while the frame was still booting.
      if (active && active.code !== frameCode) {
        sendCode(active.code);
      }
    } else if (data.type === 'cn1-playground-inline-result') {
      frameReady = true;
      if (active) {
        active.sourceUrl = data.sourceUrl || '';
      }
      setStatus(data.ok ? 'Running. Edit the code to change it.' : 'Does not compile: ' + data.summary);
    }
  }

  function attach(pre, codeText) {
    if (!pre || pre.hasAttribute('data-cn1-try')) {
      return;
    }
    // Server code: the guide marks it with this role (asciidoctor puts it on the
    // block's wrapper), and the backend API reference is server code throughout.
    if (pre.closest('.cn1-no-try') || /^\/backend\//.test(window.location.pathname)) {
      return;
    }
    var codeEl = pre.querySelector('code') || pre;
    var code = typeof codeText === 'string' ? codeText : snippetText(codeEl);
    if (!code.trim()) {
      return;
    }
    pre.setAttribute('data-cn1-try', '');
    injectStyles();

    var bar = document.createElement('div');
    bar.className = 'cn1-try-bar';

    var tryButton = document.createElement('button');
    tryButton.type = 'button';
    tryButton.textContent = 'Try it';
    tryButton.title = 'Edit and run this code here';

    var closeButton = document.createElement('button');
    closeButton.type = 'button';
    closeButton.textContent = 'Close';
    closeButton.hidden = true;

    var openLink = document.createElement('a');
    openLink.textContent = 'Open in Playground';
    openLink.target = '_blank';
    openLink.rel = 'noopener';
    openLink.href = playgroundUrl(code, false);
    // While this snippet is open, the link carries the reader's edits.
    openLink.addEventListener('click', function () {
      if (active && active.bar === bar && active.sourceUrl) {
        openLink.href = active.sourceUrl;
      }
    });

    var status = document.createElement('span');
    status.className = 'cn1-try-status';
    status.setAttribute('aria-live', 'polite');

    bar.tryButton = tryButton;
    bar.closeButton = closeButton;
    bar.status = status;
    tryButton.addEventListener('click', function () { open(pre, bar, code); });
    closeButton.addEventListener('click', close);

    bar.appendChild(tryButton);
    bar.appendChild(closeButton);
    bar.appendChild(openLink);
    bar.appendChild(status);
    pre.insertAdjacentElement('afterend', bar);
  }

  function isJava(codeEl) {
    var cls = codeEl.className || '';
    return /(^|\s)(language-java|lang-java)(\s|$)/.test(cls) || codeEl.getAttribute('data-lang') === 'java';
  }

  function scan(root) {
    var blocks = (root || document).querySelectorAll('pre > code');
    for (var i = 0; i < blocks.length; i++) {
      if (isJava(blocks[i])) {
        attach(blocks[i].parentElement);
      }
    }
  }

  window.Cn1SnippetRunner = { attach: attach, scan: scan, close: close };

  window.addEventListener('message', onMessage);
  window.addEventListener('resize', placeFrame);
  if (typeof ResizeObserver !== 'undefined') {
    // Text above the snippet reflowing (images loading, a section expanding)
    // moves the slot without a window resize.
    new ResizeObserver(placeFrame).observe(document.documentElement);
  }
  document.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && active && document.activeElement !== frame) {
      close();
    }
  });

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', function () { scan(document); });
  } else {
    scan(document);
  }
})();
