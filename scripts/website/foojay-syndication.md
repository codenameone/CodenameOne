# Foojay article submissions

Foojay moved from WordPress to Hugo in September 2026. Its
[submission guide](https://foojayio.github.io/website/today/how-to-submit-your-next-article-on-foojay-io/)
now asks for `draft/<slug>/index.md` and images in the same folder, submitted as a
pull request to `foojayio/website`. A maintainer reviews the article, sets its
publication date, and moves it into `content/posts/`. The schema comes from
[template/post.md](https://github.com/foojayio/website/blob/main/template/post.md).

`syndicate_foojay_posts.py` replaces the retired WordPress browser adapter. It
reuses the shared post discovery and state, selects only Friday posts at least
seven days old, and submits at most one article per run. Existing WordPress
entries remain recorded: migration does not resubmit the historical backlog.

The bundle uses the existing `shai-almog` author profile, the `Java` category,
a description below 160 characters, a date without a time, and `canonical`
pointing to the original CN1 article. The suggested date is the source date plus
seven days; the editor chooses the final date. Images are copied from the local
website static directory where possible, with external body images fetched when
preparing a bundle. Image descriptions are retained. The preview must be a still
JPEG or PNG under 800 KB, each image must stay within Foojay's 4 MB limit, and
the bundle must stay under 15 MB. Prefer their
recommended 1600 × 900 preview under 300 KB when authoring the original post.
Mermaid shortcodes become native fenced diagrams; CN1 post links and footer
removal use the shared renderer.
The shared Codename One aside leaves its background unset so Foojay's light
and dark themes control its contrast, as requested by the Foojay editors.

## Where it runs

Submissions run on the signed-in local syndication runner
(`run-syndication.sh`), next to Medium and Hashnode, not in GitHub Actions.
The runner uses its own `gh` login (`gh auth login`, keyring-stored), so no
repository secret is involved. That login needs the `repo` and `workflow`
scopes and a fork of `foojayio/website` (`gh repo fork foojayio/website
--clone=false`); the runner passes it as `--head-repo <owner>/website`.

The `workflow` scope matters: before creating a submission branch the script
syncs the fork's default branch with upstream, and when Foojay has edited
`.github/workflows` since the last sync GitHub refuses the branch as a bare
HTTP 404 without it. Sync by hand with
`gh repo sync <owner>/website --source foojayio/website`.

The script verifies write permission, fork ancestry, and the upstream author
profile before creating a branch. It refuses `--submit` inside GitHub Actions
without `FOOJAY_GITHUB_TOKEN`, because the workflow `GITHUB_TOKEN` cannot
write to another repository; the daily workflow therefore runs `--dry-run`.

## Preview and submission

Offline selection only (no GitHub calls, image reads, or state changes):

```sh
python3 scripts/website/syndicate_foojay_posts.py --dry-run
```

Prepare an eligible bundle for review without submitting or changing state:

```sh
python3 scripts/website/syndicate_foojay_posts.py --output-dir /tmp/foojay-review
```

To submit the next eligible article by hand, as the runner does:

```sh
python3 scripts/website/syndicate_foojay_posts.py --submit --head-repo YOUR_ACCOUNT/website
```

The deterministic `cn1-syndication/<slug>` branch contains one atomic commit
with the whole bundle. Retries recover an existing open or merged PR if the
CN1 state commit was lost. A closed, unmerged PR or a mismatched existing branch
fails for manual review. An article with the same slug already in Foojay's
published or draft tree also blocks duplicate submission.

State records the PR URL with `status: submitted` and `published: false`.
Recovering a merged PR records `status: merged`, still without claiming public
publication. The existing health checker treats an editorial submission URL as
completed delivery, just as it previously treated a WordPress draft URL; it does
not monitor Foojay's editorial acceptance or public availability.

Run the offline regressions with:

```sh
python3 scripts/website/test_syndicate_foojay_posts.py
```
