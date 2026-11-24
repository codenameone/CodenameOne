# Deploying the App and Its Backend to Production

Use this when the user asks to deploy, host, ship or "put the app online": the
backend on a VPS (Akamai/Linode, DigitalOcean, Hetzner, Vultr, OVHcloud, AWS
Lightsail/EC2, Google Compute Engine, Azure) or a container platform (ECS/Fargate,
App Runner, Cloud Run, Fly.io, Kubernetes), then the browser, mobile and desktop
apps pointed at it. The developer guide's chapter "Deploying an application end
to end" is the long form; this file is the procedure and the traps.

Copy-ready files are in `templates/deploy/` beside this directory: `build-linux.sh`,
`server-setup.sh`, `myapp.service`, `env.example`, `Caddyfile`, `native-tls.txt`,
`deploy.sh`, `rollback.sh`, `Dockerfile` and `deploy.yml` (GitHub Actions). In each one, replace
`myapp` with the project's name and `app.example.com` with the real domain. The
binary is named after the backend module's **artifactId**: `backend/target/<name>-backend`
in a Maven app with a backend, `target/<name>` in a backend-only project; the
templates copy it to the server as `myapp`.

## What you need from the user first

Ask for these up front, in one message. Don't guess any of them:

1. **Where it runs**: the provider, and either SSH access to a machine (host, user,
   key already loaded in the agent's environment) or a container platform.
2. **The domain** and whether they can create a DNS `A` record for it.
3. **The database**: a managed one (they give you the URL, or create it), PostgreSQL
   on the same box, or a SQLite file for a single small server.
4. **Which clients ship**: browser, iOS, Android, macOS, Windows, Linux.

Things only the user can do, so say so rather than trying: create provider and
store accounts, pay, set DNS at their registrar, enroll in the Apple Developer
Program, and accept store agreements.

## The order

1. Package the backend **for Linux** (below).
2. Prepare the server once (`templates/deploy/server-setup.sh`, as root). It creates
   `myapp` (runs the server) and `deploy` (SSH, copies releases, may only
   `sudo systemctl restart myapp`), and `deploy.sh` connects as `deploy`.
3. Put secrets in `/etc/myapp/env` on the server (`env.example`), never in git.
4. Install `myapp.service`, then `systemctl daemon-reload && systemctl enable myapp`.
5. HTTPS: `Caddyfile` in front, the default, or the server's own TLS (`native-tls.txt`).
6. `cn1:backend-webapp` if the server hosts the browser app, then `deploy.sh`.
7. Verify (below). Only then switch the apps to the HTTPS address and build releases.
8. Optionally, `deploy.yml` so every push to `main` redeploys. It needs its own key:
   `ssh-keygen -t ed25519 -f deploy_key -N ''`, append `deploy_key.pub` to
   `/home/deploy/.ssh/authorized_keys`, store the private half as the `DEPLOY_KEY`
   secret and the checked output of `ssh-keyscan app.example.com` as `KNOWN_HOSTS`.

## Packaging for Linux: the trap

`cn1:backend-package` (Gradle: `backendPackage`) compiles **for the machine it runs
on**. On macOS it produces a macOS binary, which won't run on a Linux server.
`-Dcn1.backend.target=...` does **not** cross-compile; it fails the build on
purpose. So package on Linux:

- From macOS or Windows: `sh .agent-skills/codename-one/templates/deploy/build-linux.sh amd64`
  (or `arm64` for Graviton/Ampere). It needs Podman or Docker. A CPU different
  from the host's runs emulated and takes a long time, so tell the user and run it
  in the background.
- On a Linux machine or CI runner:
  `apt-get install -y openjdk-17-jdk-headless clang libssl-dev libnghttp2-dev libcurl4-openssl-dev`,
  then `./mvnw -pl backend -Dcodename1.platform=backend cn1:backend-package`
  (`./mvnw cn1:backend-package` in a backend-only project).
- **Gradle**: the same, with `./gradlew :backend:backendPackage` (`backendPackage`
  in a backend-only project) and `./gradlew :backend:backendWebApp`. Edit the
  command in `build-linux.sh`, and the binary and `webapp` paths in `deploy.sh`,
  `Dockerfile` and `deploy.yml`, to the subproject's `build/` directory.
- An emulated build (an `amd64` container on Apple silicon) can take most of an
  hour. For an x86-64 server prefer a CI runner (`deploy.yml`) or a Linux x86-64
  machine, and tell the user before starting an emulated one.

The binary is dynamically linked against OpenSSL and nghttp2, and loads libcurl on
the first outbound request. **Build on the same distribution release the server
runs** (the templates use Ubuntu 24.04 everywhere), and install the runtime
libraries on the server: `libssl3t64 libnghttp2-14 libcurl4t64` on Ubuntu 24.04,
`libssl3 libnghttp2-14 libcurl4` on Debian 12. Check with `file backend/target/myapp-backend`
(it should say ELF and the right CPU; the binary is named after the backend module's artifactId) and, on the server, `ldd ./myapp` (no "not found").

## Settings that production needs

These are environment variables in `/etc/myapp/env`, or the platform's secrets.
Every key maps by upper-casing and replacing `.` with `_`:

- `DATABASE_URL`: required outside a dev profile. An unset value is a startup
  error there, not an in-memory DB. Add `?sslmode=require` for a networked database.
- Outside dev the server doesn't create tables and refuses to start if they don't
  match the entities, so the schema must come from **migrations**
  (`references/backend.md`).
- Behind Caddy, nginx or a cloud load balancer: `CN1_SERVER_FORWARDHEADERS=true` and
  `CN1_SERVER_TRUSTEDPROXIES=<proxy addresses>`, plus `CN1_SERVER_ADDRESS=127.0.0.1`
  when the proxy is on the same box, and `CN1_SESSION_SECURE=true` so the session
  cookie is `Secure`. Without these, every client has the proxy's address.
- Health checks: commit `cn1.management.enabled=true` to `application.properties`.
  It is **build-time**: without it the binary has no `/manage/*` endpoints at all.
  Then set `CN1_MANAGEMENT_TOKEN`, which is required outside dev. The probes
  `/manage/health/readiness` and `/manage/health/liveness` need no token.
- The server reads `application.properties` and `webapp/` from its **working
  directory**, which is why the unit sets `WorkingDirectory=/opt/myapp/current`.
- A SQLite file: put it under `/var/lib/myapp` and add `StateDirectory=myapp` to the
  unit. `ProtectSystem=strict` makes everything else read-only.
- The server's own TLS instead of Caddy: install certbot, and **remove**
  `CN1_SERVER_ADDRESS`, `CN1_SERVER_FORWARDHEADERS`, `CN1_SERVER_TRUSTEDPROXIES` and
  `CN1_SESSION_SECURE` from the env file (bound to 127.0.0.1 the server is unreachable).
- The server's own TLS on 443 runs as a non-root user only with
  `AmbientCapabilities=CAP_NET_BIND_SERVICE`. It reads the PEM files at startup,
  so a renewed certificate needs a restart (the certbot hook in `native-tls.txt`).
  Certbot runs that hook only on renewals: run it once by hand after the first
  `certbot certonly`.

## Verify before you report success

Run these, and report what they printed:

```bash
ssh deploy@app.example.com 'systemctl is-active myapp && journalctl -u myapp -n 30 --no-pager'
curl -fsS https://app.example.com/manage/health/readiness      # 200, state UP
curl -fsS https://app.example.com/<one real API route>
curl -fsSI https://app.example.com/ | head -1                  # browser app, if hosted
```

If you could not run a step (no SSH access, DNS not propagated, a build too slow
under emulation), **say so**. Don't claim the deployment works.

Rollback is moving the `current` link back to the previous release and restarting
(the guide's chapter has the one-liner). A schema migration does not roll back
with it, so keep migrations backward compatible for one release.

## Containers and AWS

- `templates/deploy/Dockerfile` runs the Linux binary on `ubuntu:24.04`, the
  release it was built on. `FROM scratch` only works with the fully static musl
  build, which the project's build does not produce.
- The platform sets `PORT` and often `DATABASE_URL`, and the server reads both.
  Point the readiness or target-group health check at `/manage/health/readiness`.
  Set the termination grace period above `cn1.server.shutdownTimeoutMillis`
  (10 s by default).
- **EC2/Lightsail** are the VPS path. Open 80 and 443 in the security group or the
  Lightsail Networking tab as well as `ufw`.
- **Lambda** is a different model (`com.codename1.backend.LambdaRuntime`, a custom
  runtime loop, not an HTTP listener). The templates don't cover it; pick it only
  when the user asks for Lambda, and read the developer guide's backend operations
  chapter first.
- Scaling out to several processes: set `cn1.session.store=db`, and give
  `@Scheduled` jobs a lock so each runs once (see `references/backend.md`).

## Clients

- **Browser**: `./mvnw -pl backend -Dcodename1.platform=backend cn1:backend-webapp`
  stages the browser build into `backend/target/webapp`, and `deploy.sh` copies it
  beside the binary. In a browser, the app should use the page origin,
  `CN.getProperty("browser.window.location.origin", null)`, as its server address.
- **Base URL**: keep the server address in one place in the app. Wayline's
  `AppConfig.serverUrl()` is the pattern (`references/demos.md`). Every release
  build must use `https://`, because iOS ATS and Android 9+ block cleartext.
- **Mobile**: signing, cloud builds, push credentials and store submission go
  through `references/cloud-agent.md` (BuildCloud MCP), and review readiness
  through `references/store-review.md`. The production server must be up with a
  reviewer account **before** submission. Deploy server changes before the app
  release that needs them, and keep old app versions working.
- **Desktop**: native builds `mac-os-x-native` (platform `ios`), `windows-device`
  (platform `win`) and `linux-device` (platform `linux`), per the table in
  `SKILL.md`. macOS downloads need Developer ID signing and notarization. To host
  installers on the same server, set `cn1.static.root` to a directory of them;
  they are served under `/static`.

## Don't

- Don't commit `/etc/myapp/env`, keystores, `.p12` files or tokens. Add
  `.m2-linux/` to `.gitignore`.
- Don't open port 8080 to the internet when a proxy is in front.
- Don't run the server as root.
- Don't point a release build of the app at `http://` or at a LAN or dev address.
