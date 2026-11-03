#!/bin/bash
#
# A cn1lib with a server half, used by a generated application's server.
#
# A cn1lib's backend module holds classes a server wires as its own: the
# library's @RestController gets a route, its @Entity a dao, its @Repository an
# implementation and its @Component a bean, all generated in the SERVER's build
# (see BackendLibraries in build-engine). This builds one of each and asks the
# questions nothing else in CI asks:
#
#   the cn1lib archetype's backend module builds, and the .cn1lib it produces
#     carries backend.zip, marked as a backend library
#   a project from the app archetype that depends on the library's backend jar
#     answers the library's routes, through the library's repository and its
#     migration, in a @BackendTest -- without one line of registration code
#   the server's own code can inject the library's bean
#   all of it again natively, translated by ParparVM
#
# Run with the checkout's artifacts installed (core, backend, backend-test, the
# Maven plugin, both archetypes) into the repository MAVEN_ARGS names.
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
set -e
source "$SCRIPTPATH/inc/env.sh"

WORK="$SCRIPTPATH/build/backend-cn1lib"
GROUP=com.example.backendlib
LIB=notelib
APP=noteapp
mkdir -p "$SCRIPTPATH/build"
if [ -d "$WORK" ]; then
  rm -rf "$WORK"
fi
mkdir -p "$WORK"
cd "$WORK"

fail() { echo "backend-cn1lib-test: $*" >&2; exit 1; }

echo "== the library"
mvn -B archetype:generate \
  -DarchetypeArtifactId=cn1lib-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DartifactId=$LIB \
  -DgroupId=$GROUP \
  -Dversion=1.0-SNAPSHOT \
  -DjavaVersion="$CN1_ARCHETYPE_JAVA_VERSION" \
  -DinteractiveMode=false

SRC="$LIB/backend/src/main/java/com/example/backendlib"
mkdir -p "$SRC" "$LIB/backend/src/main/resources/db/migration"
[ -f "$LIB/backend/src/main/resources/META-INF/codenameone/backend-library" ] \
  || fail "the archetype's backend module has no backend-library marker"

cat > "$LIB/backend/src/main/resources/db/migration/V900__notelib.sql" <<'EOF'
CREATE TABLE notelib_note (id INTEGER PRIMARY KEY, text VARCHAR(200) NOT NULL);
EOF
cat > "$SRC/LibNote.java" <<'EOF'
package com.example.backendlib;

import com.codename1.annotations.Entity;
import com.codename1.annotations.Id;

@Entity(table = "notelib_note")
public class LibNote {
    @Id
    public long id;
    public String text;
}
EOF
cat > "$SRC/LibNotes.java" <<'EOF'
package com.example.backendlib;

import com.codename1.orm.repository.CrudRepository;
import com.codename1.orm.repository.Repository;

@Repository(entity = LibNote.class, key = Long.class)
public interface LibNotes extends CrudRepository<LibNote, Long> {
}
EOF
cat > "$SRC/NoteService.java" <<'EOF'
package com.example.backendlib;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;

@Component
public class NoteService {
    private final LibNotes notes;

    public NoteService(LibNotes notes) {
        this.notes = notes;
    }

    @Transactional
    public long add(String text) {
        LibNote note = new LibNote();
        note.text = text;
        notes.insert(note);
        return note.id;
    }

    @Transactional
    public long count() {
        return notes.count();
    }
}
EOF
cat > "$SRC/NoteEndpoint.java" <<'EOF'
package com.example.backendlib;

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;

@RestController
@RequestMapping("/notelib")
public class NoteEndpoint {
    private final NoteService notes;

    public NoteEndpoint(NoteService notes) {
        this.notes = notes;
    }

    @PostMapping("/notes")
    public String add(@RequestBody String text) {
        return String.valueOf(notes.add(text));
    }

    @GetMapping("/notes/count")
    public String count() {
        return String.valueOf(notes.count());
    }
}
EOF

(cd "$LIB" && mvn -B install -DskipTests)

CN1LIB="$(ls "$LIB"/common/target/*.cn1lib | head -n 1)"
[ -f "$CN1LIB" ] || fail "no .cn1lib was produced"
unzip -l "$CN1LIB" | grep -q " backend.zip$" || fail "$CN1LIB has no backend.zip"
unzip -p "$CN1LIB" backend.zip > "$WORK/backend.zip"
unzip -l "$WORK/backend.zip" | grep -q "META-INF/codenameone/backend-library" \
  || fail "backend.zip is not marked as a backend library"
