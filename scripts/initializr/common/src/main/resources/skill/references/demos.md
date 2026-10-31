# The demo applications: where to learn idiomatic Codename One

Codename One ships six complete, production-shaped applications. Each is an app
**and** its server, written by the Codename One team, tested end to end, and kept
building against the current framework. When you are unsure how something is
"meant" to be done -- a screen, a navigation pattern, a live channel, a REST
contract, a test, a theme -- **read how a demo does it before inventing a
pattern.** Copy the shape, not the domain.

## Where the source is

- **If this project was generated from one of these templates in the Initializr**,
  the demo *is* this project: its packages were renamed to yours, but the
  structure below is what you have on disk, and the demo's main class is named
  after your app. Read it locally.
- Otherwise read it on GitHub (no clone needed; fetch the raw file you want):
  `https://github.com/codenameone/CodenameOne/tree/master/scripts/<demo>`, raw
  files at `https://raw.githubusercontent.com/codenameone/CodenameOne/master/scripts/<demo>/<path>`.
- Each demo has an implementor's guide in the developer guide
  (`docs/developer-guide/<Demo>-Implementors-Guide.asciidoc` in the same repository).
  It explains *why* the code is laid out the way it is -- read it before making a
  structural change modeled on a demo.

All seven share this layout, which is itself the first best practice (Skirmish and Dash add a
`rules` module, the deterministic game simulation the app, server and tests all compile):

| Module | Holds |
| --- | --- |
| `shared` | The contract: DTOs and `@RestClient` interfaces. The app's build generates the client, the server's build generates the interfaces it implements, so a change one side missed is a compile error. |
| `common` | The application. One codebase for simulator, desktop, Android, iOS and web -- no platform modules. |
| `backend` | The Codename One Backend server (`@RestController`, entities in `domain`, migration scripts in `src/main/resources/db/migration`, runs on SQLite/PostgreSQL/MySQL/MariaDB). |

Run any of them with `CN1_PROFILE=dev backend/server.sh run` and, in another
terminal, `mvn verify -Psimulator -DskipTests -Dcodename1.platform=javase`. The
README of each lists the demo accounts.

## Which demo to read for what

| Demo | What it is | Read it for |
| --- | --- | --- |
| `scripts/wayline` | Ride hailing: rider, driver, admin | Role-based start screens, maps and live markers (`map/`), bottom sheets, wallet and card entry (`pay/`), admin dashboards and charts (`admin/`, `ui/Chart.java`), the reference implementation of the shared-contract full-stack pattern |
| `scripts/savor` | Food delivery: customer, merchant, courier, admin | Catalog/menu browsing, cart and checkout (`customer/`), merchant order queues and menu editing (`merchant/`), two state machines (order + delivery) mirrored between `shared` enums and the server |
| `scripts/parley` | End-to-end encrypted messenger with calls | Chat UI and media (`chat/`, `ui/ChatForm.java`), crypto with core APIs (`crypto/`), WebRTC calls (`com.codename1.webrtc`, `ui/CallScreen.java`), local store (`store/`), push and WebSocket delivery |
| `scripts/reel` | Video streaming: viewer, creator studio, moderation, admin | Video playback and its controls, audio and subtitle tracks (`player/`), protected (DRM) playback and offline downloads, uploads and transcoding jobs (`studio/`), plans, coupons and ads (`pay/`, `admin/`), households, profiles and parental ratings (`viewer/`) |
| `scripts/bastion` | Security-hardened bank and crypto exchange: customer, staff console | Security end to end: sealed key storage, biometric/PIN app lock, certificate pinning, secure windows (`security/`), device-key request signing, passkeys, TOTP and step-up (`security/RequestSigner.java`, server `money/RequestSigning.java`, `account/`), a Security Center that reports each protection's live state (`ui/SecurityCenterForm.java`); a double-entry ledger with idempotency and conditional updates (server `ledger/`), a hash-chained audit log (`audit/`), accounting reports and PDF printing (`books/`, `ui/Printout.java`), an order-matching exchange (`exchange/`), and real Ether custody on an Ethereum node (server `chain/`). Its implementor's guide ends with a security checklist |
| `scripts/skirmish` | Real-time 3D card battler | Games: `GameView`/3D rendering (`battle/`), a deterministic `rules` module shared by app, server and tests, replays, in-app purchases and rewarded ads (`engage/`), matchmaking over sockets, in-app editors (`forge/`) |
| `scripts/dash` | Endless runner with a 2D-to-3D camera swing: player, track editor, admin console | Games: a deterministic `rules` module shared by app, server and tests, with the server re-simulating every submitted run to verify leaderboard scores (`game/`, `progress/`), live races with voice over sockets (`net/`), crews, friends and level packs (`social/`), a track editor (`forge/`), in-app purchases and rewarded ads (`engage/`, `shop/`) |

## Patterns the app demos share (copy these)

Wayline, Savor, Parley and Reel follow all of these, except that Reel has no live
channel: playback reports progress over REST heartbeats instead. Skirmish and Dash are games and differ:
their socket is `net/GameLink.java` rather than `live/LiveChannel.java`, and they have
no `Prefs`, `Telemetry` or l10n bundles. Bastion has no live channel or
`Telemetry` (a bank sends no analytics and pulls its data on demand), and its
`net/Guard.java` adds the pinning and host allow-list every request goes through.

- **Thin entry class.** `<pkg>/<App>.java` extends `Lifecycle`; configuration in
  `AppConfig`, preferences in `Prefs`, opt-in analytics in `Telemetry`.
- **One networking layer.** `net/Net.java` + `net/Api.java` wrap the generated
  `@RestClient`s, `net/Session.java` holds the signed-in state; screens never
  build URLs or parse JSON themselves.
- **Live updates through one channel.** `live/LiveChannel.java` owns the
  WebSocket and fans events out on the EDT; screens subscribe rather than poll.
- **UI helpers instead of repeated code.** `ui/Ui.java`, `ui/Layouts.java`,
  `ui/Nav.java`, `ui/Look.java`, `ui/Skeleton.java` (loading placeholders),
  `ui/BottomSheet.java`. Look for an existing helper before adding a component.
- **Theme driven by variables.** `common/src/main/css/theme.css` opens with a
  block of variables (colors, corner radius) everything else is drawn from; dark
  mode and color schemes swap those, not individual rules.
- **Localized from day one.** All text in `common/src/main/l10n` bundles,
  including an RTL language (Hebrew), so layouts are checked mirrored.
- **Tested end to end.** `run-e2e.sh` starts the server on the `test` profile
  and drives the app in the simulator; screenshots land in `target/e2e-shots`.
  Backend tests run with `-Dcodename1.platform=backend test`.

When you borrow from a demo, keep its conventions consistent with the code
already in this project; do not paste a demo's domain classes wholesale.
