# Social attribution and Page coordination

## A post to a first successful build

Use `social_campaigns.py link` when preparing a draft, before exact-link approval:

```sh
python3 scripts/website/social_campaigns.py link \
  --url https://www.codenameone.com/playground/ --source bluesky \
  --campaign social-2026-10 --item 2026-10-20-v2-b1
```

`utm_source` distinguishes the account/channel; `utm_campaign` groups the campaign;
`utm_content` is the unique immutable batch-version/item ID. Keep a private mapping
to the final platform permalink. Never put emails, handles of readers, or user IDs
in these parameters. Revising a reviewed link requires review of that version.
Already published/scheduled items are not silently retagged.

The website's `cn1-acquisition.js` preserves the first tagged acquisition for
24 hours in consent-gated session storage, across internal navigation. It forwards
the tuple only to allowlisted HTTPS BuildCloud entry links, including dynamic
pricing links, preserving the plan and fragment. Internal `utm_medium=website`
topic-interest links cannot replace acquisition. Declining analytics clears the
stored touch and restores decorated links. No consent, blocked storage after
navigation, another device/browser, a closed tab or an expired touch creates a
coverage gap, not evidence of an organic acquisition.

BuildCloud already captures console UTMs in the authentication session and claims
them once for a newly created account (password and federated auth). Its direct
`/login`, `/register` and `/secure/signup` paths must also capture the entry;
deploy the matching BuildCloud change before calling those journeys verified.
Existing-user logins are not new acquisitions. The existing
`/api/v2/console/admin/funnel-cohorts` aggregate groups by source, medium, campaign
and content and joins durable successful build evidence. No new user-level export
or tracking service is needed.

During the authorized overnight analytics session, save the existing read-only
admin aggregate response privately, with collection time and deployment versions.
Then run:

```sh
python3 scripts/website/social_campaigns.py report /private/path/funnel.json --window d30
```

The output retains only aggregate fields. `successful` counts new users in a
signup cohort who have achieved a successful build, even after a failed first
attempt; it is not `firstAttemptSucceeded`, build volume, or a causal result.
Rolling 30/90-day signup cohorts are not seven-day activation event windows.
Report unknown if diagnostics are missing, and flag disagreement with headline
activation rather than silently choosing a favorable count. Declined consent,
missing UTM identities, and historical untagged posts cannot be reconstructed.

## One LinkedIn Page publisher

The existing overnight social publishing workflow owns all Page writes, including
ad hoc promotions. Other tasks and repository social artifacts supply candidates;
they must not publish independently. The legacy local browser driver delegates
LinkedIn to this coordinator before opening a browser. Article syndication is
unchanged. Source-date-only repository tasks are not rolled to a new date or
silently approved by being present in the queue.

Private state lives under the social automation's `resources/` directory:
`linkedin-page-calendar.json` plus the existing approval/publication ledger. The
calendar is not approval evidence. Every action still requires the human approval
in the publishing chat. Scripts and backups may live there; no private state is
committed here.

Before any Page composer or schedule action, within the existing Firefox window:

1. Acquire the existing single-browser-worker lock. Verify the Page identity.
2. Read the actual live Page feed AND all native scheduled posts, other pending
   private batches and repository syndication queue/state. Record every relevant
   outside post/schedule with its actual public instant and permalink/ID. Resolve
   unknown timestamps before proceeding. Previously scheduled items need
   verification, never a second publication.
3. Update the calendar using `record`. Confirm cancellations remotely before
   recording `cancelled`. A reservation left by a crash remains blocking until
   reconciled. Record any collision even if a different producer caused it.
4. Run `audit` with the observation time, evidence file and the inspected time
   range. It must cover at least 20 hours before AND after the target publication,
   including future native schedules. Never invent a fresh audit from old state.
5. Run `reserve --id BATCH-ITEM --at OFFSET-TIMESTAMP --approval-ref CHAT-EVIDENCE`.
   The tool atomically rejects another reservation within 20 hours, stale native
   evidence, missed times and duplicate IDs. Planning `check` is not permission
   to publish. The normal interval remains 24 hours.
6. Only after success perform the exact approved native action. Immediately
   `record --status scheduled|published --url VERIFIED-URL-OR-ID --evidence FILE`
   using that same ID/instant and update the publication ledger. If the outcome
   is ambiguous, retain the reservation and inspect remotely before retrying.

All commands take `--calendar /private/path/linkedin-page-calendar.json` before
the subcommand. An absent or corrupt calendar fails closed. Do not invent an empty
replacement. The CLI never moves, cancels or publishes remote content. A collision
means defer the candidate for a new approved time, not reshuffle approved posts.
Manual writes outside the coordinator cannot be prevented by a local script; the
next native audit must reconcile them. No browser work is permitted during the
day merely to refresh this calendar.
