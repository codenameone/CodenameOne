# Evidence map

Source: `docs/website/content/blog/seamless-crash-protection.md`
Canonical: https://www.codenameone.com/blog/seamless-crash-protection/

## Thesis

From native device crash to scrubbed, symbolicated GitHub issue

## Supported beats

- **Seamless in three ways:** You wire up almost nothing. The build servers do the heavy lifting, including the part that used to be impossible from the client: turning a native crash address into a readable stack.
- **Storage-first delivery on the device:** The client is built so that a crash is not lost to a bad network. Every report is written to Storage with a fresh eventId before the upload is attempted, and the stored copy is deleted only after the server confirms receipt with a 2xx.
- **A real native crash, made readable:** Here is what that produces. The issue below was auto-filed for a native crash on iOS: a NativeCrash, a Signal 11 (SIGSEGV), the sort of failure that is normally an opaque hex address with nothing to act on.
- **Where the issues land: GitHub repo mappings:** Crashes are filed as issues on a GitHub repository you choose, and that repository does not have to be the one where your code lives.
- **Personal data is scrubbed before anything leaves the device:** Reports are scrubbed on the device, before the upload, by a PiiScrubber. The defaults are conservative: email addresses are partially redacted, keeping the first few characters of the local part and the full domain (so joe@example.com), and runs of six or more consecutive digits are collapsed to [num].
- **Turning it on:** Crash protection is opt-in and off by default, and the choice is yours to make: it is persisted in Preferences. Install the handler during startup and enable uploads.

## Referenced evidence

- https://github.com/codenameone/CodenameOne/issues

## Independent problem evidence

- Apple: Adding Identifiable Symbol Names: https://developer.apple.com/documentation/xcode/adding-identifiable-symbol-names-to-a-crash-report — Apple documents symbolication as the step that maps machine addresses back to human-readable function names and source locations.
- OWASP Logging Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html — OWASP logging guidance identifies personal data, tokens, passwords, and secrets that should be removed, masked, or excluded.

## Product proof

- `docs/website/static/blog/seamless-crash-protection/ios-crash-issue.png`
- `docs/website/static/blog/seamless-crash-protection/repo-mappings.png`
