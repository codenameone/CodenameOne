# The Full-Stack Loop — App and Backend Together

Use this when a change spans the app (`common/`) and the server (`backend/`): a new
screen that needs a new endpoint, a form whose data must land in the database, a
bug that could be on either side. The loop runs both halves locally and gives you
**two MCP servers**:

- the **backend's**, which shows its routes, beans, requests, logs and database;
- the **simulator's**, which reads the screen and taps and types for you.

Together they let you check a feature end to end — the button, the request, the
row — without asking a human to click anything.

## 1. Start the backend (it keeps running)

```bash
CN1_PROFILE=dev mvn -pl backend -Dcodename1.platform=backend cn1:backend
```

Run it in the background: it blocks, and it prints

```
cn1: MCP endpoint at http://127.0.0.1:8080/mcp (with development tools)
```

when it is ready. The `dev` profile gives it an in-memory SQLite database with the
`@Entity` tables created, so it needs nothing installed. **There is no hot reload:**
after changing backend code, stop it and run the command again (it takes seconds —
the build regenerates the wiring on the way).

## 2. Connect both MCP servers (once per machine)

```bash
claude mcp add --transport http cn1-backend http://127.0.0.1:8080/mcp
```

For the simulator, follow `references/mcp-agent-control.md`: run
`mvn -pl common cn1:run`, then **MCP -> Expose This Tool To Agents** and
**MCP -> Install in MCP Hosts...**. Restart the MCP host after registering either.

## 3. Point the app at the local backend

Keep the base URL in **one** place in the app (a constant, or a value read at
start-up) so switching between local and deployed is one edit:

| Where the app runs | URL that reaches the local backend |
| --- | --- |
| Simulator (`cn1:run`) | `http://127.0.0.1:8080` |
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://127.0.0.1:8080` |
| A physical phone | `http://<your machine's LAN address>:8080` |
| JavaScript build in a browser | The backend must answer CORS for the page's origin |

## 4. Share the API, don't transcribe it

Declare the API once, as a `@RestClient` interface both sides compile, rather than
writing a client that mirrors a controller by hand. The app gets a typed client;
building the backend with `-Dcn1.restServer=true` gets a synchronous `...Server`
interface to implement and a dispatcher that routes to it. A change to the
interface then breaks whichever side did not follow. The interface has to live
where both modules can compile it -- `backend/` does not depend on `common/`, which
carries the UI -- so it goes in a small module both depend on; the developer
guide's Backend chapter ("Sharing the contract with the app") shows the layout.
See `references/api-clients.md` for the client half and `references/backend.md`
for the server.

## 5. The loop

1. **Learn the server.** `backend_routes` (what exists), `backend_beans` (what is
   wired to what), `backend_schema` (the tables).
2. **Change the backend**, restart it, and **exercise the endpoint directly**
   with `backend_call` before touching the UI. A failing endpoint is much cheaper
   to diagnose here than through a screen.
3. **Change the app**, run it in the simulator, and **drive the flow** over the
   simulator's MCP: `ui_snapshot` -> `ui_find` the field -> `ui_set_text` ->
   `ui_activate` the button -> read the returned snapshot.
4. **Check what the server saw.** `backend_requests` lists the requests the UI
   actually sent, newest first, with status and time; `failuresOnly: true` shows
   only 5xx answers and the exception each one threw. `backend_logs` has the
   console.
5. **Check what was stored.** `backend_sql` with a `SELECT` against the table the
   flow writes.
6. Repeat until the UI, the requests and the rows all agree.

When a step fails, the tools tell you which side to fix:

| Symptom | Look at |
| --- | --- |
| The UI shows an error, `backend_requests` shows nothing | The app's URL (step 3), or the request was never sent |
| `backend_requests` shows 404 | `backend_routes`: path or method mismatch |
| `backend_requests` shows 500 | The `error` and `causes` of that request, then `backend_logs` |
| 200 but the UI shows nothing | The app's parsing of the response: `backend_call` the same request and compare |
| 200 but the row is wrong or missing | `backend_sql`; a rolled-back `@Transactional` method throws, and the request log shows it |

## 6. Tests before "done"

- **Backend**: unit-test services by constructing them; integration-test the wired
  server by starting the generated `BackendWiring` on a free port — both are shown
  in `references/backend.md`.
- **App**: screen tests with `cn1:test` (`references/testing-and-screenshots.md`).
  Keep them independent of a running backend — fake the client, or point it at a
  backend the test itself starts — so the suite does not depend on whatever you
  left running.

## 7. Ship

```bash
mvn -pl backend -Dcodename1.platform=backend cn1:backend-package   # native server binary
```

and the app's platform builds as in `references/build-and-run.md`. Before
deploying, switch the app's base URL to the real server, and give the backend its
production settings through the environment (`DATABASE_URL`, `PORT`,
`cn1.mcp.token` if it publishes `@McpTool` methods). The development MCP tools are
not compiled into the packaged binary.

If you cannot run the backend or the simulator in your environment, **say so** in
your report rather than claiming the flow works.
