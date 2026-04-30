(function() {
  var state = {
    metadata: null,
    language: "java",
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
    lintTimer: 0,
    uiids: []
  };
  var SUPPORTED_STATES = ["pressed", "selected", "unselected", "disabled"];
  var SUPPORTED_PROPERTIES = [
    "color", "background-color", "font-family", "font-size", "font-style", "text-decoration",
    "margin", "padding", "border",
    "transparency", "opacity", "alignment",
    "cn1-derive", "cn1-background-type", "cn1-bg-color", "cn1-bg-image", "cn1-bg-image-scaled",
    "cn1-border-type", "cn1-border-color", "cn1-border-radius", "cn1-border-width",
    "cn1-box-shadow-h", "cn1-box-shadow-v", "cn1-box-shadow-spread", "cn1-box-shadow-blur", "cn1-box-shadow-color"
  ];

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
    registerMonacoThemes();
    state.model = monaco.editor.createModel("", state.language || "java");
    state.editor = monaco.editor.create(document.getElementById("editor"), {
      model: state.model,
      automaticLayout: true,
      fontSize: 14,
      minimap: { enabled: false },
      scrollBeyondLastLine: false,
      wordWrap: "off",
      glyphMargin: false,
      // Narrower, lighter line-number gutter.
      lineNumbersMinChars: 2,
      lineDecorationsWidth: 4,
      renderLineHighlight: "none",
      quickSuggestions: {
        other: true,
        comments: false,
        strings: false
      },
      suggestOnTriggerCharacters: true,
      tabSize: 4,
      insertSpaces: true
    });
    registerCompletionProviders();
    state.editor.onDidChangeModelContent(function() {
      if (state.suppressChange) {
        return;
      }
      state.version += 1;
      scheduleLocalLint();
      scheduleChangeNotification();
    });
  }

  function registerMonacoThemes() {
    monaco.editor.defineTheme("cn1-playground-light", {
      base: "vs",
      inherit: true,
      rules: [],
      colors: {
        "editor.background": "#FAFAFC",
        "editor.foreground": "#112247",
        "editorGutter.background": "#FAFAFC",
        "editorLineNumber.foreground": "#C5CBD6",
        "editorLineNumber.activeForeground": "#8692A8",
        "editorCursor.foreground": "#2F6BFF",
        "editor.selectionBackground": "#E8F0FF",
        "editor.inactiveSelectionBackground": "#E8F0FFAA",
        "editorIndentGuide.background1": "#D9DEE8",
        "editorIndentGuide.activeBackground1": "#BFC7D6"
      }
    });
    monaco.editor.defineTheme("cn1-playground-dark", {
      base: "vs-dark",
      inherit: true,
      rules: [],
      colors: {
        "editor.background": "#112F70",
        "editor.foreground": "#F5F8FF",
        "editorGutter.background": "#112F70",
        "editorLineNumber.foreground": "#6E80A6",
        "editorLineNumber.activeForeground": "#D0DBEF",
        "editorCursor.foreground": "#4D86FF",
        "editor.selectionBackground": "#4D86FF33",
        "editor.inactiveSelectionBackground": "#4D86FF22",
        "editorIndentGuide.background1": "#4C6EA8",
        "editorIndentGuide.activeBackground1": "#7390C0"
      }
    });
  }

  function registerCompletionProviders() {
    monaco.languages.registerCompletionItemProvider("java", {
      triggerCharacters: [".", "(", ",", " "],
      provideCompletionItems: function(model, position) {
        if (!state.metadata) {
          return { suggestions: [] };
        }
        var text = model.getValue();
        var offset = model.getOffsetAt(position);
        return { suggestions: collectSuggestions(model, position, text, offset) };
      }
    });
    monaco.languages.registerCompletionItemProvider("css", {
      triggerCharacters: [".", "-", " "],
      provideCompletionItems: function(model, position) {
        return { suggestions: cssSuggestions(model, position) };
      }
    });
  }

  function cssSuggestions(model, position) {
    var text = model.getValue();
    var offset = model.getOffsetAt(position);
    if (isInsideCssRuleBlock(text, offset)) {
      return cssPropertySuggestions(model, position);
    }
    var stateContext = findCssStateContext(text, offset);
    if (stateContext) {
      return cssStateSuggestions(model, position, stateContext.prefix);
    }
    return cssSelectorSuggestions(model, position);
  }

  function cssSelectorSuggestions(model, position) {
    var word = model.getWordUntilPosition(position);
    var prefix = (word && word.word ? word.word : "");
    var range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
    var suggestions = [];
    (state.uiids || []).forEach(function(uiid) {
      if (!uiid) {
        return;
      }
      var selector = uiid;
      if (!matchesPrefix(selector, prefix)) {
        return;
      }
      suggestions.push({
        label: selector,
        kind: monaco.languages.CompletionItemKind.Class,
        insertText: selector,
        detail: "UIID selector",
        range: range
      });
      suggestions.push({
        label: selector + " { ... }",
        kind: monaco.languages.CompletionItemKind.Snippet,
        insertText: selector + " {\n\t$0\n}",
        insertTextRules: monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet,
        detail: "UIID rule",
        range: range
      });
    });
    return suggestions;
  }

  function cssStateSuggestions(model, position, prefix) {
    var range = new monaco.Range(position.lineNumber, position.column - prefix.length, position.lineNumber, position.column);
    var suggestions = [];
    SUPPORTED_STATES.forEach(function(stateName) {
      if (!matchesPrefix(stateName, prefix)) {
        return;
      }
      suggestions.push({
        label: "." + stateName,
        kind: monaco.languages.CompletionItemKind.EnumMember,
        insertText: stateName,
        detail: "Component state",
        range: range
      });
    });
    return suggestions;
  }

  function cssPropertySuggestions(model, position) {
    var word = model.getWordUntilPosition(position);
    var prefix = (word && word.word ? word.word : "");
    var range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
    var suggestions = [];
    SUPPORTED_PROPERTIES.forEach(function(propertyName) {
      if (!matchesPrefix(propertyName, prefix)) {
        return;
      }
      suggestions.push({
        label: propertyName,
        kind: monaco.languages.CompletionItemKind.Property,
        insertText: propertyName + ": ",
        detail: "Supported CN1 property",
        range: range
      });
    });
    return suggestions;
  }

  function isInsideCssRuleBlock(text, offset) {
    var depth = 0;
    for (var i = 0; i < offset && i < text.length; i++) {
      var ch = text.charAt(i);
      if (ch === "{") {
        depth++;
      } else if (ch === "}" && depth > 0) {
        depth--;
      }
    }
    return depth > 0;
  }

  function findCssStateContext(text, offset) {
    var i = offset - 1;
    while (i >= 0 && /\s/.test(text.charAt(i))) {
      i--;
    }
    var end = i + 1;
    while (i >= 0 && /[A-Za-z0-9_-]/.test(text.charAt(i))) {
      i--;
    }
    var prefix = text.substring(i + 1, end);
    while (i >= 0 && /\s/.test(text.charAt(i))) {
      i--;
    }
    if (i < 0 || text.charAt(i) !== ".") {
      return null;
    }
    i--;
    while (i >= 0 && /\s/.test(text.charAt(i))) {
      i--;
    }
    var selectorEnd = i + 1;
    while (i >= 0 && /[A-Za-z0-9_-]/.test(text.charAt(i))) {
      i--;
    }
    var selector = text.substring(i + 1, selectorEnd);
    if (!selector || selector.length === 0) {
      return null;
    }
    return { selector: selector, prefix: prefix };
  }

  function collectSuggestions(model, position, text, offset) {
    var importContext = findImportContext(model, position);
    if (importContext) {
      return importSuggestions(importContext, model, position);
    }
    var visibleTypes = getVisibleTypes(text);
    var receiver = findReceiver(text, offset);
    if (receiver) {
      var typeName = inferExpressionType(receiver, text, visibleTypes);
      return memberSuggestions(typeName);
    }
    return globalSuggestions(model, position, text, visibleTypes);
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

  function globalSuggestions(model, position, text, visibleTypes) {
    var suggestions = [];
    var word = model.getWordUntilPosition(position);
    var prefix = (word && word.word ? word.word : "");
    var range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
    var globals = state.metadata.globals || {};
    Object.keys(globals).forEach(function(name) {
      if (!matchesPrefix(name, prefix)) {
        return;
      }
      suggestions.push({
        label: name,
        kind: monaco.languages.CompletionItemKind.Variable,
        insertText: name,
        detail: globals[name],
        range: range
      });
    });
    Object.keys(visibleTypes).forEach(function(simple) {
      if (!matchesPrefix(simple, prefix)) {
        return;
      }
      suggestions.push({
        label: simple,
        kind: monaco.languages.CompletionItemKind.Class,
        insertText: simple,
        detail: visibleTypes[simple],
        range: range
      });
    });
    Object.keys(state.metadata.simpleIndex || {}).forEach(function(simple) {
      if (visibleTypes[simple] || !matchesPrefix(simple, prefix)) {
        return;
      }
      var candidates = state.metadata.simpleIndex[simple];
      for (var i = 0; i < candidates.length; i++) {
        suggestions.push(typeSuggestion(simple, candidates[i], text, range));
      }
    });
    ["import", "new", "return", "if", "for", "while", "class"].forEach(function(keyword) {
      if (!matchesPrefix(keyword, prefix)) {
        return;
      }
      suggestions.push({
        label: keyword,
        kind: monaco.languages.CompletionItemKind.Keyword,
        insertText: keyword,
        range: range
      });
    });
    return suggestions;
  }

  function findImportContext(model, position) {
    var line = model.getLineContent(position.lineNumber).substring(0, position.column - 1);
    var match = line.match(/^\s*import\s+([A-Za-z0-9_$.]*)$/);
    if (!match) {
      return null;
    }
    var path = match[1] || "";
    var lastDot = path.lastIndexOf(".");
    if (lastDot < 0) {
      return {
        parentPath: "",
        segmentPrefix: path
      };
    }
    return {
      parentPath: path.substring(0, lastDot),
      segmentPrefix: path.substring(lastDot + 1)
    };
  }

  function importSuggestions(context, model, position) {
    var suggestions = [];
    var word = model.getWordUntilPosition(position);
    var range = new monaco.Range(position.lineNumber, word.startColumn, position.lineNumber, word.endColumn);
    var seen = {};
    var packageNames = Object.keys(state.metadata.packages || {});
    for (var i = 0; i < packageNames.length; i++) {
      var packageName = packageNames[i];
      var child = childPackageSegment(packageName, context.parentPath);
      if (child && matchesPrefix(child, context.segmentPrefix) && !seen["pkg:" + child]) {
        seen["pkg:" + child] = true;
        suggestions.push({
          label: child,
          kind: monaco.languages.CompletionItemKind.Module,
          insertText: child,
          detail: context.parentPath ? context.parentPath + "." + child : child,
          range: range
        });
      }
    }
    var importPackage = context.segmentPrefix.length === 0 ? context.parentPath : null;
    if (importPackage && state.metadata.packages[importPackage]) {
      suggestions.push({
        label: "*",
        kind: monaco.languages.CompletionItemKind.Module,
        insertText: "*",
        detail: importPackage + ".*",
        range: range
      });
      var classes = state.metadata.packages[importPackage];
      for (var j = 0; j < classes.length; j++) {
        suggestions.push({
          label: classes[j],
          kind: monaco.languages.CompletionItemKind.Class,
          insertText: classes[j],
          detail: importPackage + "." + classes[j],
          range: range
        });
      }
    }
    return suggestions;
  }

  function typeSuggestion(simple, qualifiedName, text, range) {
    var packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
    return {
      label: simple,
      kind: monaco.languages.CompletionItemKind.Class,
      insertText: simple,
      detail: qualifiedName,
      range: range,
      additionalTextEdits: buildImportEdits(text, qualifiedName, packageName + ".*")
    };
  }

  function buildImportEdits(text, qualifiedName, wildcardImport) {
    if (text.indexOf("import " + qualifiedName + ";") >= 0 || text.indexOf("import " + wildcardImport + ";") >= 0) {
      return [];
    }
    var lines = text.split("\n");
    var insertAfter = 0;
    for (var i = 0; i < lines.length; i++) {
      var trimmed = lines[i].trim();
      if (trimmed.indexOf("package ") === 0 || trimmed.indexOf("import ") === 0) {
        insertAfter = i + 1;
      }
    }
    return [{
      range: new monaco.Range(insertAfter + 1, 1, insertAfter + 1, 1),
      text: "import " + qualifiedName + ";\n"
    }];
  }

  function childPackageSegment(packageName, parentPath) {
    if (!parentPath) {
      var rootDot = packageName.indexOf(".");
      return rootDot < 0 ? packageName : packageName.substring(0, rootDot);
    }
    if (packageName === parentPath || packageName.indexOf(parentPath + ".") !== 0) {
      return "";
    }
    var remainder = packageName.substring(parentPath.length() + 1);
    var nextDot = remainder.indexOf(".");
    return nextDot < 0 ? remainder : remainder.substring(0, nextDot);
  }

  function matchesPrefix(candidate, prefix) {
    return !prefix || candidate.toLowerCase().indexOf(prefix.toLowerCase()) === 0;
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
      var codeLine = "";
      for (var column = 0; column < line.length; column++) {
        var ch = line.charAt(column);
        var next = column + 1 < line.length ? line.charAt(column + 1) : "";
        if (inBlockComment) {
          codeLine += " ";
          if (ch === "*" && next === "/") {
            inBlockComment = false;
            codeLine += " ";
            column += 1;
          }
          continue;
        }
        if (!inString && ch === "/" && next === "/") {
          while (codeLine.length < line.length) {
            codeLine += " ";
          }
          break;
        }
        if (!inString && ch === "/" && next === "*") {
          inBlockComment = true;
          codeLine += "  ";
          column += 1;
          continue;
        }
        if (!inString && (ch === '"' || ch === "'")) {
          inString = ch;
          codeLine += " ";
          continue;
        }
        if (inString) {
          codeLine += " ";
          if (ch === "\\") {
            if (column + 1 < line.length) {
              codeLine += " ";
            }
            column += 1;
            continue;
          }
          if (ch === inString) {
            inString = "";
          }
          continue;
        }
        codeLine += ch;
        if (ch === "{" || ch === "(" || ch === "[") {
          braceStack.push({ ch: ch, line: lineIndex + 1, column: column + 1 });
        } else if (ch === "}" || ch === ")" || ch === "]") {
          var open = braceStack.pop();
          if (!open || !matchesBrace(open.ch, ch)) {
            markers.push(marker(lineIndex + 1, column + 1, lineIndex + 1, column + 2, "Unmatched " + ch, monaco.MarkerSeverity.Error));
          }
        }
      }
      collectUnknownTypeMarkers(markers, codeLine, lineIndex + 1, text);
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
      if (!visible[token] && !(state.metadata.simpleIndex && state.metadata.simpleIndex[token])) {
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

  function bootstrap(metadataJson, source, language, darkMode, markers, messages, uiids) {
    ensureMonaco(function() {
      state.metadata = normalizeMetadata(JSON.parse(metadataJson));
      state.language = language || "java";
      state.uiids = uiids || [];
      state.bootstrapped = true;
      if (state.model && state.model.getLanguageId && state.model.getLanguageId() !== state.language) {
        monaco.editor.setModelLanguage(state.model, state.language);
      }
      setSource(source || "");
      setMarkers(markers || []);
      setInlineMessages(messages || []);
      applyTheme(!!darkMode);
      scheduleLocalLint();
    });
  }

  function normalizeMetadata(raw) {
    var packages = {};
    var simpleIndex = {};
    Object.keys(raw.types || {}).forEach(function(name) {
      var info = raw.types[name];
      if (!packages[info.package]) {
        packages[info.package] = [];
      }
      packages[info.package].push(info.simple);
      if (!simpleIndex[info.simple]) {
        simpleIndex[info.simple] = [];
      }
      simpleIndex[info.simple].push(name);
    });
    raw.packages = packages;
    raw.simpleIndex = simpleIndex;
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

  function setUiids(uiids) {
    state.uiids = uiids || [];
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
    monaco.editor.setTheme(state.dark ? "cn1-playground-dark" : "cn1-playground-light");
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
    applyTheme: applyTheme,
    setUiids: setUiids
  };
})();
