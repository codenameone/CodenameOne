// The storefront's floating chat. The page is public and cached, so it
// carries only the store and the interface text; everything about this
// visitor comes from POST /s/{handle}/chat/session (a single-use ticket and
// the conversation's token) and then the /ws/chat socket. The token lives in
// localStorage, so the conversation follows the visitor from page to page,
// and a signed-in customer's follows the account (the server links it).
(function () {
  'use strict';
  var root = document.getElementById('chat');
  if (!root || !window.WebSocket || !window.fetch) { return; }
  var store = root.getAttribute('data-store');
  var T = function (k) { return root.getAttribute('data-t-' + k) || ''; };
  var KEY = 'bazaar.chat.' + store;
  var bubble = root.querySelector('.chat-bubble');
  var panel = root.querySelector('.chat-panel');
  var log = root.querySelector('.chat-log');
  var form = root.querySelector('.chat-form');
  var input = form.querySelector('textarea');
  var typing = root.querySelector('.chat-typing');
  var statusT = root.querySelector('.chat-status-t');
  var badge = root.querySelector('.chat-badge');
  var emailForm = root.querySelector('.chat-email');
  var voice = root.querySelector('.chat-voice');
  var video = root.querySelector('.chat-video');
  var callBox = root.querySelector('.chat-call');
  var callT = root.querySelector('.chat-call-t');
  var avatar = root.querySelector('.chat-avatar');
  var title = root.querySelector('#chat-title');
  var lang = document.documentElement.lang || undefined;

  var ws = null;
  var ice = [];
  var online = 0;
  var started = false;
  var hasEmail = false;
  var unread = 0;
  var refs = 0;
  var lastTyping = 0;
  var typingTimer = 0;
  var retry = 1000;
  var call = null;
  var connecting = false;
  // Messages written before the socket is up (or while it reconnects); sent
  // in order on the next hello.
  var outbox = [];

  function get(k) { try { return window.localStorage.getItem(KEY + k); } catch (e) { return null; } }
  function put(k, v) { try { window.localStorage.setItem(KEY + k, v); } catch (e) { /* private mode */ } }
  function sget(k) { try { return window.sessionStorage.getItem(KEY + k); } catch (e) { return null; } }
  function sput(k, v) { try { window.sessionStorage.setItem(KEY + k, v); } catch (e) { /* private mode */ } }

  avatar.textContent = (title.textContent || '?').trim().charAt(0).toUpperCase();
  root.hidden = false;

  // ------------------------------------------------------------ rendering
  var SVG = 'http://www.w3.org/2000/svg';
  function icon(d) {
    var s = document.createElementNS(SVG, 'svg');
    s.setAttribute('viewBox', '0 0 24 24');
    s.setAttribute('aria-hidden', 'true');
    var p = document.createElementNS(SVG, 'path');
    p.setAttribute('d', d);
    s.appendChild(p);
    return s;
  }
  function time(at) {
    var d = new Date(at);
    var t = document.createElement('time');
    t.dateTime = d.toISOString();
    try {
      t.textContent = d.toLocaleTimeString(lang, { hour: 'numeric', minute: '2-digit' });
    } catch (e) {
      t.textContent = d.toLocaleTimeString();
    }
    return t;
  }
  function tick(li, state) {
    var old = li.querySelector('.chat-tick');
    if (old) { old.parentNode.removeChild(old); }
    if (!state) { return; }
    var s = document.createElement('span');
    s.className = 'chat-tick ' + state;
    s.title = T(state === 'read' ? 'read' : 'delivered');
    s.appendChild(icon(state === 'read' ? 'M2 12.5l4 4L15 7.5M9.5 16.5L19 7.5' : 'M5 12.5l4.5 4.5L19 7.5'));
    var label = document.createElement('span');
    label.className = 'sr';
    label.textContent = s.title;
    s.appendChild(label);
    li.querySelector('.chat-meta').appendChild(s);
  }
  function line(m) {
    var li = document.createElement('li');
    li.className = 'chat-msg ' + (m.staff ? 'them' : 'me');
    if (m.id) { li.setAttribute('data-id', m.id); }
    li.setAttribute('data-at', String(m.at || 0));
    var p = document.createElement('p');
    p.textContent = m.body;
    li.appendChild(p);
    var meta = document.createElement('span');
    meta.className = 'chat-meta';
    if (m.staff && m.author) {
      var who = document.createElement('span');
      who.className = 'sr';
      who.textContent = m.author + ' ';
      meta.appendChild(who);
    }
    meta.appendChild(time(m.at || Date.now()));
    li.appendChild(meta);
    log.appendChild(li);
    return li;
  }
  function scroll() { log.scrollTop = log.scrollHeight; }
  function markRead(at) {
    var mine = log.querySelectorAll('.chat-msg.me[data-id]');
    for (var i = 0; i < mine.length; i++) {
      tick(mine[i], Number(mine[i].getAttribute('data-at')) <= at ? 'read' : 'delivered');
    }
  }
  function setBadge() {
    badge.hidden = unread === 0;
    badge.textContent = unread > 9 ? '9+' : String(unread);
    bubble.setAttribute('aria-label', bubble.getAttribute('data-label')
        + (unread ? ' (' + unread + ' ' + T('unread') + ')' : ''));
  }
  bubble.setAttribute('data-label', bubble.getAttribute('aria-label'));
  function setOnline(n) {
    online = n;
    root.classList.toggle('is-online', n > 0);
    statusT.textContent = n > 0 ? T('online') : T('away');
    var canCall = n > 0 && started && window.BazaarCall && window.BazaarCall.supported();
    voice.hidden = !canCall;
    video.hidden = !canCall;
  }
  function showEmail() { emailForm.hidden = !started || hasEmail; }
  function note(text) {
    var li = document.createElement('li');
    li.className = 'chat-note';
    li.textContent = text;
    log.appendChild(li);
    scroll();
  }

  // ------------------------------------------------------------ the panel
  function isOpen() { return !panel.hidden; }
  function seenNow() {
    var last = 0;
    var them = log.querySelectorAll('.chat-msg.them[data-id]');
    if (them.length) { last = Number(them[them.length - 1].getAttribute('data-at')); }
    unread = 0;
    setBadge();
    if (last > Number(get('.seen') || 0)) {
      put('.seen', String(last));
      send({ op: 'seen', at: last });
    }
  }
  function open(focus) {
    panel.hidden = false;
    root.classList.add('is-open');
    bubble.setAttribute('aria-expanded', 'true');
    sput('.open', '1');
    connect();
    scroll();
    seenNow();
    if (focus) { input.focus(); }
  }
  function close() {
    panel.hidden = true;
    root.classList.remove('is-open');
    bubble.setAttribute('aria-expanded', 'false');
    sput('.open', '');
  }
  bubble.addEventListener('click', function () { if (isOpen()) { close(); } else { open(true); } });
  root.querySelector('.chat-x').addEventListener('click', function () { close(); bubble.focus(); });
  panel.addEventListener('keydown', function (e) {
    if (e.key === 'Escape' && !call) { close(); bubble.focus(); }
  });

  // ------------------------------------------------------------ the socket
  function send(f) {
    if (ws && ws.readyState === 1) { ws.send(JSON.stringify(f)); return true; }
    return false;
  }
  function connect() {
    if (connecting || (ws && ws.readyState <= 1)) { return; }
    connecting = true;
    var body = new URLSearchParams();
    body.set('token', get('') || '');
    fetch('/s/' + encodeURIComponent(store) + '/chat/session', { method: 'POST', body: body,
        credentials: 'same-origin', cache: 'no-store' })
      .then(function (r) { if (!r.ok) { throw new Error('session ' + r.status); } return r.json(); })
      .then(function (s) {
        if (s.token) { put('', s.token); }
        var url = (location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host
            + '/ws/chat?ticket=' + encodeURIComponent(s.ticket);
        ws = new WebSocket(url);
        ws.onmessage = function (e) { frame(JSON.parse(e.data)); };
        ws.onopen = function () { connecting = false; retry = 1000; root.classList.remove('is-down'); };
        ws.onclose = function () {
          connecting = false;
          ws = null;
          root.classList.add('is-down');
          if (call) { endCall(T('failed')); }
          setTimeout(connect, retry);
          retry = Math.min(retry * 2, 30000);
        };
      })
      .catch(function () {
        connecting = false;
        setTimeout(connect, retry);
        retry = Math.min(retry * 2, 30000);
      });
  }
  setInterval(function () { send({ op: 'ping' }); }, 25000);

  function frame(f) {
    switch (f.type) {
      case 'hello': {
        ice = f.ice || [];
        var old = log.querySelectorAll('.chat-msg[data-id], .chat-note');
        for (var i = 0; i < old.length; i++) { old[i].parentNode.removeChild(old[i]); }
        var msgs = f.messages || [];
        started = msgs.length > 0;
        hasEmail = !!f.email;
        var seen = Number(get('.seen') || 0);
        unread = 0;
        for (var j = 0; j < msgs.length; j++) {
          line(msgs[j]);
          if (msgs[j].staff && msgs[j].at > seen) { unread++; }
        }
        markRead(f.staffRead || 0);
        setOnline(f.online || 0);
        showEmail();
        if (isOpen()) { seenNow(); } else { setBadge(); }
        var pending = log.querySelectorAll('.chat-msg[data-ref]');
        for (var q = 0; q < pending.length; q++) { log.appendChild(pending[q]); }
        while (outbox.length) { send(outbox.shift()); }
        scroll();
        break;
      }
      case 'token':
        put('', f.token);
        started = true;
        setOnline(online);
        showEmail();
        break;
      case 'sent': {
        var li = log.querySelector('.chat-msg[data-ref="' + f.ref + '"]');
        if (li) {
          li.removeAttribute('data-ref');
          li.setAttribute('data-id', f.id);
          li.setAttribute('data-at', String(f.at));
          li.classList.remove('is-sending');
          tick(li, 'delivered');
        }
        break;
      }
      case 'message':
        typing.hidden = true;
        if (!f.staff) { break; }
        line(f);
        scroll();
        if (isOpen() && !document.hidden) { seenNow(); } else { unread++; setBadge(); }
        break;
      case 'seen':
        if (f.staff) { markRead(f.at); }
        break;
      case 'typing':
        if (!f.staff) { break; }
        typing.querySelector('.chat-typing-t').textContent = (title.textContent || '').trim()
            + ' ' + T('typing');
        typing.hidden = false;
        clearTimeout(typingTimer);
        typingTimer = setTimeout(function () { typing.hidden = true; }, 5000);
        break;
      case 'presence':
        setOnline(f.online || 0);
        break;
      case 'email':
        hasEmail = true;
        showEmail();
        note(T('saved'));
        break;
      case 'error': {
        var bad = f.ref && log.querySelector('.chat-msg[data-ref="' + f.ref + '"]');
        if (bad) { bad.classList.add('is-failed'); bad.classList.remove('is-sending'); }
        note(f.reason || T('error'));
        break;
      }
      case 'call':
        callFrame(f);
        break;
      default:
        break;
    }
  }
  document.addEventListener('visibilitychange', function () {
    if (!document.hidden && isOpen()) { seenNow(); }
  });

  // ------------------------------------------------------------ writing
  function grow() {
    input.style.height = 'auto';
    input.style.height = Math.min(input.scrollHeight, 120) + 'px';
  }
  input.addEventListener('input', function () {
    grow();
    var now = Date.now();
    if (started && now - lastTyping > 3000) {
      lastTyping = now;
      send({ op: 'typing' });
    }
  });
  input.addEventListener('keydown', function (e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
      e.preventDefault();
      form.requestSubmit ? form.requestSubmit() : form.dispatchEvent(new Event('submit'));
    }
  });
  form.addEventListener('submit', function (e) {
    e.preventDefault();
    var text = input.value.trim();
    if (!text) { return; }
    var ref = 'r' + (++refs);
    var li = line({ staff: false, body: text, at: Date.now() });
    li.setAttribute('data-ref', ref);
    li.classList.add('is-sending');
    var f = { op: 'send', body: text, ref: ref };
    if (!send(f)) {
      outbox.push(f);
      connect();
    }
    input.value = '';
    grow();
    scroll();
  });
  emailForm.addEventListener('submit', function (e) {
    e.preventDefault();
    send({ op: 'email', email: emailForm.querySelector('input').value });
  });

  // ------------------------------------------------------------ calls
  function callStatus(text) { callT.textContent = text; }
  function endCall(text) {
    if (call) { call.close(); }
    call = null;
    callBox.hidden = true;
    root.classList.remove('is-calling', 'is-video');
    if (text) { note(text); }
  }
  function startCall(withVideo) {
    if (call || !window.BazaarCall) { return; }
    if (online === 0) { note(T('unavailable')); return; }
    callBox.hidden = false;
    root.classList.add('is-calling');
    root.classList.toggle('is-video', withVideo);
    callStatus(T('calling'));
    call = new window.BazaarCall({
      ice: ice,
      local: callBox.querySelector('.chat-local'),
      remote: callBox.querySelector('.chat-remote'),
      signal: function (kind, extra) {
        var f = { op: 'call', kind: kind };
        for (var k in extra) { if (Object.prototype.hasOwnProperty.call(extra, k)) { f[k] = extra[k]; } }
        send(f);
      },
      state: function (s) {
        if (s === 'connected') { callStatus(T('live')); }
        if (s === 'failed') { send({ op: 'call', kind: 'END' }); endCall(T('failed')); }
      }
    });
    send({ op: 'call', kind: 'RING', video: withVideo });
    call.offer(withVideo).catch(function () {
      send({ op: 'call', kind: 'END' });
      endCall(T('media'));
    });
  }
  function callFrame(f) {
    if (f.kind === 'UNAVAILABLE') { endCall(T('unavailable')); return; }
    if (!call) { return; }
    if (f.kind === 'ANSWER') {
      call.accepted(f.sdp).catch(function () { send({ op: 'call', kind: 'END' }); endCall(T('failed')); });
    } else if (f.kind === 'ICE') {
      call.candidate(f.candidate);
    } else if (f.kind === 'END') {
      endCall(T('ended'));
    }
  }
  voice.addEventListener('click', function () { startCall(false); });
  video.addEventListener('click', function () { startCall(true); });
  root.querySelector('.chat-end').addEventListener('click', function () {
    send({ op: 'call', kind: 'END' });
    endCall(T('ended'));
  });
  var mute = root.querySelector('.chat-mute');
  mute.addEventListener('click', function () {
    if (call) { mute.setAttribute('aria-pressed', call.mute() ? 'true' : 'false'); }
  });

  // ------------------------------------------------------------ start
  setOnline(0);
  if (location.hash === '#chat') {
    open(true);
  } else if (sget('.open') === '1') {
    open(false);
  } else if (get('')) {
    connect();
  }
})();
