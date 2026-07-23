# Positioning red-team: AR And VR In Java: ARKit, ARCore, And A Virtual Room You Can Debug

## Verdict

SHIP

## Thesis

AR and VR development fragments an ordinary application feature across APIs, languages, tools, devices, sensors, and physical environments. Codename One collapses the application work into one Java codebase and makes difficult room states controllable in the JavaSE simulator.

## Act 1 audit

- The first sentence says what Codename One is: native apps in Java from one codebase.
- The topic is named in full: augmented reality and virtual reality.
- The logo is a small signature. The topic-specific spatial graphic and title carry the frame.
- The splash makes no feature claim beyond the basic platform identity.

## Act 2 audit

- Codename One is absent after the splash until the intervention.
- The problem is built before the solution: different APIs, languages, tools, device loops, hardware inputs, and simulator requirements.
- The difficulty is concrete rather than adjectival: the camera, sensors, room, architecture, runtime, and device are part of reproduction.
- A hostile newcomer can explain why AR development is hard without knowing Codename One exists.

## Act 3 audit

- Every act-two row has a matching resolution: API, language, tools, debugging, simulation, and hardware support.
- The resolution is shown before it is celebrated: exact Java code, a compiled AR session, forced tracking loss and recovery, anchored-model changes, and a compiled Media360View.
- The portability claim is limited to the shipped native ARKit and ARCore backends plus JavaSE simulation described in the source.
- The developer remains the protagonist; the script never announces that Codename One “comes to the rescue.”

## Evidence audit

- The Stack Overflow card attributes a public question and accepted answer about ARKit simulator limits.
- The Reddit card attributes a practitioner complaint about cross-platform AR debugging.
- The Google card summarizes official emulator prerequisites and links the exact documentation.
- Product proof comes from repository source and compiled demo code, not a generated claim card.

## Interaction audit

- The old picture-drag treatment is removed.
- `ARDemoScene` opens the actual AR API and mutates anchors, nodes, and JavaSE tracking state.
- `Media360DemoScene` mounts the actual component and changes yaw, pitch, stereo, and recenter state.
- No pointer choreography remains; the running application state and status readouts show each action.

## Rejected versions

The previous desire-first cut failed this contract because it introduced the product before establishing the industry problem, treated screenshot movement as interaction, and had no point-by-point problem-to-resolution map. It would receive `REWRITE`, not `SHIP`, under the current gate.

## Highest-leverage risk

The independent evidence is rendered as attributed evidence cards rather than literal browser screenshots. Do not let their polish make them look like invented testimonials: retain the source name, short quotation or factual summary, and visible URL in every card.
