# LinkedIn company-page recovery experiment

Status: **blocked until BuildCloud attribution is available**. This document and the post in `social/linkedin/_drafts` are planning artifacts only. Nothing is queued or published.

## Decision

Stop judging LinkedIn by impressions alone. Run a four-week company-page experiment only after BuildCloud can associate the tagged console visit with signup, first build attempt, and first successful JavaScript build.

## Baselines

July 24, 2026 baseline:

- 1,511 followers
- 0 Premium-attributed auto-invited followers
- 253 post impressions and no reactions, comments, or reposts in the prior 30-day report
- 3 unique visitors
- 0 custom-button clicks
- 8 weekly search appearances

August 1–7, 2026 review:

- 1,508 total followers; one net follower lost during the week
- 0 Premium-attributed auto-invited followers
- 194 post impressions and no recorded engagement
- 0 unique visitors
- 8 search appearances
- Premium auto-invite and expanded custom-button placement remained enabled

These are small, directional samples. They establish that the existing syndication cadence is not producing a measurable business signal; they do not prove that LinkedIn cannot work.

## Hypothesis

Two native company-page posts per week, each organized around one concrete developer obstacle and one Build Cloud action, will produce attributable BuildCloud activity more reliably than the current article-summary cadence.

## Four-week test

- Publish two company-page posts per week for four weeks.
- Use one tagged Build Cloud link family: `utm_source=linkedin`, `utm_medium=company_page`, `utm_campaign=company_recovery_2026q3`, and a distinct `utm_content` value for every post.
- Keep every JavaScript call to action cloud-based. Do not use local or offline JavaScript builds as the campaign path.
- Give each post one technical claim, one runnable command or concrete artifact, and one question worth answering.
- Do not boost posts, invite followers manually, or change the Premium configuration during the test.

Planned angles:

1. JavaScript Build Cloud as the certificate-free first result.
2. JavaScript Build Cloud when iOS signing blocks progress.
3. The exact `mvn cn:build -P javascript` first-build path.
4. Moving the same Java source from JavaScript to Android and iOS.
5. A first-build error clinic: reply with the first error, not the whole log.
6. What the Free plan makes possible before committing to a paid tier.
7. A direct question about the setup step that blocks developers.
8. A transparent follow-up showing what the experiment learned.

## Measurement

Primary business measures, by `utm_content`:

- tagged console visits
- new or returning authenticated users
- first build attempts
- first successful JavaScript builds

LinkedIn diagnostics:

- post impressions and engagement
- unique Page visitors
- custom-button clicks
- search appearances
- Premium-attributed auto-invited followers

Guardrails:

- Treat LinkedIn samples as directional.
- Do not infer product activation from impressions or clicks.
- Do not alter the cadence or CTA midway through the four-week window.
- Do not publish the first post until the primary BuildCloud measures can be exported.

## Go/no-go rule

At the end of four weeks, continue only if the tagged posts produce BuildCloud visits and at least one downstream build attempt. A successful JavaScript build is the strongest signal. If posts generate impressions but no tagged BuildCloud activity, stop this format and test a different channel or offer.
