# Storyboard: The App Stops. The Experience Does Not.

Status: **rendered, red-teamed, and retained as a reference implementation**

Target landscape length: 3:05-3:25
Target Short length: 0:52-0:58
Canonical article: https://www.codenameone.com/blog/widgets-live-activities-dynamic-island/

## The story in one sentence

A developer needs customers to keep seeing a delivery ETA after the app has stopped, discovers that every operating system surface has its own lifecycle and rendering rules, then publishes one serializable Java timeline that the system can keep rendering without a live Codename One UI.

## Viewer promise

By the end, the viewer will understand why widgets cannot be ordinary app components, how a dated timeline survives the dead-process boundary, and how the same source model becomes a widget, Live Activity, Dynamic Island presentation, Android ongoing notification, and desktop surface.

## Recurring visual identity

An orange delivery package and its ETA are the visual thread. At first they are trapped inside a running phone app. The app process powers off, but dated state cards leave the phone on a luminous timeline. Each card lands in a different system surface while the same package keeps moving toward the customer.

This is not a generic architecture diagram. It is a small animated story object that changes state from **Preparing** to **Out for delivery** to **Arriving now** to **Delivered**.

## Landscape storyboard

### 1. Identity and promise — 0:00-0:08

- **Picture:** A restrained Codename One signature appears in one corner while the delivery ETA slides out of a phone-shaped app window and remains visible after that window fades to black. No centered logo wall.
- **On-screen copy:** `Your app can stop. The experience can keep moving.`
- **Narration:** “Codename One lets Java developers build native mobile apps from one codebase. But today the interesting part begins after the app stops.”
- **Purpose:** Say who we are, what the topic is, and create a question before naming the feature.

### 2. The ordinary desire — 0:08-0:22

- **Picture:** A customer is waiting for a package. The app shows a four-minute ETA. The customer closes it and returns to the home screen.
- **Narration:** “Imagine a delivery app. The customer should see the ETA at a glance, without reopening the app every few seconds.”
- **Human beat:** The viewer understands the desired experience before hearing implementation language.

### 3. The dead-process problem — 0:22-0:43

- **Picture:** The app process, component tree, EDT, and Java listeners power down one by one. The ETA briefly disappears. A system widget remains outside the dead-process boundary.
- **On-screen copy:** `No process. No component tree. No Java listener.`
- **Narration:** “A widget does not live inside the running application UI. The process may be gone. There is no Codename One component tree, event dispatch thread, live object graph, or Java listener waiting for a tap.”
- **Independent proof:** Use an annotated crop derived from Apple’s WidgetKit timeline documentation, not a competitor logo collage.

### 4. The difficulty multiplies — 0:43-1:02

- **Picture:** The one ETA branches into four incompatible construction sites:
  - WidgetKit and ActivityKit
  - Android RemoteViews and ongoing notifications
  - desktop floating surfaces
  - simulator and build-time registration
- **Animation:** Different lifecycles, layouts, tools, and update mechanisms stack up around each site. The package does not move yet.
- **Narration:** “Then each platform changes the rules. Apple widgets use timelines. Live Activities have their own regions and updates. Android widgets use a constrained RemoteViews tree. Even taps and time changes must survive a cold start.”
- **Guardrail:** Do not mention Codename One’s solution during this escalation.

### 5. The insight — 1:02-1:18

- **Picture:** The app component tree dissolves into two durable artifacts: a compact surface layout and dated state cards. The platform side lights up again.
- **On-screen copy:** `Make the widget data, not a live component.`
- **Narration:** “The escape hatch is to stop treating the widget as a live component. Publish a serializable layout and a timeline of state. The operating system can persist that data and render it with its own surface technology.”
- **Transition:** Morph the dead component tree into the serialized surface tree. No bounce or decorative wipe.

### 6. The real Java source — 1:18-1:43

- **Picture:** Real code from `samples/samples/SurfacesSample/SurfacesSample.java`, with the live simulator occupying the right 42 percent of the frame.
- **Code focus sequence:**
  1. `WidgetTimeline timeline = new WidgetTimeline()`
  2. the four dated `addEntry(...)` calls
  3. `Surfaces.publish(KIND_DELIVERY, timeline);`
- **Pointer behavior:** A thin amber connector travels from each highlighted entry to its matching state in the preview. Never cover the code with a large annotation box.
- **Narration:** “This compiled sample publishes one layout and four dated state maps. Preparing. Out for delivery. Arriving now. Delivered. The system advances the timeline without waking the app.”
- **Proof:** Code must come from the sample, not a rewritten slide snippet.

### 7. Show it running — 1:43-2:06

- **Picture:** Actual JavaSE Surfaces sample and Widgets Preview.
- **Actions:**
  1. Click **Publish widget timeline** with a visible touch/click ring.
  2. Open or focus **Widgets Preview**.
  3. Step the timeline from Preparing to Out for delivery to Delivered.
  4. Change widget size once to demonstrate the registered layout.
- **Narration:** “In the simulator we can inspect the registered kind, change its size, and move through time before making a device build.”
- **Replay:** Instant-replay only the timeline step: 700 ms fast rewind with a small rewind glyph at top left, followed by a 1.6 second slow replay from Out for delivery to Delivered.

### 8. Time moves while Java sleeps — 2:06-2:21

- **Picture:** Split code and preview. Highlight `SurfaceDynamicText.STYLE_TIMER_DOWN` while the ETA visibly ticks in the preview. A sleeping Java-process icon stays dim.
- **Narration:** “The countdown is dynamic text. WidgetKit renders timed text, and Android uses a chronometer, so the seconds change without waking Java.”
- **Proof boundary:** Demonstrate the simulator behavior; describe backend mappings only as implemented mappings, not as on-device footage.

