#!/usr/bin/env python3
"""
Serve a JavaScript-port browser bundle and capture CN1SS/browser logs.
"""

from __future__ import annotations

import argparse
import http.server
import json
import socketserver
import sys
import threading
from pathlib import Path


PROBE_JS = r"""
(function() {
  function stringify(value) {
    if (typeof value === 'string') {
      return value;
    }
    try {
      return JSON.stringify(value);
    } catch (err) {
      return String(value);
    }
  }

  var logSendDisabled = false;
  var logQueue = '';
  var logFlushScheduled = false;

  function flushLogs() {
    logFlushScheduled = false;
    if (logSendDisabled || !logQueue) {
      return;
    }
    var payload = logQueue;
    logQueue = '';
    try {
      if (navigator.sendBeacon) {
        var blob = new Blob([payload], { type: 'text/plain' });
        if (navigator.sendBeacon('/__cn1__/log', blob)) {
          return;
        }
      }
    } catch (err) {}
    fetch('/__cn1__/log', {
      method: 'POST',
      headers: { 'Content-Type': 'text/plain' },
      body: payload,
      keepalive: true
    }).catch(function() {
      logSendDisabled = true;
    });
  }

  function send(line) {
    if (logSendDisabled) {
      return;
    }
    logQueue += String(line) + "\n";
    if (!logFlushScheduled) {
      logFlushScheduled = true;
      setTimeout(flushLogs, 50);
    }
  }

  var screenshotStartSent = false;
  function detectPhaseMarkerFromConsole(line) {
    if (line.indexOf('CN1SS:INFO:suite starting test=') >= 0 && !screenshotStartSent) {
      screenshotStartSent = true;
      send('PARPAR:DIAG:SCREENSHOT_START:source=cn1ss_suite_start');
    }
  }

  ['log', 'warn', 'error'].forEach(function(level) {
    var original = console[level];
    console[level] = function() {
      var line = Array.prototype.map.call(arguments, stringify).join(' ');
      send(line);
      detectPhaseMarkerFromConsole(line);
      if (original) {
        original.apply(console, arguments);
      }
    };
  });

  window.addEventListener('error', function(event) {
    send('BROWSER:ERROR:' + stringify(event.message || event.error || event));
  });
  window.addEventListener('unhandledrejection', function(event) {
    send('BROWSER:REJECTION:' + stringify(event.reason || event));
  });

  var lastState = '';
  setInterval(function() {
    var state = 'initialized=' + (!!window.cn1Initialized) + ' started=' + (!!window.cn1Started);
    if (state !== lastState) {
      lastState = state;
      send('BROWSER:STATE:' + state);
    }
    if (window.__parparError) {
      send('BROWSER:PARPAR_ERROR:' + stringify(window.__parparError));
    }
    if (window.__parparResult) {
      send('BROWSER:PARPAR_RESULT:' + stringify(window.__parparResult));
    }
  }, 1000);

  send('BROWSER:READY');
  send('PARPAR:DIAG:BOOT:probe=ready');
})();
"""


class HarnessState:
    def __init__(self, serve_dir: Path, log_file: Path, finished_marker: str):
        self.serve_dir = serve_dir
        self.log_file = log_file
        self.finished_marker = finished_marker
        self.finished = threading.Event()

    def append_log(self, text: str) -> None:
        self.log_file.parent.mkdir(parents=True, exist_ok=True)
        with self.log_file.open("a", encoding="utf-8") as out:
            out.write(text)
        if self.finished_marker and self.finished_marker in text:
            self.finished.set()


def create_handler(state: HarnessState):
    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=str(state.serve_dir), **kwargs)

        def log_message(self, fmt: str, *args) -> None:
            sys.stderr.write("[javascript-browser-harness] " + (fmt % args) + "\n")

        def do_GET(self) -> None:
            if self.path == "/__cn1__/probe.js":
                payload = PROBE_JS.encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/javascript; charset=utf-8")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)
                return
            if self.path == "/__cn1__/health":
                payload = json.dumps({"ready": True}).encode("utf-8")
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)
                return
            super().do_GET()

        def do_POST(self) -> None:
            if self.path != "/__cn1__/log":
                self.send_error(404)
                return
            length = int(self.headers.get("Content-Length", "0"))
            data = self.rfile.read(length).decode("utf-8", errors="replace")
            state.append_log(data)
            self.send_response(204)
            self.end_headers()

    return Handler


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--serve-dir", required=True)
    parser.add_argument("--log-file", required=True)
    parser.add_argument("--url-file", required=True)
    parser.add_argument("--finished-marker", default="CN1SS:SUITE:FINISHED")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=0)
    args = parser.parse_args()

    state = HarnessState(Path(args.serve_dir), Path(args.log_file), args.finished_marker)
    handler = create_handler(state)

    class ThreadingTCPServer(socketserver.ThreadingMixIn, socketserver.TCPServer):
        allow_reuse_address = True

    with ThreadingTCPServer((args.host, args.port), handler) as httpd:
        url = f"http://{args.host}:{httpd.server_address[1]}/"
        Path(args.url_file).write_text(url, encoding="utf-8")
        print(url, flush=True)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            return 0
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
