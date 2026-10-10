(() => {
  const input = document.getElementById("cn1-search-input");
  const status = document.getElementById("cn1-search-status");
  if (!input || !status) return;
  const scopes = [...document.querySelectorAll("[data-search-scope]")];
  const params = new URLSearchParams(location.search);
  let scope = scopes.some(link => link.dataset.searchScope === params.get("scope")) ? params.get("scope") : "all";
  input.value = params.get("q") || "";
  let docs = [], docsById = new Map(), entries = [];
  let pagesReady = false, apiReady = false, pagesFailed = false, apiFailed = false;
  let index = null;
  const escapeHtml = value => String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;")
    .replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  const sectionOf = doc => doc.section || (doc.url.startsWith("/developer-guide/") ? "guide" : doc.url.startsWith("/blog/") ? "blog" : "site");
  const snippet = (content, query) => {
    const pos = content.toLowerCase().indexOf(query.toLowerCase());
    const start = Math.max(0, pos - 70), end = Math.min(content.length, start + 200);
    return (start ? "…" : "") + content.slice(start, end) + (end < content.length ? "…" : "");
  };
  const recencyBoost = doc => {
    if (sectionOf(doc) !== "blog" || !doc.date || Number.isNaN(Date.parse(doc.date))) return 1;
    return Math.max(0.5, 2 * Math.exp(-Math.max(0, Date.now() - Date.parse(doc.date)) / (3 * 365.25 * 86400000)));
  };
  // Keep literal words alongside stems in ONE index. This supports both normal
  // inflections and typo recovery ("notificaton" versus "notification") without
  // retaining separate indexes for each scope or fallback mode.
  const literalAndStem = token => {
    const stem = lunr.stemmer(token.clone());
    return stem.toString() === token.toString() ? token : [token, stem];
  };
  const pageIndex = () => {
    if (!index) {
      lunr.Pipeline.registerFunction(literalAndStem, "cn1LiteralAndStem");
      index = lunr(function () {
        this.ref("id");
        this.field("title", { boost: 10 });
        this.field("content");
        this.pipeline.remove(lunr.stemmer);
        this.pipeline.add(literalAndStem);
        docs.forEach(doc => this.add(doc));
      });
    }
    return index;
  };
  const pageMatches = query => {
    if (!pagesReady || pagesFailed || scope === "javadoc") return [];
    // The query builder treats punctuation as text, not Lunr query syntax.
    // Require every word so broad chapters do not outrank a specific match
    // merely because they contain one of several unrelated query terms.
    const tokens = lunr.tokenizer(query).map(token => token.toString());
    if (!tokens.length) return [];
    // Filter EACH attempt before deciding whether a fallback is needed: an exact
    // match in the blog must not suppress typo recovery in the guide.
    const inScope = hits => hits.filter(hit => scope === "all" || sectionOf(docsById.get(hit.ref)) === scope);
    let matches = inScope(pageIndex().query(q => {
      tokens.forEach(term => q.term(term, { presence: lunr.Query.presence.REQUIRED }));
    }));
    if (!matches.length) {
      matches = inScope(pageIndex().query(q => {
        tokens.forEach(term => q.term(term, { presence: lunr.Query.presence.REQUIRED,
          wildcard: lunr.Query.wildcard.TRAILING, usePipeline: false }));
      }));
    }
    if (!matches.length) {
      matches = inScope(pageIndex().query(q => {
        tokens.forEach(term => q.term(term, { presence: lunr.Query.presence.REQUIRED, editDistance: 1, usePipeline: false }));
      }));
    }
    return matches.map(hit => ({ doc: docsById.get(hit.ref), score: hit.score }))
      .sort((a, b) => b.score * recencyBoost(b.doc) - a.score * recencyBoost(a.doc));
  };
  const humps = (name) => {
    let out = "";
    for (let i = 0; i < name.length; i++) {
      const c = name[i];
      if (i === 0 || (c >= "A" && c <= "Z")) out += c.toLowerCase();
    }
    return out;
  };

  // Two indexes, one per reference: the client API and the backend API. A type
  // shared by both (the ORM and the entity annotations) is in each; it is kept
  // from the client index, where its page is the canonical one, and labelled as
  // shared rather than listed twice.
  const audienceOf = (payload, type) => (type.sh ? "shared" : (type.a || payload.audience || "client"));

  const buildEntries = (payloads) => {
    const out = [];
    for (const payload of payloads) {
      const isBackendIndex = payload.audience === "backend";
      for (const type of payload.types || []) {
        if (isBackendIndex && type.sh) continue;
        const audience = audienceOf(payload, type);
        out.push({
          label: type.n,
          context: type.p,
          url: type.u,
          kind: type.k,
          audience: audience,
          summary: type.s || "",
          key: type.n.toLowerCase(),
          humps: humps(type.n),
          isType: true,
        });
        for (const [label, anchor] of type.m || []) {
          const name = label.split("(")[0];
          out.push({
            label: label,
            context: type.p + "." + type.n,
            url: type.u + "#" + anchor,
            kind: "member",
            audience: audience,
            summary: "",
            key: name.toLowerCase(),
            humps: humps(name),
            isType: false,
          });
        }
      }
    }
    return out;
  };

  const AUDIENCE_LABELS = { client: "client", backend: "backend", shared: "client + backend" };

  // Lower sorts first. Types outrank members at equal quality of match, because
  // a query that names a type usually means the type.
  const score = (entry, query) => {
    const key = entry.key;
    let base;
    if (key === query) base = 0;
    else if (key.startsWith(query)) base = 10;
    else if (entry.humps.startsWith(query)) base = 20;
    else if (key.includes(query)) base = 30;
    else return null;
    return base + (entry.isType ? 0 : 5) + Math.min(key.length / 100, 0.99);
  };

  const renderPages = (id, hits, query) => {
    document.getElementById(id).innerHTML = hits.slice(0, 25).map(({doc}) => `
      <article class="cn1-search-result">
        <h3><a href="${escapeHtml(doc.url)}">${escapeHtml(doc.title)}</a></h3>
        <p>${escapeHtml(snippet(doc.content || "", query))}</p>
        <div class="cn1-search-result__meta">
          <a class="cn1-search-result__url" href="${escapeHtml(doc.url)}">${escapeHtml(doc.url)}</a>
          ${sectionOf(doc) === "blog" && doc.date ? `<span class="cn1-search-result__date">${escapeHtml(doc.date.slice(0, 10))}</span>` : ""}
        </div>
      </article>`).join("");
  };
  const renderApi = hits => {
    document.getElementById("cn1-search-api-results").innerHTML = hits.slice(0, 25).map(({entry}) => `
      <a class="cn1-search-api__hit" href="${escapeHtml(entry.url)}">
        <code class="cn1-search-api__name">${escapeHtml(entry.label)}</code>
        <span class="cn1-search-api__kind"><span class="cn1-api-badge cn1-api-badge--${escapeHtml(entry.audience)}">${escapeHtml(AUDIENCE_LABELS[entry.audience] || entry.audience)}</span> ${escapeHtml(entry.kind)}</span>
        <span class="cn1-search-api__context">${escapeHtml(entry.context)}</span>
        ${entry.summary ? `<span class="cn1-search-api__summary">${escapeHtml(entry.summary)}</span>` : ""}
      </a>`).join("");
  };
  const updateLinks = () => {
    scopes.forEach(link => {
      const selected = link.dataset.searchScope === scope;
      if (selected) link.setAttribute("aria-current", "page");
      else link.removeAttribute("aria-current");
      const target = new URL(link.href);
      if (input.value.trim()) target.searchParams.set("q", input.value.trim());
      else target.searchParams.delete("q");
      link.href = target.pathname + target.search;
    });
  };
  const run = () => {
    const query = input.value.trim();
    updateLinks();
    const hits = query ? pageMatches(query) : [];
    const guide = hits.filter(hit => sectionOf(hit.doc) === "guide");
    const pages = hits.filter(hit => sectionOf(hit.doc) !== "guide");
    const api = query && (scope === "all" || scope === "javadoc") ? entries.map(entry => ({entry, score: score(entry, query.toLowerCase())}))
      .filter(hit => hit.score !== null).sort((a, b) => a.score - b.score) : [];
    renderPages("cn1-search-guide-results", guide, query);
    renderPages("cn1-search-results", pages, query);
    renderApi(api);
    document.getElementById("cn1-search-guide").hidden = !guide.length;
    document.getElementById("cn1-search-pages").hidden = !pages.length;
    document.getElementById("cn1-search-api").hidden = !api.length;
    document.getElementById("cn1-search-pages-title").textContent = scope === "blog" ? "Blog" : "Site pages and blog";
    const waiting = (scope !== "javadoc" && !pagesReady) || ((scope === "all" || scope === "javadoc") && !apiReady);
    const failed = (scope !== "javadoc" && pagesFailed) || ((scope === "all" || scope === "javadoc") && apiFailed);
    const count = hits.length + api.length;
    const shown = Math.min(25, guide.length) + Math.min(25, pages.length) + Math.min(25, api.length);
    status.textContent = !query ? "Type to search." : count ? `${count} result${count === 1 ? "" : "s"}${shown < count ? ` (showing ${shown})` : ""}.` : waiting ? "Searching…" : failed ? "Search is temporarily unavailable." : "No results found.";
    if (failed && (count || !query)) status.textContent += " Some search indexes are unavailable. Please try again later.";
    else if (waiting && count) status.textContent += " Loading more results…";
  };
  const syncUrl = () => {
    const url = new URL(location.href);
    url.searchParams.set("scope", scope);
    if (input.value.trim()) url.searchParams.set("q", input.value.trim());
    else url.searchParams.delete("q");
    history.replaceState(null, "", url);
  };
  let timer;
  input.addEventListener("input", () => {
    clearTimeout(timer);
    syncUrl();
    timer = setTimeout(run, 120);
  });
  scopes.forEach(link => link.addEventListener("click", event => {
    if (event.button || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    event.preventDefault();
    scope = link.dataset.searchScope;
    syncUrl();
    run();
  }));
  const load = url => fetch(url).then(response => {
    if (!response.ok) throw new Error(`HTTP ${response.status}: ${url}`);
    return response.json();
  });
  load("/lunr-index.json").then(index => Array.isArray(index.parts)
    ? Promise.all(index.parts.map(load)).then(parts => parts.flatMap(part => part.docs || [])) : index.docs || [])
    .then(data => {
      if (!window.lunr || !data.length) throw new Error("Page search index unavailable");
      docs = data;
      docsById = new Map(docs.map(doc => [String(doc.id), doc]));
    }).catch(error => { pagesFailed = true; console.error(error); })
    .finally(() => { pagesReady = true; run(); });
  Promise.allSettled([load("/javadoc-search.json"), load("/backend-javadoc-search.json")])
    .then(results => {
      const payloads = results.filter(result => result.status === "fulfilled").map(result => result.value);
      apiFailed = payloads.length < results.length;
      entries = buildEntries(payloads);
      apiReady = true;
      run();
    });
  run();
})();