### 9. One state model, native system surfaces — 2:21-2:44

- **Picture:** The same package and state card morph through:
  - iOS home-screen widget
  - lock-screen Live Activity
  - Dynamic Island compact and expanded regions
  - Android ongoing notification
  - desktop floating pill
- **Code:** Briefly highlight `LiveActivityDescriptor` regions, then `activity.update(...)`.
- **Real interaction:** Click **Start Live Activity**, then **Advance state** twice. The mock Dynamic Island must visibly change progress and text; do not drag a static picture.
- **Narration:** “The Live Activity reuses the same nodes and state. Its descriptor adds the Dynamic Island regions. On Android the equivalent live surface is an ongoing notification; on desktop it is a floating pill.”

### 10. The tap crosses the cold start — 2:44-2:58

- **Picture:** Click the delivery surface. The app returns to the Order form with `Order: CN1-12345`, source, and cold-start state. Animate the string `open_order` crossing the process boundary before the form opens.
- **Narration:** “There is no listener inside the dead app. The surface carries an action ID. A tap launches the app, and Codename One delivers that action after startup.”
- **Proof:** Use `showOrderForm(...)` and the sample action handler.

### 11. Honest constraint — 2:58-3:09

- **Picture:** A rich component tree compresses into the deliberately smaller surface-node catalog. RemoteViews is shown as the narrowest gate, not as a fake or inferior backend.
- **Narration:** “This is intentionally not the full Codename One component set. System surfaces have tighter rules, and Android’s RemoteViews constraints define part of the common floor.”
- **Purpose:** Defuse the lowest-common-denominator objection before it becomes a comment.

### 12. Victory and outro — 3:09-3:24

- **Picture:** The app process is asleep. The delivery progresses across the home screen, lock screen, Dynamic Island, notification, and desktop. The package arrives. Reserve the right side for YouTube elements.
- **On-screen copy:** `The app can sleep. The experience keeps moving.`
- **Narration:** “The developer writes the delivery state once, tests its hard states at the desk, and lets each platform render the experience where users already look. What would you keep useful after your app closes?”
- **Engagement:** Spoken and visual question. End screen: **Subscribe** plus **Best for viewer**. No generic “like and subscribe” interruption.

## Portrait Short storyboard

The Short is a distinct edit, not a crop of the landscape narration.

### 0:00-0:04 — Hook

- A delivery countdown escapes a phone as its app process powers off.
- Copy: `The app is dead. Why is the ETA still moving?`
- Narration: “The app stopped. The delivery countdown did not.”

### 0:04-0:14 — Problem

- Stack three fast vertical beats: no component tree, no listener, different platform surface APIs.
- Narration: “Widgets live outside your running UI, and Apple, Android, and desktop all give them different rules.”

### 0:14-0:29 — Source and running proof together

- Upper 44 percent: three real lines from the timeline code.
- Lower 48 percent: Widgets Preview.
- Click Publish, then step the timeline while the matching source entry highlights.
- Narration: “Codename One publishes a serializable layout with dated state. The simulator lets us move from Preparing to Delivered before a device build.”

### 0:29-0:42 — Live surface

- Highlight `SurfaceDynamicText`, then start and advance the real mock Dynamic Island.
- Narration: “Native timed text keeps the ETA moving, and the same state feeds a Live Activity, Dynamic Island, Android notification, or desktop surface.”

### 0:42-0:51 — Cold-start tap

- Click the surface; animate `open_order`; show the Order form.
- Narration: “A tap carries an action ID back across the cold start.”

### 0:51-0:58 — Payoff

- App asleep, surfaces alive; package arrives.
- Copy: `One Java model. Useful beyond the app.`
- Narration: “What should your app keep useful after it closes?”

## Required assets and proof

- Actual compiled source: `samples/samples/SurfacesSample/SurfacesSample.java`
- Build-time declaration: `samples/samples/SurfacesSample/surfaces.json`
- Article images:
  - `docs/website/static/blog/widgets-live-activities-dynamic-island.jpg`
  - `docs/website/static/blog/widgets-live-activities-dynamic-island/widget-preview.png`
  - `docs/website/static/blog/widgets-live-activities-dynamic-island/dynamic-island.png`
  - `docs/website/static/blog/widgets-live-activities-dynamic-island/clock-widget.png`
  - `docs/website/static/blog/widgets-live-activities-dynamic-island/sample-form.png`
- New bespoke animation: dead process to living timeline to system surfaces.
- No stock “developer at laptop” image and no repeated person montage.

## Narration and caption audit

- Spoken forms must be generated through the pronunciation layer and listened to before render approval: `WidgetKit`, `ActivityKit`, `RemoteViews`, `Dynamic Island`, `JavaSE`, `ETA`, `API`, `iOS`, and `EDT`.
- Captions retain normal written forms. They must never contain phonetic spellings such as `A. P. I.` or `E. T. A.`.
- Do not use the phrases “shown as evidence,” “the boundary is,” “comes to the rescue,” “three real backends,” or any roadmap/unshipped discussion.
- Narration segments remain mutually exclusive; the audio manifest must prove no overlap and a 120-300 ms conversational gap.

## Approval gates before rendering

1. Human approves the story beats and the exact thesis.
2. The real sample builds and every planned interaction is captured end to end.
3. Pronunciation audition passes for all terms above.
4. A full-resolution frame review passes in both orientations.
5. The final 15 seconds leave a clean end-screen safe area.
