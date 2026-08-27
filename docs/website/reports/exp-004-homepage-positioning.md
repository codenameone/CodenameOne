# EXP-004: Homepage ownership versus reach

Date: 2026-08-27  
Owner: Shai Almog  
Status: Prepared; starts when the website change reaches production  
Funnel stage: Consideration  
North-Star link: More qualified project downloads can produce more first successful builds and monthly active builders.

## Hypothesis

If the homepage leads with UI ownership and stability instead of shared reach, then a larger share of eligible homepage sessions will complete an Initializr project download because ownership gives a developer a sharper reason to evaluate Codename One.

## The one metric

Completed Initializr downloads divided by homepage exposures for each arm:

- `Exp004OwnershipDownload / Exp004OwnershipExposure`
- `Exp004ReachDownload / Exp004ReachExposure`

Crisp records these events only for visitors who enable Crisp chat and analytics consent. The result therefore describes that consented subset, not every website visitor. An arm-specific download is counted only when the same browser tab/session recorded the corresponding homepage exposure, and each event is limited to once per session.

## Variants

- **Ownership:** `Native, and you own the UI` / `Native apps in Java — A UI you control`.
- **Reach:** `One Java project across every screen` / `Ship one feature once — Reach every target`.

The CTA, product proof, layout, and remaining homepage stay identical. After Crisp chat and analytics consent is enabled, a first-party browser assignment is persisted under `cn1-exp-004-arm-v1` so returning eligible visitors do not switch messages. Visitors who decline or have not chosen consent receive a page variant but no assignment is stored and no experiment event is sent. Local QA and Cloudflare Pages previews do not emit experiment telemetry; deployed previews show the ownership control.

## Expected effect

Ownership should increase the completed-download rate by at least 30% relative to reach. A smaller movement is not decision-useful at Codename One's traffic level.

## Guardrail

The overall 30-day signup-to-successful-build rate in the BuildCloud cohort must not fall materially during the test. This is a population guardrail, not a variant-level causal read.

## Sample and window

Run for at least four weeks and no more than six weeks. Treat the result as directional unless each arm records at least 15 completed downloads. Do not stop on a favorable early fluctuation.

## Decision rule

- **PROMOTE ownership:** at least 30% relative lift, at least 15 downloads in each arm, and the BuildCloud guardrail holds.
- **KILL ownership as the lead:** reach wins by the same threshold with the guardrail intact.
- **ITERATE:** neither arm clears the threshold, the sample is smaller, or the guardrail deteriorates.

## Reading procedure

1. Read the four Crisp event counts for the complete window.
2. Report both denominators and rates.
3. Read the BuildCloud 30-day cohort's `real`, `tried`, and `activated` values as the guardrail.
4. State whether a handful of downloads could reverse the result.
5. Record `PROMOTE`, `ITERATE`, or `KILL`; do not reinterpret the threshold after seeing the data.
