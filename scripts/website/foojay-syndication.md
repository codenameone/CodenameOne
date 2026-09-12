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

## One-time GitHub setup

The Actions `GITHUB_TOKEN` is scoped to the CN1 repository and cannot submit
articles to another repository. The old `FOOJAY_USER` / `FOOJAY_PASSWORD`
secrets are no longer used.

1. Use an account with direct write access to `foojayio/website`, or create a
   fork in that account. For example:
   `gh repo fork foojayio/website --clone=false`.
2. Set CN1's Actions variable `FOOJAY_HEAD_REPO` to the writable `owner/website`
   repository. If unset, it defaults to `foojayio/website` for direct contributors.
3. Set CN1's Actions secret `FOOJAY_GITHUB_TOKEN` to a token that can write Git
   contents in the head repository and open pull requests against the public
   upstream repository. Choose credentials that support the fork-to-upstream
   route; a token restricted to CN1 alone cannot do this. Keep the token out of
   command arguments and logs.

The script verifies write permission, fork ancestry, and the upstream author
profile before creating a branch. A missing token fails an eligible CI
submission with a setup message instead of silently dropping Foojay.

## Preview and submission

Offline selection only (no GitHub calls, image reads, or state changes):

```sh
python3 scripts/website/syndicate_foojay_posts.py --dry-run
```

Prepare an eligible bundle for review without submitting or changing state:

```sh
python3 scripts/website/syndicate_foojay_posts.py --output-dir /tmp/foojay-review
```

After configuration, the daily workflow uses `--submit`. To explicitly submit
the next eligible article locally, use authenticated `gh` or the token above:

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
