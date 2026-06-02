// Reusable blog prose remediation workflow (Phase 5).
// Invoke via Workflow({ scriptPath: ".../blog_remediation.workflow.js", args: { posts: [...] } }).
// Each item: { path, findings: [{rule, line, message, context}, ...] }.
// Stage 1: a fix agent applies ONLY high-confidence corrections (Edit tool).
// Stage 2: an adversarial verifier checks each diff. Pipeline = no barrier.
export const meta = {
  name: 'blog-prose-remediation',
  description: 'Fix high-confidence prose defects across a batch of blog posts, then adversarially verify each diff',
  phases: [
    { title: 'Fix', detail: 'one agent per post applies only unambiguous corrections' },
    { title: 'Verify', detail: 'adversarial verifier checks meaning/guardrails per diff' },
  ],
}

const RULES = `
HARD GUARDRAILS — violating any of these is a failure:
- Edit ONLY the author's body prose. NEVER modify: the YAML front matter (the
  leading block between --- and ---), fenced or inline code, Hugo shortcodes
  ({{< ... >}} / {{% ... %}}), link/image targets or URLs, and ANYTHING in or
  after a "## Archived Comments" or "## Discussion" heading (imported
  third-party comments — strictly off limits).
- Fix ONLY unambiguous errors: clear typos (e.g. "apprently" -> "apparently"),
  doubled words ("the the" -> "the"), and clear grammar ("can sends" ->
  "can send", "This weeks update" -> "This week's update", "Steves" -> "Steve's").
- DO NOT change, and SKIP, the following (they are NOT errors):
  * British spellings (behaviour, colour, rasterisation, artefacts, …).
  * Proper names and handles, even if lowercased or unusual (leave them exactly).
  * Product/library names even if lowercased (braintree, sendgrid, builtin) —
    SKIP unless it is unmistakably a typo of a common word.
  * The informal word "thru" — the author's deliberate voice. SKIP it.
  * Typography nits ("..." vs "…") and Oxford-comma preferences — SKIP.
- When in any doubt, SKIP. Minimal, conservative edits only. Never change meaning
  or voice. Many of the flagged findings are false positives; fixing nothing is a
  perfectly valid outcome for a post.
`

const FIX_SCHEMA = {
  type: 'object',
  properties: {
    edits: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          before: { type: 'string' }, after: { type: 'string' }, reason: { type: 'string' },
        },
        required: ['before', 'after', 'reason'],
      },
    },
    skipped: {
      type: 'array',
      items: {
        type: 'object',
        properties: { context: { type: 'string' }, why: { type: 'string' } },
        required: ['context', 'why'],
      },
    },
  },
  required: ['edits', 'skipped'],
}

const VERDICT_SCHEMA = {
  type: 'object',
  properties: {
    safe: { type: 'boolean' },
    meaning_preserved: { type: 'boolean' },
    only_intended_changes: { type: 'boolean' },
    front_matter_and_code_untouched: { type: 'boolean' },
    issues: { type: 'array', items: { type: 'string' } },
  },
  required: ['safe', 'meaning_preserved', 'only_intended_changes', 'front_matter_and_code_untouched', 'issues'],
}

let _a = args
if (typeof _a === 'string') { try { _a = JSON.parse(_a) } catch (e) { _a = null } }
const posts = Array.isArray(_a) ? _a : (_a && Array.isArray(_a.posts) ? _a.posts : [])
if (!posts.length) throw new Error('no posts provided in args; typeof args=' + typeof args)

const results = await pipeline(
  posts,
  (post) => agent(
    `You are correcting genuine prose errors in the AUTHOR's own writing of a Codename One blog post.\n\n` +
    `File: ${post.path}\n\n` +
    `A linter flagged these candidate findings (many are false positives — names, British spellings, intentional style):\n` +
    JSON.stringify(post.findings, null, 1) + `\n\n` +
    `Read the file, then use the Edit tool to apply ONLY the high-confidence corrections.\n` +
    `The findings list above is a STARTING POINT, not exhaustive and full of false positives. ` +
    `While reading the author's prose, ALSO fix any other GENUINE, unambiguous errors you spot ` +
    `(obvious misspellings like "apprently"->"apparently", doubled words, clear subject-verb or ` +
    `possessive grammar). If the findings list is empty, still read the post and fix any genuine ` +
    `errors. If the post's author prose is clean, make NO edits — that is the expected outcome for ` +
    `most posts.\n` +
    RULES +
    `\nAfter editing, return the structured list of the edits you actually applied and the findings you skipped (with why).`,
    { label: `fix:${post.path.split('/').pop()}`, phase: 'Fix', schema: FIX_SCHEMA }
  ),
  (fix, post) => (!fix || !Array.isArray(fix.edits) || fix.edits.length === 0)
    // No edits were made — nothing to verify. Skip the verifier agent (saves a
    // whole agent on the clean majority); the post-batch mechanical guardrail
    // check is the backstop.
    ? { safe: true, meaning_preserved: true, only_intended_changes: true, front_matter_and_code_untouched: true, issues: [], no_edits: true }
    : agent(
    `Adversarially verify the prose edits made to a blog post. Be skeptical; your job is to catch mistakes.\n\n` +
    `File: ${post.path}\n` +
    `Run: git diff -- ${post.path}   to see exactly what changed.\n\n` +
    `The fixer reported these edits:\n${JSON.stringify(fix?.edits ?? [], null, 1)}\n\n` +
    `Confirm ALL of the following, and set safe=false if ANY fails:\n` +
    `- Every change fixes a genuine, unambiguous error (not a name, British spelling, product-name casing, "thru", or typography nit).\n` +
    `- Meaning and the author's voice are exactly preserved.\n` +
    `- Only intended changes are present (no stray edits).\n` +
    `- The YAML front matter, code, shortcodes, links, and any archived-comments/discussion section are untouched.\n` +
    `List any problems in issues[]. If the fixer made zero edits, that is valid: safe=true, issues empty.`,
    { label: `verify:${post.path.split('/').pop()}`, phase: 'Verify', schema: VERDICT_SCHEMA }
  )
)

return posts.map((p, i) => ({ path: p.path, verdict: results[i] || null }))
