# CN1 Device Runtime — privacy

The short version: this app collects nothing, sends nothing anywhere, and talks
only to a computer you have explicitly paired it with.

## What it does on the network

It looks for a computer running the Codename One push tool, on the local network
only, and accepts a connection from one you have paired with. It makes no other
connection of its own.

A program you push may make its own network requests — those are your program's,
under your control, and are not made by the runtime.

## What it stores on the device

- The address of the computer it last spoke to, so it does not have to search again.
- The identity and friendly name of computers you have paired with, and whether
  you chose "Always" for each. **Forget paired computers** deletes all of it.
- The program you pushed most recently, including its source, so it can be shown
  and re-run. It is replaced by the next push.

Nothing is written anywhere else and nothing leaves the device.

## What it does not do

- No analytics, telemetry, crash reporting or advertising identifiers.
- No accounts, no sign-in, no contacts, no location, no camera, no microphone.
- No download of code from any server. Code arrives only from a paired computer.

## Permissions

`INTERNET` and `ACCESS_NETWORK_STATE`, for the local connection to your computer.
On iOS, local network access, which the system prompts for on first use.

## Data safety declarations

Both stores ask; the answer is the same. No data collected, no data shared, no
data linked to the user. The pairing records and the last pushed program stay on
the device and are removable from within the app.
