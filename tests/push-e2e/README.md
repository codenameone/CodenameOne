# Push delivery contract tests

Push cannot be proven end-to-end in hosted CI because CI has no installed application, OS push
daemon, user permission state, or physical device. The automated strategy therefore places fakes
at the lowest boundary controlled by Codename One and treats the native provider/OS hop as an
explicit blind spot.

## Client CI

The normal `core-unittests` suite runs `PushClientTransportTest`. Its fake `PushTransport` drives
the same callback boundary used by native ports and covers registration, application-owned token
storage, foreground/background messages, cold-start replay, malformed envelopes, surface
updates, and unregistration. It performs no BuildCloud call and needs no provider credential.

`service-worker-contract.mjs` executes the real JavaScript service-worker source inside a mocked
service-worker environment. It checks focused, background, and silent delivery at the browser's
lowest controllable boundary, including duplicate-callback and notification-display behavior.
PR CI runs it with Node after the Java 8 unit suite.

Port-specific native code should keep parsing and lifecycle decisions in small callable seams so
the port build can inject these cases without a provider:

* token registration, rotation, rejection, and unregistration;
* foreground, background, force-stopped/cold-start replay, and duplicate delivery;
* visible, silent, collapsed, expired, malformed, and maximum-size envelopes;
* notification open/action/deep-link routing;
* widget and live-activity commands before the application listener;
* permission denied/revoked and provider registration errors.

These tests verify Codename One's behavior. They don't claim that APNs, FCM, Huawei, WNS, or a
browser vendor delivered a message.

## Private backend suite

BuildCloud has its own local Maven tests, independent of client CI. Provider HTTP clients are
tested against mocks or local HTTP fakes; quota/rate rows, durable claims, lease recovery, retries,
legacy `/push/push` normalization, and Enterprise automation matching are tested without real
credentials. This suite is intended to run locally before backend deployment; it need not incur a
hosted private-repository CI run on every client change.

## Optional manual diagnostic

`scripts/run-push-e2e.sh` remains an operator-run staging diagnostic when physical devices and
credentials happen to be available. It is not a CI gate or a nightly requirement. Provider
acceptance is never reported as proof of device delivery.