unzip -l "$WORK/backend.zip" | grep -q "com/example/backendlib/NoteEndpoint.class" \
  || fail "backend.zip does not hold the library's classes"
if unzip -l "$WORK/backend.zip" | grep -q "Router.class\|cn1app/"; then
  fail "backend.zip holds generated wiring; the server generates it"
fi

echo "== the application"
mvn -B archetype:generate -DplatformModules=all -DprojectType=app-with-backend \
  -DarchetypeArtifactId=cn1app-archetype \
  -DarchetypeGroupId=com.codenameone \
  -DarchetypeVersion="$CN1_VERSION" \
  -DartifactId=$APP \
  -DgroupId=com.example.noteapp \
  -Dversion=1.0-SNAPSHOT \
  -DmainName=NoteApp \
  -DjavaVersion="$CN1_ARCHETYPE_JAVA_VERSION" \
  -DinteractiveMode=false

python3 - "$APP/backend/pom.xml" "$GROUP" "$LIB" <<'PY'
import sys
path, group, lib = sys.argv[1:]
text = open(path).read()
marker = '<!-- INJECT DEPENDENCIES -->'
if marker not in text:
    sys.exit('the app archetype backend pom has no ' + marker)
dependency = ('<dependency>\n  <groupId>%s</groupId>\n  <artifactId>%s-backend</artifactId>\n'
              '  <version>1.0-SNAPSHOT</version>\n</dependency>\n' % (group, lib))
open(path, 'w').write(text.replace(marker, dependency + marker, 1))
PY

APP_PKG_DIR="$(dirname "$(grep -rl "@RestController" "$APP/backend/src/main/java" | head -n 1)")"
APP_PKG="$(sed -n 's/^package \(.*\);/\1/p' "$(grep -rl "@RestController" "$APP/backend/src/main/java" | head -n 1)")"
TEST_DIR="$APP/backend/src/test/java/$(echo "$APP_PKG" | tr '.' '/')"
mkdir -p "$TEST_DIR"
# The server's own bean, built on the library's.
cat > "$APP_PKG_DIR/NoteCounter.java" <<EOF
package $APP_PKG;

import com.codename1.backend.annotations.Component;
import com.example.backendlib.NoteService;

@Component
public class NoteCounter {
    private final NoteService notes;

    public NoteCounter(NoteService notes) {
        this.notes = notes;
    }

    public long twice() {
        return notes.count() * 2;
    }
}
EOF
cat > "$TEST_DIR/LibraryWiringTest.java" <<EOF
package $APP_PKG;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@BackendTest
class LibraryWiringTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private NoteCounter counter;

    @Test
    void theLibrarysRoutesBeansAndTablesAreTheServers() throws Exception {
        mvc.perform(post("/notelib/notes").content("first")).andExpect(status().isOk());
        mvc.perform(post("/notelib/notes").content("second")).andExpect(status().isOk());
        String count = mvc.perform(get("/notelib/notes/count")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals("2", count.replace("\"", "").trim());
        assertEquals(4, counter.twice());
    }
}
EOF

(cd "$APP" && mvn -B -pl backend -am -Dcodename1.platform=backend verify)

ls "$APP"/backend/target/surefire-reports/TEST-*LibraryWiringTest.xml >/dev/null 2>&1 \
  || fail "LibraryWiringTest did not run"
[ -f "$APP/backend/target/classes/com/example/backendlib/NoteEndpointRouter.class" ] \
  || fail "the server generated no router for the library's controller"

# The same tests translated by ParparVM and run natively: the native packager
# merges the library through its own compile, not process-annotations, and
# stages the library jar for its natives only. Needs clang and the backend's
# native headers; CN1_BACKEND_PACKAGE_REQUIRED=1 (CI) makes their absence an
# error instead of a skip.
if command -v clang >/dev/null 2>&1; then
  (cd "$APP" && mvn -B -pl backend -Dcodename1.platform=backend -Dcn1.backend.compiledTests=true verify) \
    | tee "$WORK/native.log"
  grep -q "compiled tests: [0-9]* passed, 0 failed" "$WORK/native.log" \
    || fail "the natively compiled tests did not all pass"
elif [ "${CN1_BACKEND_PACKAGE_REQUIRED:-}" = "1" ]; then
  fail "clang is not installed, and CN1_BACKEND_PACKAGE_REQUIRED=1"
else
  echo "backend-cn1lib-test: no clang; the native leg is skipped"
fi

echo "backend-cn1lib-test: OK"
