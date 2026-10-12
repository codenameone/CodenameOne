/* Runs unchanged on POSIX and Windows (including Wine). Tests the exact OS layer
 * used by the translated adapter, without requiring a desktop window system. */
#include "cn1_process.h"
#include <stdio.h>
#ifdef _WIN32
#define pause_test() Sleep(1)
#else
#define pause_test() usleep(1000)
#endif
#define CHECK(value, message) do { if (!(value)) { fprintf(stderr, "FAIL: %s at %d\n", message, __LINE__); exit(1); } } while (0)
static cn1p_child *start(char **args, const char *cwd, char **env, int merge) {
    int error = 0;
    cn1p_child *p = cn1p_launch(args, cwd, env, merge, &error);
    if (!p) fprintf(stderr, "launch OS error %d\n", error);
    CHECK(p != NULL, "launch"); return p;
}
static int collect(cn1p_child *p, int pipe, unsigned char *out, int capacity) {
    int total = 0, attempts;
    for (attempts = 0; attempts < 30000; ++attempts) {
        int n = cn1p_transfer(p, pipe, out + total, capacity - total);
        if (n == -1) return total;
        CHECK(n != -2, "read");
        if (n >= 0) { total += n; CHECK(total < capacity, "buffer size"); }
        else pause_test();
    }
    CHECK(0, "read timeout"); return 0;
}
static void reap(cn1p_child *p, int code) {
    int i;
    for (i = 0; i < 30000 && !cn1p_poll(p); ++i) pause_test();
    CHECK(cn1p_poll(p), "reap timeout");
    if (code >= 0) CHECK(p->code == code, "exit status");
    cn1p_release(p);
}
static int resource_count(void) {
#ifdef _WIN32
    DWORD count = 0;
    CHECK(GetProcessHandleCount(GetCurrentProcess(), &count), "handle count");
    return (int)count;
#else
    int count = 0, fd;
    for (fd = 0; fd < 1024; ++fd) if (fcntl(fd, F_GETFD) >= 0) ++count;
    return count;
#endif
}
static int run(int argc, char **argv) {
    unsigned char buffer[8192];
    char *literals[] = {NULL, "args", "", "two words", "quote\"here", "\\\\trailing\\", "a\\\"b", "שלום-🙂", "$HOME;|&<>*?", NULL};
    char *command[] = {NULL, "env", NULL, NULL};
    char *env[] = {"CN1_PROCESS_TEST=changed value", NULL};
    cn1p_child *p;
    int length, i, offset = 0, error, merged;
    CHECK(argc == 3, "fixture and cwd required");
    literals[0] = command[0] = argv[1];
    p = start(literals, NULL, NULL, 0);
    length = collect(p, 1, buffer, sizeof(buffer));
    for (i = 2; literals[i]; ++i) {
        size_t n = strlen(literals[i]) + 1;
        CHECK(offset + (int)n <= length && !memcmp(buffer + offset, literals[i], n), "literal arguments");
        offset += (int)n;
    }
    CHECK(offset == length, "argument byte count"); reap(p, 0);
    p = start(command, NULL, env, 0);
    length = collect(p, 1, buffer, sizeof(buffer));
    CHECK(length == 13 && !memcmp(buffer, "changed value", 13), "environment"); reap(p, 0);
    env[0] = NULL;
    p = start(command, NULL, env, 0);
    length = collect(p, 1, buffer, sizeof(buffer));
    CHECK(length == 8 && !memcmp(buffer, "<absent>", 8), "empty environment"); reap(p, 0);
    command[1] = "cwd";
    p = start(command, argv[2], NULL, 0);
    length = collect(p, 1, buffer, sizeof(buffer)); buffer[length] = 0;
    CHECK(!strcmp((char *)buffer, argv[2]), "working directory"); reap(p, 0);
    CHECK(!cn1p_launch(command, "cn1-no-such-directory", NULL, 0, &error) && error, "missing cwd");
    command[0] = "cn1-no-such-executable";
    CHECK(!cn1p_launch(command, NULL, NULL, 0, &error) && error, "missing executable");
    command[0] = argv[1];
    for (merged = 0; merged < 2; ++merged) {
        int sent = 0, out = 0, err = 0, eofOut = 0, eofErr = 0, iterations, stdinClosed = 0;
        unsigned char input[4096];
        command[1] = "duplex";
        for (i = 0; i < (int)sizeof(input); ++i) input[i] = (unsigned char)i;
        p = start(command, NULL, NULL, merged);
        for (iterations = 0; iterations < 30000 && !(eofOut && eofErr); ++iterations) {
            int n, j;
            if (sent < 262144) {
                n = cn1p_transfer(p, 0, input, sizeof(input)); CHECK(n > 0 || n == -3, "stdin write");
                if (n > 0) sent += n;
            } else if (!stdinClosed) { cn1p_close_pipe(p, 0); stdinClosed = 1; }
            if (!eofOut) {
                n = cn1p_transfer(p, 1, buffer, 4096); CHECK(n != -2, "stdout read");
                if (n == -1) eofOut = 1;
                if (n > 0) for (j = 0; j < n; ++j, ++out) {
                    int prefix = 524288 * (merged + 1);
                    CHECK(buffer[j] == (out < prefix ? 37 : (unsigned char)(out - prefix)), "binary output");
                }
            }
            if (!eofErr) {
                n = cn1p_transfer(p, 2, buffer, 4096); CHECK(n != -2, "stderr read");
                if (n == -1) eofErr = 1;
                if (n > 0) { for (j = 0; j < n; ++j) CHECK(buffer[j] == 37, "stderr bytes"); err += n; }
            }
            pause_test();
        }
        CHECK(eofOut && eofErr && out == 524288 * (merged + 1) + 262144 && err == (merged ? 0 : 524288), "duplex length/timeout");
        reap(p, 0);
    }
    command[1] = "sleep";
    p = start(command, NULL, NULL, 0);
    CHECK(!cn1p_poll(p), "sleep alive");
    CHECK(cn1p_transfer(p, 1, buffer, 4096) == -3, "pending read");
    cn1p_close_pipe(p, 1); cn1p_close_pipe(p, 1);
    cn1p_terminate(p, 1); reap(p, -1);
    command[1] = "exit"; command[2] = "7";
    { int before = resource_count();
      for (i = 0; i < 40; ++i) { p = start(command, NULL, NULL, 0); reap(p, 7); }
      CHECK(resource_count() == before, "descriptor/handle leak across repeated launches");
    }
    p = start(command, NULL, NULL, 0);
    for (i = 0; i < 30000 && !cn1p_poll(p); ++i) pause_test();
    CHECK(cn1p_poll(p), "exit before broken-pipe test");
    { int n = -3;
      // Windows may accept a buffered write while it finishes closing the
      // exited child's handles. Observe the eventual pipe error.
      for (i = 0; i < 30000 && n != -2; ++i) {
          n = cn1p_transfer(p, 0, buffer, 1);
          if (n != -2) pause_test();
      }
      CHECK(n == -2, "closed stdin must fail without SIGPIPE");
    }
#ifndef _WIN32
    { int status;
      CHECK(waitpid(p->pid, &status, WNOHANG) == -1 && errno == ECHILD, "child was not reaped");
    }
#endif
    cn1p_release(p);
    puts("PROCESS_OS_CONFORMANCE_OK"); return 0;
}

#ifdef _WIN32
int wmain(int argc, wchar_t **wide) {
    int i, code;
    char **argv = (char **)calloc((size_t)argc + 1, sizeof(char *));
    CHECK(argv != NULL, "argument allocation");
    for (i = 0; i < argc; ++i) {
        int n = WideCharToMultiByte(CP_UTF8, 0, wide[i], -1, NULL, 0, NULL, NULL);
        argv[i] = (char *)malloc((size_t)n);
        CHECK(argv[i] != NULL, "argument allocation");
        WideCharToMultiByte(CP_UTF8, 0, wide[i], -1, argv[i], n, NULL, NULL);
    }
    code = run(argc, argv);
    for (i = 0; i < argc; ++i) free(argv[i]);
    free(argv); return code;
}
#else
int main(int argc, char **argv) { return run(argc, argv); }
#endif
