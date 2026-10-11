# Developer Guide website pages

The AsciiDoc book in `docs/developer-guide/developer-guide.asciidoc` remains the
source for the PDF, full-book HTML view, and Hugo chapter pages. Edit those source
files, not the generated Markdown.

`scripts/website/build.sh` generates the build-hint catalog, renders the full book,
and calls `generate_developer_guide.rb` before Hugo. The generator loads the entire
book with Asciidoctor so includes, attributes, figure numbers, and cross references
resolve in the same context as the PDF. Each chapter becomes a Markdown file under
`docs/website/content/developer-guide/chapters/`. Headings and ordinary source code
use Markdown; rich AsciiDoc blocks (including tables, callouts, admonitions, and
inline markup) retain Asciidoctor HTML through the `guide-block` shortcode. This
avoids a second AsciiDoc parser and does not enable raw HTML for the whole site.

Chapter URLs derive from source filenames, so editing a heading does not move its
page. Existing section IDs are retained. Navigation follows the book's part and
chapter order. Generated files are ignored by Git. Hugo provides the shared page
shell, a shared chapter sidebar, a numbered contents page, previous/next links
at the top and bottom of each chapter, and search content. Search excludes
the full-book duplicate at `/developer-guide/single-page/`. The consolidated build-hint
catalog lives in `Build-Hints.asciidoc`, under Build, sign and ship, and renders at
`/developer-guide/build-hints/`. Keep the generated table include there so the PDF,
full book, and chapter page use the same catalog.

Search filters are shareable URLs: `/search/?scope=guide`, `?scope=javadoc`,
`?scope=blog`, and `?scope=all`, with an optional `q` parameter. The prose index
labels each page's section and excludes API pages, which use the client/backend
identifier indexes. Entire-site results show matching guide chapters, then API
entries, then other site pages and blog posts. Recency only affects blog results.
Run `python3 scripts/website/test_search_index.py` for index regression checks and
`node scripts/website/test_search.mjs http://localhost:8794` against a built site
served by `wrangler pages dev docs/website/public --port 8794` for browser checks.

The Markdown link render hook also resolves legacy links in generated API comments
using the same anchor map, so Javadoc links lead directly to the relevant section.

Old `/manual/` chapter paths have redirects in `static/_redirects`. Old fragments
are routed through the generated `anchors.json` in the browser because HTTP
servers do not receive fragments. `guide-legacy-links.json` records renamed
anchors and historical chapter names. When adding a legacy chapter mapping, add
its extensionless, trailing-slash, `.html`, and `.html/` redirects too. Without
JavaScript, the guide directory and chapter navigation remain available, but an
old book fragment cannot automatically select a chapter.

Run the normal website build (Java 8 for the catalog Maven build):

```sh
WEBSITE_INCLUDE_DEVGUIDE=true WEBSITE_INCLUDE_SKINDESIGNER=false scripts/website/build.sh
ruby scripts/website/test_generate_developer_guide.rb
python3 scripts/website/check_developer_guide.py docs/website/public
```

For guide-only iteration after the generated build-hint catalog exists:

```sh
ruby scripts/website/generate_developer_guide.rb
hugo --source docs/website
python3 docs/website/scripts/generate_lunr_index.py
```

CI tests a small book with cross-chapter links, inline anchors, images, code,
callouts, tables, and footnotes. It also checks actual rendered guide links and
assets across the website, including every generated bookmark route.
