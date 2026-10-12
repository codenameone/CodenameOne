/* Subprocess OS layer shared by ParparVM desktop and backend targets.
 * No VM pointers cross an OS call. All pipe transfers and exit checks are polls.
 * SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0 */
#ifndef CN1_PROCESS_H
#define CN1_PROCESS_H
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>
#include <limits.h>
#ifdef _WIN32
#include <windows.h>
#include <wchar.h>

typedef struct {
    HANDLE handle;
    OVERLAPPED op;
    unsigned char *buffer;
    DWORD size;
    int pending;
} cn1p_pipe;
typedef struct {
    HANDLE process;
    cn1p_pipe pipes[3];
    int done;
    int code;
} cn1p_child;

static wchar_t *cn1p_wide(const char *s) {
    int n = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, s, -1, NULL, 0);
    wchar_t *out = n ? (wchar_t *)malloc((size_t)n * sizeof(wchar_t)) : NULL;
    if (out) MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, s, -1, out, n);
    return out;
}
/* Quote every argument using the Windows CRT parsing rules, including empty
 * arguments, embedded quotes and trailing backslashes. No cmd.exe is involved. */
static wchar_t *cn1p_command(char **args) {
    size_t size = 1;
    int i;
    for (i = 0; args[i]; ++i) size += 2 * strlen(args[i]) + 3;
    wchar_t *result = (wchar_t *)calloc(size, sizeof(wchar_t)), *out = result;
    if (!out) return NULL;
    for (i = 0; args[i]; ++i) {
        wchar_t *arg = cn1p_wide(args[i]), *p;
        if (!arg) { free(result); return NULL; }
        if (i) *out++ = L' ';
        *out++ = L'"';
        p = arg;
        while (*p) {
            size_t slashes = 0;
            while (*p == L'\\') { ++slashes; ++p; }
            if (*p == L'"' || !*p) slashes *= 2;
            while (slashes--) *out++ = L'\\';
            if (*p == L'"') *out++ = L'\\';
            if (*p) *out++ = *p++;
        }
        *out++ = L'"';
        free(arg);
    }
    *out = 0;
    return result;
}
static int cn1p_envcmp(const void *a, const void *b) {
    return _wcsicmp(*(wchar_t *const *)a, *(wchar_t *const *)b);
}
static wchar_t *cn1p_environment(char **env) {
    int count = 0, i;
    size_t size = 2;
    while (env[count]) ++count;
    wchar_t **entries = (wchar_t **)calloc((size_t)count + 1, sizeof(wchar_t *));
    if (!entries) return NULL;
    for (i = 0; i < count; ++i) {
        entries[i] = cn1p_wide(env[i]);
        if (!entries[i]) { while (i--) free(entries[i]); free(entries); return NULL; }
        size += wcslen(entries[i]) + 1;
    }
    qsort(entries, (size_t)count, sizeof(wchar_t *), cn1p_envcmp);
    wchar_t *block = (wchar_t *)calloc(size, sizeof(wchar_t)), *p = block;
    for (i = 0; i < count; ++i) {
        if (p) { size_t n = wcslen(entries[i]) + 1; memcpy(p, entries[i], n * sizeof(wchar_t)); p += n; }
        free(entries[i]);
    }
    free(entries);
    return block;
}
static void cn1p_close_pipe(cn1p_child *child, int index) {
    cn1p_pipe *p = &child->pipes[index];
    if (p->handle && p->handle != INVALID_HANDLE_VALUE) {
        if (p->pending) {
            DWORD ignored;
            CancelIoEx(p->handle, &p->op);
            GetOverlappedResult(p->handle, &p->op, &ignored, TRUE);
        }
        CloseHandle(p->handle);
    }
    if (p->op.hEvent) CloseHandle(p->op.hEvent);
    free(p->buffer);
    memset(p, 0, sizeof(*p));
}
static void cn1p_release(cn1p_child *child) {
    int i;
    if (!child) return;
    for (i = 0; i < 3; ++i) cn1p_close_pipe(child, i);
    if (child->process) CloseHandle(child->process);
    free(child);
}
static int cn1p_make_pipe(cn1p_pipe *parent, HANDLE *child, int input) {
    static volatile LONG serial;
    wchar_t name[100];
    SECURITY_ATTRIBUTES security = {sizeof(security), NULL, TRUE};
    swprintf(name, 100, L"\\\\.\\pipe\\cn1-process-%lu-%lu", (unsigned long)GetCurrentProcessId(),
             (unsigned long)InterlockedIncrement(&serial));
    parent->handle = CreateNamedPipeW(name, (input ? PIPE_ACCESS_OUTBOUND : PIPE_ACCESS_INBOUND)
            | FILE_FLAG_OVERLAPPED | FILE_FLAG_FIRST_PIPE_INSTANCE,
            PIPE_TYPE_BYTE | PIPE_READMODE_BYTE | PIPE_WAIT | PIPE_REJECT_REMOTE_CLIENTS,
            1, 65536, 65536, 0, NULL);
    if (parent->handle == INVALID_HANDLE_VALUE) return 0;
    *child = CreateFileW(name, input ? GENERIC_READ : GENERIC_WRITE, 0, &security,
                         OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (*child == INVALID_HANDLE_VALUE) return 0;
    parent->op.hEvent = CreateEventW(NULL, TRUE, FALSE, NULL);
    return parent->op.hEvent != NULL;
}
static cn1p_child *cn1p_launch(char **args, const char *directory, char **env, int merge, int *error) {
    cn1p_child *child = (cn1p_child *)calloc(1, sizeof(*child));
    HANDLE inherited[3] = {NULL, NULL, NULL};
    STARTUPINFOEXW startup;
    PROCESS_INFORMATION info;
    SIZE_T attrSize = 0;
    wchar_t *command = cn1p_command(args), *cwd = directory ? cn1p_wide(directory) : NULL;
    wchar_t *environment = env ? cn1p_environment(env) : NULL;
    int ok = 0, i, attrReady = 0;
    memset(&startup, 0, sizeof(startup)); memset(&info, 0, sizeof(info));
    startup.StartupInfo.cb = sizeof(startup);
    *error = ERROR_NOT_ENOUGH_MEMORY;
    if (!child || !command || (directory && !cwd) || (env && !environment)) goto cleanup;
    for (i = 0; i < (merge ? 2 : 3); ++i) {
        if (!cn1p_make_pipe(&child->pipes[i], &inherited[i], i == 0)) { *error = (int)GetLastError(); goto cleanup; }
    }
    startup.StartupInfo.dwFlags = STARTF_USESTDHANDLES;
    startup.StartupInfo.hStdInput = inherited[0];
    startup.StartupInfo.hStdOutput = inherited[1];
    startup.StartupInfo.hStdError = merge ? inherited[1] : inherited[2];
    InitializeProcThreadAttributeList(NULL, 1, 0, &attrSize);
    startup.lpAttributeList = (LPPROC_THREAD_ATTRIBUTE_LIST)malloc(attrSize);
    if (!startup.lpAttributeList) goto cleanup;
    if (!InitializeProcThreadAttributeList(startup.lpAttributeList, 1, 0, &attrSize)) { *error = (int)GetLastError(); goto cleanup; }
    attrReady = 1;
    if (!UpdateProcThreadAttribute(startup.lpAttributeList, 0, PROC_THREAD_ATTRIBUTE_HANDLE_LIST,
            inherited, (merge ? 2 : 3) * sizeof(HANDLE), NULL, NULL)) { *error = (int)GetLastError(); goto cleanup; }
    ok = CreateProcessW(NULL, command, NULL, NULL, TRUE,
            EXTENDED_STARTUPINFO_PRESENT | CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW,
            environment, cwd, &startup.StartupInfo, &info);
    if (!ok) { *error = (int)GetLastError(); goto cleanup; }
    child->process = info.hProcess;
    CloseHandle(info.hThread);
cleanup:
    if (attrReady) DeleteProcThreadAttributeList(startup.lpAttributeList);
    free(startup.lpAttributeList);
    for (i = 0; i < 3; ++i) if (inherited[i] && inherited[i] != INVALID_HANDLE_VALUE) CloseHandle(inherited[i]);
    free(command); free(cwd); free(environment);
    if (!ok) { cn1p_release(child); return NULL; }
    return child;
}
static int cn1p_poll(cn1p_child *child) {
    DWORD code;
    if (!child->done && WaitForSingleObject(child->process, 0) == WAIT_OBJECT_0) {
        if (GetExitCodeProcess(child->process, &code)) { child->code = (int)code; child->done = 1; }
    }
    return child->done;
}
static void cn1p_terminate(cn1p_child *child, int force) {
    (void)force;
    if (!cn1p_poll(child)) TerminateProcess(child->process, 1);
}
static int cn1p_transfer(cn1p_child *child, int index, unsigned char *bytes, int length) {
    cn1p_pipe *p = &child->pipes[index];
    DWORD done = 0, error;
    BOOL ok;
    if (!p->handle) return index ? -1 : -2;
    if (p->pending) {
        ok = GetOverlappedResult(p->handle, &p->op, &done, FALSE);
        error = ok ? ERROR_SUCCESS : GetLastError();
        if (error == ERROR_IO_INCOMPLETE) return -3;
        p->pending = 0;
    } else {
        DWORD size = (DWORD)(length > 65536 ? 65536 : length);
        if (size > p->size) {
            unsigned char *buffer = (unsigned char *)realloc(p->buffer, size);
            if (!buffer) return -2;
            p->buffer = buffer; p->size = size;
        }
        if (!index) memcpy(p->buffer, bytes, size);
        ResetEvent(p->op.hEvent);
        ok = index ? ReadFile(p->handle, p->buffer, size, &done, &p->op)
                   : WriteFile(p->handle, p->buffer, size, &done, &p->op);
        error = ok ? ERROR_SUCCESS : GetLastError();
        if (error == ERROR_IO_PENDING) { p->pending = 1; return -3; }
    }
    if (error != ERROR_SUCCESS) return index && error == ERROR_BROKEN_PIPE ? -1 : -2;
    if (index && done) memcpy(bytes, p->buffer, done);
    return done ? (int)done : (index ? -1 : -2);
}
#else
#include <unistd.h>
#include <fcntl.h>
#include <signal.h>
#include <pthread.h>
#include <sys/types.h>
#include <sys/wait.h>
#if defined(__APPLE__)
#include <crt_externs.h>
#define CN1P_ENVIRON (*_NSGetEnviron())
#else
extern char **environ;
#define CN1P_ENVIRON environ
#endif

typedef struct { pid_t pid; int pipes[3]; int done; int code; } cn1p_child;
static void cn1p_close_pipe(cn1p_child *child, int index) {
    if (child->pipes[index] >= 0) { close(child->pipes[index]); child->pipes[index] = -1; }
}
static void cn1p_release(cn1p_child *child) {
    int i;
    if (!child) return;
    for (i = 0; i < 3; ++i) cn1p_close_pipe(child, i);
    free(child);
}
static int cn1p_pipe(int *fds) {
    int i;
    if (pipe(fds)) return -1;
    for (i = 0; i < 2; ++i) {
        if (fds[i] < 3) {
            int fd = fcntl(fds[i], F_DUPFD, 3);
            if (fd < 0) goto fail;
            close(fds[i]); fds[i] = fd;
        }
        if (fcntl(fds[i], F_SETFD, FD_CLOEXEC) < 0) goto fail;
    }
    return 0;
fail:
    { int saved = errno; close(fds[0]); close(fds[1]); fds[0] = fds[1] = -1; errno = saved; }
    return -1;
}
/* Prepare PATH candidates before fork. The child does only async-signal-safe
 * syscalls, never allocation, Java, stdio, or setenv in a multithreaded VM. */
static char **cn1p_paths(const char *command, char **env) {
    const char *path = "/usr/bin:/bin", *start, *end;
    size_t count = 1, i = 0;
    char **paths;
    if (strchr(command, '/')) {
        paths = (char **)calloc(2, sizeof(char *));
        if (paths) { paths[0] = strdup(command); if (!paths[0]) { free(paths); return NULL; } }
        return paths;
    }
    for (i = 0; env[i]; ++i) if (!strncmp(env[i], "PATH=", 5)) { path = env[i] + 5; break; }
    for (start = path; *start; ++start) if (*start == ':') ++count;
    paths = (char **)calloc(count + 1, sizeof(char *));
    if (!paths) return NULL;
    i = 0; start = path;
    do {
        end = strchr(start, ':');
        size_t len = end ? (size_t)(end - start) : strlen(start);
        paths[i] = (char *)malloc(len + strlen(command) + 2);
        if (!paths[i]) { while (i) free(paths[--i]); free(paths); return NULL; }
        if (len) { memcpy(paths[i], start, len); paths[i][len] = '/'; strcpy(paths[i] + len + 1, command); }
        else strcpy(paths[i], command);
        ++i; start = end ? end + 1 : NULL;
    } while (start);
    return paths;
}
static cn1p_child *cn1p_launch(char **args, const char *directory, char **environment, int merge, int *error) {
    int pipes[4][2] = {{-1,-1},{-1,-1},{-1,-1},{-1,-1}};
    int i, j, err = 0, status;
    long maxfd = sysconf(_SC_OPEN_MAX);
    char **env = environment ? environment : CN1P_ENVIRON;
    char **paths = cn1p_paths(args[0], env);
    cn1p_child *child = (cn1p_child *)calloc(1, sizeof(*child));
    pid_t pid = -1;
    *error = ENOMEM;
    if (maxfd < 0 || maxfd > INT_MAX) maxfd = INT_MAX;
    if (child) for (i = 0; i < 3; ++i) child->pipes[i] = -1;
    if (!paths || !child) goto cleanup;
    for (i = 0; i < 4; ++i) {
        if (i == 2 && merge) continue;
        if (cn1p_pipe(pipes[i])) { *error = errno; goto cleanup; }
    }
    pid = fork();
    if (pid < 0) { *error = errno; goto cleanup; }
    if (pid == 0) {
        int failure = 0;
        if (directory && chdir(directory)) failure = errno;
        if (!failure && (dup2(pipes[0][0], 0) < 0 || dup2(pipes[1][1], 1) < 0
                || dup2(pipes[merge ? 1 : 2][1], 2) < 0)) failure = errno;
        for (i = 3; i < maxfd; ++i) if (i != pipes[3][1]) close(i);
        // The VM may ignore SIGPIPE. Give the child normal shell-independent
        // process signal semantics and an unblocked signal mask.
        struct sigaction action;
        sigset_t mask;
        memset(&action, 0, sizeof(action)); action.sa_handler = SIG_DFL;
        sigemptyset(&action.sa_mask); sigaction(SIGPIPE, &action, NULL);
        sigemptyset(&mask); sigprocmask(SIG_SETMASK, &mask, NULL);
        if (!failure) {
            int denied = 0;
            for (i = 0; paths[i]; ++i) {
                execve(paths[i], args, env);
                failure = errno;
                if (failure == EACCES) denied = 1;
                else if (failure != ENOENT && failure != ENOTDIR) break;
            }
            if (!paths[i] && denied) failure = EACCES;
        }
        while (write(pipes[3][1], &failure, sizeof(failure)) < 0 && errno == EINTR) { }
        _exit(127);
    }
    close(pipes[3][1]); pipes[3][1] = -1;
    { ssize_t n;
      do { n = read(pipes[3][0], &err, sizeof(err)); } while (n < 0 && errno == EINTR);
      if (n != 0) {
          *error = n < 0 ? errno : err;
          kill(pid, SIGKILL);
          while (waitpid(pid, &status, 0) < 0 && errno == EINTR) { }
          pid = -1; goto cleanup;
      }
    }
    child->pid = pid;
    for (i = 0; i < 3; ++i) {
        if (i == 2 && merge) continue;
        j = i == 0 ? 1 : 0;
        child->pipes[i] = pipes[i][j]; pipes[i][j] = -1;
        if (fcntl(child->pipes[i], F_SETFL, O_NONBLOCK) < 0) {
            *error = errno; kill(pid, SIGKILL);
            while (waitpid(pid, &status, 0) < 0 && errno == EINTR) { }
            pid = -1; goto cleanup;
        }
    }
cleanup:
    for (i = 0; i < 4; ++i) for (j = 0; j < 2; ++j) if (pipes[i][j] >= 0) close(pipes[i][j]);
    if (paths) { for (i = 0; paths[i]; ++i) free(paths[i]); free(paths); }
    if (pid < 0) { cn1p_release(child); return NULL; }
    return child;
}
/* Called under the Java child's lock, as is terminate: a reaped PID can never
 * be reused between the running check and kill. */
static int cn1p_poll(cn1p_child *child) {
    int status;
    pid_t result;
    if (child->done) return 1;
    do { result = waitpid(child->pid, &status, WNOHANG); } while (result < 0 && errno == EINTR);
    if (result == child->pid) {
        child->code = WIFEXITED(status) ? WEXITSTATUS(status) : 128 + WTERMSIG(status);
        child->done = 1;
    } else if (result < 0 && errno == ECHILD) { child->done = 1; child->code = -1; }
    return child->done;
}
static void cn1p_terminate(cn1p_child *child, int force) {
    if (!cn1p_poll(child)) kill(child->pid, force ? SIGKILL : SIGTERM);
}
static int cn1p_transfer(cn1p_child *child, int index, unsigned char *bytes, int length) {
    ssize_t n;
    if (child->pipes[index] < 0) return index ? -1 : -2;
    if (index) n = read(child->pipes[index], bytes, (size_t)length);
    else {
        // Suppress only this write's SIGPIPE, without changing the application's
        // process-wide signal disposition or consuming a previously pending one.
        sigset_t block, old, pending;
        int wasPending, saved;
        sigemptyset(&block); sigaddset(&block, SIGPIPE);
        if (pthread_sigmask(SIG_BLOCK, &block, &old)) return -2;
        sigpending(&pending); wasPending = sigismember(&pending, SIGPIPE);
        n = write(child->pipes[index], bytes, (size_t)length); saved = errno;
        if (n < 0 && saved == EPIPE && !wasPending) {
            sigpending(&pending);
            if (sigismember(&pending, SIGPIPE)) { int sig; sigwait(&block, &sig); }
        }
        pthread_sigmask(SIG_SETMASK, &old, NULL); errno = saved;
    }
    if (n < 0) return errno == EAGAIN || errno == EWOULDBLOCK || errno == EINTR ? -3 : -2;
    return n ? (int)n : (index ? -1 : -2);
}
#endif
#endif
