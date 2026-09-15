#!/usr/bin/env python3
"""Black-box canary for the Codename One cloud onboarding starter.

Does exactly what a new user does, against the live build service:

  1. sign in and pull the personalised starter ZIP from the console (the same
     session-cookie endpoint the onboarding checklist links to)
  2. unzip it into a path containing a space and an apostrophe
  3. assert the properties that have silently broken before -- launcher execute
     bits, and a <repositories>/<pluginRepositories> pair that can actually
     resolve Codename One
  4. run the SHIPPED launcher (build.sh / build.bat) with a real Maven and
     require the cloud build to reach a terminal success

Why a live end-to-end check and not a unit test: the starter a user receives is
generated and personalised server-side, then resolves its dependencies over the
network on the user's own machine. Neither of those is visible to a test that
reads the template out of a repository or replaces Maven with a stub, and both
have broken in ways that made the first build impossible while every offline
check stayed green:

  * the generated pom declared no dependency repository at all, so once
    Codename One releases moved off Maven Central a pinned starter could not
    resolve anything
  * the ZIP shipped build.sh/run.sh/mvnw without the execute bit, so the first
    command the README documents failed with "permission denied"
  * build.bat fell through to producing a local jar when given no target, so
    the user got no cloud build and no error either

Each survived for weeks because nothing exercised the artefact a user actually
receives. This runs nightly and opens an issue when it breaks.
"""
import argparse
import json
import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from http.cookiejar import CookieJar
from pathlib import Path

WINDOWS = os.name == "nt"
# A directory whose name breaks naive quoting. A shipped mvnw.cmd once
# interpolated the path into PowerShell source, so a space or an apostrophe was
# a build failure; keep reproducing that shape here.
AWKWARD = "Project O'Brien with spaces"

# An allowlist, not a blocklist. The launchers expose ios, ios_release,
# ios_source, xcode, mac_native and mac_catalyst, all of which need a Mac host
# and cost several times a normal build -- and a blocklist would have to be
# updated every time another one is added. These are the cheap, non-Apple
# targets a canary has any reason to ask for.
CHEAP_TARGETS = ("javascript", "windows_device", "windows_desktop",
                 "linux_device", "android", "android_source")


class CanaryFailure(RuntimeError):
    """A user-visible breakage. The message becomes the GitHub issue body."""


REPORT_PATH = None


def log(message):
    print(f"[canary] {message}", flush=True)


def build_opener():
    jar = CookieJar()
    return urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(jar),
        urllib.request.HTTPRedirectHandler(),
    ), jar


def fetch(opener, url, data=None, headers=None):
    request = urllib.request.Request(url, data=data, headers=headers or {})
    request.add_header("User-Agent", "cn1-starter-canary")
    try:
        with opener.open(request, timeout=120) as response:
            return response.status, response.read(), response.headers, response.url
    except urllib.error.HTTPError as error:
        return error.code, error.read(), error.headers, url


