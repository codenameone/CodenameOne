(function() {
  var state = {
    metadata: null,
    monacoReady: false,
    bootstrapped: false,
    editor: null,
    model: null,
    dark: false,
    version: 0,
    suppressChange: false,
    runtimeMarkers: [],
    heuristicMarkers: [],
    inlineMessages: [],
    decorations: [],
    zoneId: null,
    changeTimer: 0,
    lintTimer: 0
  };

  function post(payload) {
    var message = JSON.stringify(payload);
    if (window.cn1PostMessage) {
      window.cn1PostMessage(message);
      return;
    }
    if (window.parent && window.parent !== window && window.parent.postMessage) {
      window.parent.postMessage(message, '*');
    }
  }

  function ensureMonaco(callback) {
    if (state.monacoReady) {
      callback();
      return;
    }
    var vsBasePath = "monaco/min/vs";
    var workerMainUrl = new URL(vsBasePath + "/base/worker/workerMain.js", window.location.href).toString();
    window.MonacoEnvironment = {
      getWorkerUrl: function() {
        var worker = ""
          + "self.MonacoEnvironment={baseUrl:'" + vsBasePath + "/'};"
          + "importScripts('" + workerMainUrl + "');";
        return "data:text/javascript;charset=utf-8," + encodeURIComponent(worker);
      }
    };
    require.config({
      paths: {
        vs: vsBasePath
      }
    });
    require(["vs/editor/editor.main"], function() {
      createEditor();
      state.monacoReady = true;
      callback();
      post({ type: "ready" });
    });
  }

  function createEditor() {
    state.model = monaco.editor.createModel("", "java");
    state.editor = monaco.editor.create(document.getElementById("editor"), {
      model: state.model,
      automaticLayout: true,
      fontSize: 14,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      wordWrap: "off",
      glyphMargin: true,
      quickSuggestions: {
        other: true,
        comments: false,
        strings: false
      },
      suggestOnTriggerCharacters: true,
      tabSize: 4,
      insertSpaces: true
    });
    registerCompletionProvider();
    state.editor.onDidChangeModelContent(function() {
      if (state.suppressChange) {
        return;
      }
      state.version += 1;
      scheduleLocalLint();
      scheduleChangeNotification();
    });
  }

  function registerCompletionProvider() {
    monaco.languages.registerCompletionItemProvider("java", {
      triggerCharacters: [".", "(", ","],
      provideCompletionItems: function(model, position) {
        if (!state.metadata) {
          return { suggestions: [] };
        }
        var text = model.getValue();
        var offset = model.getOffsetAt(position);
        return { suggestions: collectSuggestions(text, offset) };
      }
    });
  }

  function collectSuggestions(text, offset) {
    var visibleTypes = getVisibleTypes(text);
    var receiver = findReceiver(text, offset);
    if (receiver) {
      var typeName = inferExpressionType(receiver, text, visibleTypes);
      return memberSuggestions(typeName);
    }
    return globalSuggestions(visibleTypes);
  }

  function memberSuggestions(typeName) {
    var typeInfo = typeName && state.metadata.types[typeName];
    if (!typeInfo) {
      return [];
    }
    var suggestions = [];
    (typeInfo.fields || []).forEach(function(field) {
      suggestions.push({
        label: field,
        kind: monaco.languages.CompletionItemKind.Field,
        insertText: field,
        detail: typeInfo.simple + "." + field
      });
    });
    (typeInfo.methods || []).forEach(function(signature) {
      var name = signature.substring(0, signature.indexOf("("));
      var hasArgs = signature !== name + "()";
      suggestions.push({
        label: signature,
        kind: monaco.languages.CompletionItemKind.Method,
        insertText: hasArgs ? name + "(${1})" : name + "()",
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        detail: typeInfo.simple + "." + signature
      });
    });
    return suggestions;
  }

  function globalSuggestions(visibleTypes) {
    var suggestions = [];
    var globals = state.metadata.globals || {};
    Object.keys(globals).forEach(function(name) {
      suggestions.push({
        label: name,
        kind: monaco.languages.CompletionItemKind.Variable,
        insertText: name,
        detail: globals[name]
      });
    });
    Object.keys(visibleTypes).forEach(function(simple) {
      suggestions.push({
        label: simple,
        kind: monaco.languages.CompletionItemKind.Class,
        insertText: simple,
        detail: visibleTypes[simple]
      });
    });
    ["import", "new", "return", "if", "for", "while", "class"].forEach(function(keyword) {
      suggestions.push({
        label: keyword,
        kind: monaco.languages.CompletionItemKind.Keyword,
        insertText: keyword
      });
    });
    return suggestions;
  }

  function findReceiver(text, offset) {
    var prefix = text.substring(0, offset);
    var dot = prefix.lastIndexOf(".");
    if (dot < 0) {
      return "";
    }
    var tail = prefix.substring(dot + 1);
    if (/[^A-Za-z0-9_$]/.test(tail)) {
      return "";
    }
    var head = prefix.substring(0, dot).match(/([A-Za-z_$][A-Za-z0-9_$]*)\s*$/);
    return head ? head[1] : "";
  }

  function inferExpressionType(token, text, visibleTypes) {
    if (!token) {
      return "";
    }
    var globals = state.metadata.globals || {};
    if (globals[token]) {
      return globals[token];
    }
    if (visibleTypes[token]) {
      return visibleTypes[token];
    }
    var declarations = collectDeclaredTypes(text, visibleTypes);
    return declarations[token] || "";
  }

  function collectDeclaredTypes(text, visibleTypes) {
    var found = {};
    var declaration = /\b([A-Z][A-Za-z0-9_$.<>\[\]]*)\s+([a-zA-Z_$][A-Za-z0-9_$]*)\s*(=|;|,)/g;
    var match;
    while ((match = declaration.exec(text))) {
      var typeToken = sanitizeTypeToken(match[1]);
      var fqcn = visibleTypes[typeToken] || state.metadata.simpleToQualified[typeToken] || "";
      if (fqcn) {
        found[match[2]] = fqcn;
      }
    }
    return found;
  }

  function sanitizeTypeToken(value) {
    return value.replace(/<.*$/, "").replace(/\[\]/g, "").replace(/^.*\./, function(prefix) {
      return prefix.indexOf(".") >= 0 ? prefix : "";
    });
  }

  function getVisibleTypes(text) {
    var visible = {};
    var defaults = state.metadata.defaultImports || [];
    for (var i = 0; i < defaults.length; i++) {
      mergePackage(visible, defaults[i]);
    }
    var importMatch;
    var explicitImport = /^\s*import\s+([a-zA-Z0-9_$.]+)\s*;\s*$/gm;
    while ((importMatch = explicitImport.exec(text))) {
      var imported = importMatch[1];
      if (imported.slice(-2) === ".*") {
        mergePackage(visible, imported.substring(0, imported.length - 2));
      } else {
        var simple = imported.substring(imported.lastIndexOf(".") + 1);
        visible[simple] = imported;
      }
    }
    Object.keys(state.metadata.globals || {}).forEach(function(name) {
      var typeName = state.metadata.globals[name];
      if (typeName) {
        visible[name] = typeName;
      }
    });
    return visible;
  }

  function mergePackage(target, packageName) {
    var names = state.metadata.packages[packageName] || [];
    for (var i = 0; i < names.length; i++) {
      target[names[i]] = packageName + "." + names[i];
    }
  }

  function scheduleChangeNotification() {
    if (state.changeTimer) {
      clearTimeout(state.changeTimer);
    }
    state.changeTimer = setTimeout(function() {
      post({
        type: "change",
        text: state.model.getValue(),
        version: state.version
      });
    }, 280);
  }

  function scheduleLocalLint() {
    if (state.lintTimer) {
      clearTimeout(state.lintTimer);
    }
    state.lintTimer = setTimeout(function() {
      applyHeuristicMarkers(computeHeuristicMarkers(state.model.getValue()));
    }, 180);
  }

  function applyHeuristicMarkers(markers) {
    state.heuristicMarkers = markers;
    refreshMarkers();
  }

  function refreshMarkers() {
    if (!state.model) {
      return;
    }
    monaco.editor.setModelMarkers(
      state.model,
      "cn1-playground",
      state.heuristicMarkers.concat(state.runtimeMarkers)
    );
  }

  function computeHeuristicMarkers(text) {
    var markers = [];
    var braceStack = [];
    var lines = text.split("\n");
    var inBlockComment = false;
    for (var lineIndex = 0; lineIndex < lines.length; lineIndex++) {
      var line = lines[lineIndex];
      var inString = "";
      for (var column = 0; column < line.length; column++) {
        var ch = line.charAt(column);
        var next = column + 1 < line.length ? line.charAt(column + 1) : "";
        if (inBlockComment) {
          if (ch === "*" && next === "/") {
            inBlockComment = false;
            column += 1;
          }
          continue;
        }
        if (!inString && ch === "/" && next === "/") {
          break;
        }
        if (!inString && ch === "/" && next === "*") {
          inBlockComment = true;
          column += 1;
          continue;
        }
        if (!inString && (ch === '"' || ch === "'")) {
          inString = ch;
          continue;
        }
        if (inString) {
          if (ch === "\\") {
            column += 1;
            continue;
          }
          if (ch === inString) {
            inString = "";
          }
          continue;
        }
        if (ch === "{" || ch === "(" || ch === "[") {
          braceStack.push({ ch: ch, line: lineIndex + 1, column: column + 1 });
        } else if (ch === "}" || ch === ")" || ch === "]") {
          var open = braceStack.pop();
          if (!open || !matchesBrace(open.ch, ch)) {
            markers.push(marker(lineIndex + 1, column + 1, lineIndex + 1, column + 2, "Unmatched " + ch, monaco.MarkerSeverity.Error));
          }
        }
      }
      collectUnknownTypeMarkers(markers, line, lineIndex + 1, text);
    }
    for (var i = 0; i < braceStack.length; i++) {
      var item = braceStack[i];
      markers.push(marker(item.line, item.column, item.line, item.column + 1, "Missing closing match for " + item.ch, monaco.MarkerSeverity.Error));
    }
    return markers;
  }

  function collectUnknownTypeMarkers(markers, line, lineNumber, fullText) {
    var visible = getVisibleTypes(fullText);
    var matcher = /\b([A-Z][A-Za-z0-9_]*)\b/g;
    var match;
    while ((match = matcher.exec(line))) {
      var token = match[1];
      if (isIgnoredTypeToken(line, token, match.index)) {
        continue;
      }
      if (!visible[token] && !state.metadata.simpleToQualified[token]) {
        markers.push(marker(lineNumber, match.index + 1, lineNumber, match.index + token.length + 1,
          "Unknown type " + token, monaco.MarkerSeverity.Warning));
      }
    }
  }

  function isIgnoredTypeToken(line, token, index) {
    if (["Component", "Object", "String", "Integer", "Long", "Boolean", "Double", "Float"].indexOf(token) >= 0) {
      return false;
    }
    if (line.indexOf("class " + token) >= 0 || line.indexOf("interface " + token) >= 0 || line.indexOf("enum " + token) >= 0) {
      return true;
    }
    if (index > 0 && line.charAt(index - 1) === ".") {
      return true;
    }
    return false;
  }

  function matchesBrace(open, close) {
    return (open === "{" && close === "}") ||
      (open === "(" && close === ")") ||
      (open === "[" && close === "]");
  }

  function marker(startLine, startColumn, endLine, endColumn, message, severity) {
    return {
      startLineNumber: Math.max(1, startLine),
      startColumn: Math.max(1, startColumn),
      endLineNumber: Math.max(1, endLine),
      endColumn: Math.max(1, endColumn),
      message: message,
      severity: severity
    };
  }

  function renderInlineMessages() {
    if (!state.editor || !state.model) {
      return;
    }
    state.editor.changeViewZones(function(accessor) {
      if (state.zoneId) {
        accessor.removeZone(state.zoneId);
        state.zoneId = null;
      }
      if (!state.inlineMessages.length) {
        return;
      }
      var node = document.createElement("div");
      node.className = "playground-zone" + (state.dark ? " dark" : "");
      state.inlineMessages.forEach(function(message) {
        var row = document.createElement("div");
        row.className = "playground-zone-row kind-" + (message.kind || "info");
        row.textContent = message.text;
        node.appendChild(row);
      });
      state.zoneId = accessor.addZone({
        afterLineNumber: state.model.getLineCount(),
        heightInPx: Math.max(28, state.inlineMessages.length * 22 + 8),
        domNode: node
      });
    });
  }

  function bootstrap(metadataJson, source, darkMode, markers, messages) {
    ensureMonaco(function() {
      state.metadata = normalizeMetadata(JSON.parse(metadataJson));
      state.bootstrapped = true;
      setSource(source || "");
      setMarkers(markers || []);
      setInlineMessages(messages || []);
      applyTheme(!!darkMode);
      scheduleLocalLint();
    });
  }

  function normalizeMetadata(raw) {
    var packages = {};
    var simpleToQualified = {};
    Object.keys(raw.types || {}).forEach(function(name) {
      var info = raw.types[name];
      if (!packages[info.package]) {
        packages[info.package] = [];
      }
      packages[info.package].push(info.simple);
      simpleToQualified[info.simple] = name;
    });
    raw.packages = packages;
    raw.simpleToQualified = simpleToQualified;
    return raw;
  }

  function setSource(source) {
    if (!state.model) {
      return;
    }
    if (state.model.getValue() === source) {
      return;
    }
    state.suppressChange = true;
    state.model.setValue(source);
    state.suppressChange = false;
    scheduleLocalLint();
  }

  function setMarkers(markers) {
    state.runtimeMarkers = (markers || []).map(function(markerDef) {
      return {
        startLineNumber: Math.max(1, markerDef.line || 1),
        startColumn: Math.max(1, markerDef.column || 1),
        endLineNumber: Math.max(1, markerDef.endLine || markerDef.line || 1),
        endColumn: Math.max(1, markerDef.endColumn || (markerDef.column || 1) + 1),
        message: markerDef.message || "",
        severity: toSeverity(markerDef.severity)
      };
    });
    refreshMarkers();
  }

  function setInlineMessages(messages) {
    state.inlineMessages = messages || [];
    renderInlineMessages();
  }

  function applyTheme(darkMode) {
    state.dark = !!darkMode;
    if (!state.monacoReady) {
      return;
    }
    monaco.editor.setTheme(state.dark ? "vs-dark" : "vs");
    renderInlineMessages();
  }

  function toSeverity(severity) {
    switch ((severity || "").toLowerCase()) {
      case "warning":
        return monaco.MarkerSeverity.Warning;
      case "info":
        return monaco.MarkerSeverity.Info;
      default:
        return monaco.MarkerSeverity.Error;
    }
  }

  window.PlaygroundEditor = {
    bootstrap: bootstrap,
    setSource: setSource,
    setMarkers: setMarkers,
    setInlineMessages: setInlineMessages,
    applyTheme: applyTheme
  };
})();