def login(opener, base, email, password):
    """Sign in through the ordinary HTML form, exactly as a person does."""
    status, body, _, _ = fetch(opener, f"{base}/login")
    if status != 200:
        raise CanaryFailure(f"GET /login returned HTTP {status}; production may be down")
    token = extract_csrf(body.decode("utf-8", "replace"))
    form = {"username": email, "password": password}
    if token:
        form[token[0]] = token[1]
    status, body, _, final = fetch(
        opener,
        f"{base}/login",
        data=urllib.parse.urlencode(form).encode(),
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    if status not in (200, 302) or "error" in urllib.parse.urlparse(final).query:
        raise CanaryFailure(
            f"form login as {email} failed (HTTP {status}, landed on {final}). "
            "Either the canary credentials are wrong or the sign-in path is broken."
        )
    return final


def extract_csrf(html):
    match = re.search(
        r'<input[^>]+name="(_csrf[^"]*)"[^>]+value="([^"]*)"', html
    ) or re.search(
        r'<input[^>]+value="([^"]*)"[^>]+name="(_csrf[^"]*)"', html
    )
    if not match:
        return None
    a, b = match.group(1), match.group(2)
    return (a, b) if a.startswith("_csrf") else (b, a)


def download_starter(opener, base, target, destination):
    url = f"{base}/api/v2/console/onboarding/starter.zip?source=canary"
    if target:
        url += f"&target={urllib.parse.quote(target)}"
    status, body, headers, _ = fetch(opener, url)
    if status == 403:
        raise CanaryFailure(
            "starter.zip returned 403 -- the session did not carry through login. "
            "A signed-in user cannot download the starter."
        )
    if status != 200:
        raise CanaryFailure(f"starter.zip returned HTTP {status}")
    kind = (headers.get("Content-Type") or "").lower()
    if body[:2] != b"PK":
        raise CanaryFailure(
            f"starter.zip was not a ZIP (Content-Type {kind!r}, {len(body)} bytes). "
            "The generator is serving something else -- most likely an error page."
        )
    destination.write_bytes(body)
    log(f"downloaded starter.zip ({len(body)} bytes)")
    return destination


def unpack(archive, into):
    """Unzip preserving the unix mode, which is the whole point of the check."""
    into.mkdir(parents=True, exist_ok=True)
    modes = {}
    try:
        with zipfile.ZipFile(archive) as zf:
            zf.extractall(into)
            for info in zf.infolist():
                mode = info.external_attr >> 16
                if mode:
                    modes[info.filename] = mode
    except (zipfile.BadZipFile, OSError) as error:
        raise CanaryFailure(
            f"the served starter could not be unpacked: {error}. "
            "A user who clicks Download gets a file that will not open."
        ) from error
    for name, mode in modes.items():
        path = into / name
        if path.exists() and not path.is_dir():
            path.chmod(mode & 0o7777)
    roots = [p for p in into.iterdir() if p.is_dir()]
    if len(roots) != 1:
        raise CanaryFailure(
            f"expected exactly one directory inside starter.zip, found {[p.name for p in roots]}"
        )
    return roots[0], modes


def check_launcher_bits(project, modes):
    """The launchers must be executable in the ZIP the server hands out."""
    if WINDOWS:
        # Windows has no unix mode; read what the ZIP recorded instead, which is
        # what a macOS or Linux user would actually get.
        broken = [
            name for name, mode in modes.items()
            if Path(name).name in ("build.sh", "run.sh", "mvnw") and not (mode & 0o111)
        ]
    else:
        broken = []
        for name in ("build.sh", "run.sh", "mvnw"):
            path = project / name
            if path.exists() and not (path.stat().st_mode & stat.S_IXUSR):
                broken.append(name)
    if broken:
        raise CanaryFailure(
            f"the served starter ZIP has non-executable launchers: {', '.join(sorted(broken))}. "
            "Every macOS/Linux user gets 'permission denied' on the first documented command. "
            "Check that the server-side generator sets a unix mode on the launchers."
        )
    log("launcher execute bits OK")


def check_repositories(project):
    """A pinned release with no repository declared resolves nothing."""
    pom = (project / "pom.xml").read_text(encoding="utf-8", errors="replace")
    version = None
    match = re.search(r"<cn1\.version>\s*([^<\s]+)\s*</cn1\.version>", pom) \
        or re.search(r"<codenameone\.version>\s*([^<\s]+)\s*</codenameone\.version>", pom)
    if match:
        version = match.group(1)
    missing = [
        block for block in ("repositories", "pluginRepositories")
        if f"<{block}>" not in pom
    ]
    if missing:
        raise CanaryFailure(
            f"the served starter pom.xml declares no <{'> and no <'.join(missing)}>. "
            "Codename One left Maven Central at 7.0.268, so a pin past 7.0.267 resolves "
            "nothing on a user's machine."
        )
    if "repo.codenameone.com" not in pom:
        raise CanaryFailure(
            "the served starter pom.xml declares repositories but none pointing at "
            "repo.codenameone.com -- releases past 7.0.267 live only there."
        )
    if version and version <= "7.0.267":
        log(f"WARNING: starter pins cn1 {version}, at or below the Maven Central freeze point")
    log(f"repository declarations OK (pinned version: {version or 'unknown'})")
    return version


def seed_token(project, mvn, email, token, version):
    """Headless build-client auth, so no browser OAuth is needed in CI.

    The goal writes into the java Preferences node the build client reads, so it
    needs no project state -- but it does need an explicit plugin version.
    An unversioned groupId:artifactId:goal makes Maven resolve LATEST, which is
    precisely the sort of implicit resolution this canary exists to catch.
    """
    if not version:
        raise CanaryFailure(
            "could not read the Codename One version out of the served starter pom, "
            "so the build client cannot be authenticated with a pinned plugin."
        )
    run(
        [mvn, "-B", "-q",
         f"com.codenameone:codenameone-maven-plugin:{version}:set-user-token",
         f"-Dtoken={token}", f"-Duser={email}"],
        cwd=project,
        what="cn1:set-user-token",
        secret=token,
    )
    log("seeded build-client token")


def run(command, cwd, what, timeout=3600, secret=None, check=True):
    result = subprocess.run(
        command, cwd=str(cwd), capture_output=True, text=True, timeout=timeout
    )
    output = (result.stdout or "") + (result.stderr or "")
    if secret:
        output = output.replace(secret, "***")
    if check and result.returncode != 0:
        raise CanaryFailure(
            f"{what} failed with exit {result.returncode}:\n{tail(output)}"
        )
    return result.returncode, output


def tail(text, lines=40):
    rows = [r for r in text.splitlines() if r.strip()]
    return "\n".join(rows[-lines:])


def launch(project, target):
    """Run the launcher the README tells the user to run -- not mvn directly."""
    if WINDOWS:
        command = ["cmd", "/c", "build.bat", target]
    else:
        launcher = project / "build.sh"
        if not os.access(launcher, os.X_OK):
            raise CanaryFailure("build.sh is present but not executable")
        command = ["./build.sh", target]
    env_note = f"{'build.bat' if WINDOWS else './build.sh'} {target}"
    log(f"running {env_note} (this downloads Maven and the CN1 toolchain; several minutes)")
    code, output = run(
        command, cwd=project, what=env_note, timeout=3600, check=False
    )
    return code, output


def list_builds(opener, base):
    """Authoritative build state, straight from the console API.

    Matching launcher stdout for phrases like "sent to the build server" would
    be guesswork -- the upload client ships as a binary dependency, so its exact
    wording is not in this repository and could change without notice. The
    console's own build list is what the user sees in the web UI, so assert
    against that instead.
    """
    status, body, _, _ = fetch(opener, f"{base}/api/v2/console/builds")
    if status != 200:
        raise CanaryFailure(f"GET /api/v2/console/builds returned HTTP {status}")
    try:
        return json.loads(body.decode("utf-8", "replace")).get("builds", [])
    except ValueError as error:
        raise CanaryFailure(f"the console build list was not JSON: {error}") from error


TERMINAL = {"success", "failed", "cancelled"}


def await_cloud_build(opener, base, known_ids, target, launcher_code, output,
                      timeout=1800, interval=20):
    """Wait for a build this run submitted to reach a terminal state."""
    deadline = time.time() + timeout
    seen = None
    while time.time() < deadline:
        fresh = [b for b in list_builds(opener, base) if b.get("id") not in known_ids]
        if fresh:
            fresh.sort(key=lambda b: b.get("submittedAt") or 0)
            seen = fresh[-1]
            if (seen.get("status") or "").lower() in TERMINAL:
                break
        time.sleep(interval)

    if seen is None:
        # The launcher "succeeded" locally and nothing was ever submitted, so
        # the user is left holding a jar and no cloud build.
        raise CanaryFailure(
            f"running the documented launcher for '{target}' never created a build on the "
            f"server (launcher exit {launcher_code}). A new user following the README gets "
            f"no cloud build.\n\n{tail(output, 60)}"
        )

    state = (seen.get("status") or "unknown").lower()
    if state == "success":
        log(f"cloud build {seen.get('id')} for '{target}' finished: success")
        return seen
    if state in TERMINAL:
        raise CanaryFailure(
            f"the cloud build for '{target}' finished as '{state}' "
            f"(build {seen.get('id')}): {seen.get('message') or 'no message'}"
        )
    raise CanaryFailure(
        f"the cloud build for '{target}' (build {seen.get('id')}) was still '{state}' "
        f"after {timeout}s"
    )


def check_target_is_cloud(project, target):
    """Confirm the served launcher maps this target to a cloud build.

    Target names are not portable between launchers: the project archetype maps
    `javascript` to `local-javascript` and keeps a separate `javascript_cloud`,
    while the starter served by the console maps `javascript` straight to the
    cloud target. Reading the launcher we were actually handed is the only way
    to be sure -- and without this the canary would spend the full build poll
    waiting for a build that was never going to be submitted, then report the
    starter as broken when the real fault is the target name.
    """
    launcher = project / ("build.bat" if WINDOWS else "build.sh")
    if not launcher.exists():
        raise CanaryFailure(f"the served starter has no {launcher.name}")
    text = launcher.read_text(encoding="utf-8", errors="replace")
    marker = f":{target}" if WINDOWS else f"function {target}"
    if marker not in text:
        offered = re.findall(r"^:([a-z_0-9]+)" if WINDOWS else r"^function ([a-z_0-9]+)",
                             text, re.MULTILINE)
        raise CanaryFailure(
            f"the served {launcher.name} has no '{target}' target. It offers: "
            f"{', '.join(sorted(set(offered))) or 'nothing recognisable'}."
        )
    body = text.split(marker, 1)[1][:400]
    if "local-" in body:
        raise CanaryFailure(
            f"'{target}' maps to a LOCAL build in the served {launcher.name} "
            f"(buildTarget contains 'local-'), so it would never submit anything. "
            "Point the canary at the launcher's cloud target instead."
        )
    log(f"'{target}' is a cloud target in the served {launcher.name}")


def find_maven(project):
    """Prefer the shipped wrapper -- that is what a real user runs."""
    wrapper = project / ("mvnw.cmd" if WINDOWS else "mvnw")
    if wrapper.exists():
        return str(wrapper)
    found = shutil.which("mvn")
    if not found:
        raise CanaryFailure("neither the shipped mvnw nor a system mvn is available")
    return found


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="https://cloud.codenameone.com")
    parser.add_argument("--target", default="javascript",
                        help="build target; never use iphone/macos -- those cost 8 credits")
    parser.add_argument("--report", help="write a JSON result here")
    parser.add_argument("--skip-build", action="store_true",
                        help="artefact checks only; do not submit a cloud build")
    args = parser.parse_args()
    global REPORT_PATH
    REPORT_PATH = args.report or os.environ.get("CANARY_REPORT")

    email = os.environ.get("CN1_CANARY_EMAIL", "").strip()
    password = os.environ.get("CN1_CANARY_PASSWORD", "").strip()
    token = os.environ.get("CN1_CANARY_TOKEN", "").strip()
    if not email or not password:
        raise CanaryFailure(
            "CN1_CANARY_EMAIL and CN1_CANARY_PASSWORD are not set; the canary cannot sign in."
        )
    if args.target not in CHEAP_TARGETS:
        raise CanaryFailure(
            f"refusing target '{args.target}': the canary only submits cheap, non-Apple "
            f"builds ({', '.join(CHEAP_TARGETS)}). Apple targets need a Mac host and cost "
            "several times a normal build, which a nightly run would not survive."
        )

    base = args.base.rstrip("/")
    started = time.time()
    opener, _ = build_opener()

    log(f"signing in to {base} as {email}")
    login(opener, base, email, password)

    with tempfile.TemporaryDirectory(prefix="cn1-canary-") as tmp:
        root = Path(tmp)
        archive = download_starter(opener, base, args.target, root / "starter.zip")
        project, modes = unpack(archive, root / AWKWARD)
        log(f"unpacked to {project}")

        check_launcher_bits(project, modes)
        version = check_repositories(project)
        check_target_is_cloud(project, args.target)

        if args.skip_build:
            log("--skip-build set; stopping after artefact checks")
            result = {"ok": True, "stage": "artefact", "cn1Version": version}
        else:
            if not token:
                raise CanaryFailure(
                    "CN1_CANARY_TOKEN is not set; cannot authenticate the build client headlessly."
                )
            mvn = find_maven(project)
            seed_token(project, mvn, email, token, version)
            # Snapshot first: a free account keeps only its most recent build,
            # so "is there a new id" is the only safe way to spot this run's.
            known = {b.get("id") for b in list_builds(opener, base)}
            code, output = launch(project, args.target)
            build = await_cloud_build(opener, base, known, args.target, code, output)
            result = {
                "ok": True,
                "stage": "build",
                "cn1Version": version,
                "target": args.target,
                "buildId": build.get("id"),
                "seconds": round(time.time() - started),
            }

    if REPORT_PATH:
        Path(REPORT_PATH).write_text(json.dumps(result, indent=2))
    log(f"PASS in {round(time.time() - started)}s")


if __name__ == "__main__":
    try:
        main()
    except CanaryFailure as failure:
        message = str(failure)
        print(f"[canary] FAIL: {message}", file=sys.stderr, flush=True)
        report = REPORT_PATH or os.environ.get("CANARY_REPORT")
        if report:
            Path(report).write_text(json.dumps({"ok": False, "error": message}, indent=2))
        sys.exit(1)
