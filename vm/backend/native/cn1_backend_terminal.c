/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */

#include "cn1_globals.h"
#include <string.h>
#ifdef _WIN32
#include <windows.h>
#ifndef ENABLE_VIRTUAL_TERMINAL_PROCESSING
#define ENABLE_VIRTUAL_TERMINAL_PROCESSING 0x0004
#endif
#ifndef ENABLE_VIRTUAL_TERMINAL_INPUT
#define ENABLE_VIRTUAL_TERMINAL_INPUT 0x0200
#endif
#else
#include <errno.h>
#include <signal.h>
#include <stdlib.h>
#include <poll.h>
#include <termios.h>
#include <unistd.h>
#include <sys/ioctl.h>
#endif

/* Whether a standard stream is a terminal; fd is 0, 1 or 2. On Windows a console
   handle is one GetConsoleMode accepts -- a pipe or a file fails it. */
JAVA_INT com_codename1_cli_TerminalImpl_isTerminalImpl___int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT fd) {
#ifdef _WIN32
    DWORD mode;
    HANDLE h = GetStdHandle(fd == 0 ? STD_INPUT_HANDLE : fd == 2 ? STD_ERROR_HANDLE : STD_OUTPUT_HANDLE);
    return h != INVALID_HANDLE_VALUE && h != NULL && GetConsoleMode(h, &mode) ? 1 : 0;
#else
    return isatty(fd) ? 1 : 0;
#endif
}

/* 1 on Windows, where the console API differs: TERM is usually unset and
   raw input comes through SetConsoleMode rather than termios. */
JAVA_INT com_codename1_cli_TerminalImpl_isWindowsImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    return 1;
#else
    return 0;
#endif
}

/* The terminal size packed as columns << 16 | rows, or 0 when standard output
   (then standard error, then standard input) is not a terminal. */
JAVA_INT com_codename1_cli_TerminalImpl_sizeImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    CONSOLE_SCREEN_BUFFER_INFO info;
    HANDLE h = GetStdHandle(STD_OUTPUT_HANDLE);
    if (!GetConsoleScreenBufferInfo(h, &info)) {
        h = GetStdHandle(STD_ERROR_HANDLE);
        if (!GetConsoleScreenBufferInfo(h, &info)) {
            return 0;
        }
    }
    {
        int cols = info.srWindow.Right - info.srWindow.Left + 1;
        int rows = info.srWindow.Bottom - info.srWindow.Top + 1;
        if (cols <= 0 || rows <= 0) {
            return 0;
        }
        return (JAVA_INT)(((cols & 0xffff) << 16) | (rows & 0xffff));
    }
#else
    struct winsize ws;
    int fds[3] = {1, 2, 0};
    int i;
    for (i = 0; i < 3; i++) {
        memset(&ws, 0, sizeof(ws));
        if (ioctl(fds[i], TIOCGWINSZ, &ws) == 0 && ws.ws_col > 0 && ws.ws_row > 0) {
            return (JAVA_INT)(((ws.ws_col & 0xffff) << 16) | (ws.ws_row & 0xffff));
        }
    }
    return 0;
#endif
}

/*
 * What has to be put back when the process ends, however it ends: the mode
 * enterRawImpl changed, and a cursor a live region hid (a spinner killed by
 * Ctrl-C would otherwise leave the shell with no cursor). Raw mode is restored
 * by restoreImpl; both are restored at exit and by a handler on the signals
 * that end a process -- a terminal left raw by a killed tool is unusable until
 * the user types `reset`.
 */
#ifdef _WIN32
static DWORD cn1TermSavedMode;
static UINT cn1TermSavedInputCP = 0;
static UINT cn1TermSavedOutputCP = 0;
static volatile LONG cn1TermRaw = 0;
static volatile LONG cn1TermCursorHidden = 0;
static volatile LONG cn1TermExitInstalled = 0;

static void cn1TermRestoreNow(void) {
    if (InterlockedExchange(&cn1TermRaw, 0)) {
        SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), cn1TermSavedMode);
        if (cn1TermSavedInputCP != 0) {
            SetConsoleCP(cn1TermSavedInputCP);
        }
    }
    if (InterlockedExchange(&cn1TermCursorHidden, 0)) {
        static const char showCursor[] = "\033[?25h";
        DWORD written;
        WriteFile(GetStdHandle(STD_OUTPUT_HANDLE), showCursor, sizeof(showCursor) - 1, &written, NULL);
    }
}

static void cn1TermAtExit(void) {
    cn1TermRestoreNow();
    if (cn1TermSavedOutputCP != 0) {
        SetConsoleOutputCP(cn1TermSavedOutputCP);
    }
}

/* Ctrl-C outside raw mode, Ctrl-Break, closing the window, logoff and
   shutdown: restore, then let the default handling end the process. */
static BOOL WINAPI cn1TermCtrlHandler(DWORD type) {
    (void)type;
    cn1TermAtExit();
    return FALSE;
}

static void cn1TermInstallHandlers(void) {
    if (InterlockedExchange(&cn1TermExitInstalled, 1)) {
        return;
    }
    atexit(cn1TermAtExit);
    SetConsoleCtrlHandler(cn1TermCtrlHandler, TRUE);
}
#else
static struct termios cn1TermSaved;
static struct termios cn1TermRawMode;
static volatile sig_atomic_t cn1TermRaw = 0;
static volatile sig_atomic_t cn1TermCursorHidden = 0;
static int cn1TermHandlersInstalled = 0;
static struct sigaction cn1TermOldInt, cn1TermOldTerm, cn1TermOldHup, cn1TermOldQuit;

static void cn1TermRestoreNow(void) {
    /* tcsetattr and write are async-signal-safe. */
    if (cn1TermRaw) {
        tcsetattr(0, TCSAFLUSH, &cn1TermSaved);
        cn1TermRaw = 0;
    }
    if (cn1TermCursorHidden) {
        /* Only ever set by a live region, which draws only when standard
           output is a terminal, so this never reaches a pipe. */
        static const char showCursor[] = "\033[?25h";
        ssize_t ignored;
        cn1TermCursorHidden = 0;
        ignored = write(1, showCursor, sizeof(showCursor) - 1);
        (void)ignored;
    }
}

static void cn1TermSignal(int signo, siginfo_t *info, void *ctx) {
    struct sigaction *old = signo == SIGINT ? &cn1TermOldInt : signo == SIGTERM ? &cn1TermOldTerm
            : signo == SIGHUP ? &cn1TermOldHup : &cn1TermOldQuit;
    int wasRaw = cn1TermRaw;
    if (old->sa_handler == SIG_IGN && !(old->sa_flags & SA_SIGINFO)) {
        return;
    }
    cn1TermRestoreNow();
    if ((old->sa_flags & SA_SIGINFO) || (old->sa_handler != SIG_DFL && old->sa_handler != NULL)) {
        /* Chain to a handler installed before ours, the server's SIGTERM
           pipe. If it returns the process goes on, and a prompt that was
           open is still reading keys: put raw mode back. A cursor this
           restored stays shown; the next frame of a live region draws on. */
        if (old->sa_flags & SA_SIGINFO) {
            if (old->sa_sigaction != NULL) {
                old->sa_sigaction(signo, info, ctx);
            }
        } else {
            old->sa_handler(signo);
        }
        if (wasRaw && tcsetattr(0, TCSANOW, &cn1TermRawMode) == 0) {
            cn1TermRaw = 1;
        }
        return;
    }
    signal(signo, SIG_DFL);
    raise(signo);
}

static void cn1TermAtExit(void) {
    cn1TermRestoreNow();
}

static void cn1TermInstallHandlers(void) {
    struct sigaction sa;
    if (cn1TermHandlersInstalled) {
        return;
    }
    cn1TermHandlersInstalled = 1;
    memset(&sa, 0, sizeof(sa));
    sa.sa_sigaction = cn1TermSignal;
    sigemptyset(&sa.sa_mask);
    sa.sa_flags = SA_RESTART | SA_SIGINFO;
    sigaction(SIGINT, &sa, &cn1TermOldInt);
    sigaction(SIGTERM, &sa, &cn1TermOldTerm);
    sigaction(SIGHUP, &sa, &cn1TermOldHup);
    sigaction(SIGQUIT, &sa, &cn1TermOldQuit);
    atexit(cn1TermAtExit);
}
#endif

/* Lets the console interpret ANSI escapes on standard output and error. A no-op
   on POSIX, where every terminal does; Windows 10+ conhost needs asking. */
JAVA_VOID com_codename1_cli_TerminalImpl_prepareOutputImpl__(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    DWORD mode;
    HANDLE out = GetStdHandle(STD_OUTPUT_HANDLE);
    HANDLE err = GetStdHandle(STD_ERROR_HANDLE);
    if (GetConsoleMode(out, &mode)) {
        SetConsoleMode(out, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
    }
    if (GetConsoleMode(err, &mode)) {
        SetConsoleMode(err, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
    }
    if (cn1TermSavedOutputCP == 0) {
        cn1TermSavedOutputCP = GetConsoleOutputCP();
    }
    SetConsoleOutputCP(65001);
    cn1TermInstallHandlers();
#endif
}

/* Records that a live region hid (1) or showed (0) the cursor, so the exit
   paths show it again. */
JAVA_VOID com_codename1_cli_TerminalImpl_cursorHiddenImpl___int(CODENAME_ONE_THREAD_STATE, JAVA_INT hidden) {
    cn1TermInstallHandlers();
#ifdef _WIN32
    InterlockedExchange(&cn1TermCursorHidden, hidden ? 1 : 0);
#else
    cn1TermCursorHidden = hidden ? 1 : 0;
#endif
}

/* Puts standard input in raw mode -- keys arrive one byte at a time,
   unechoed, with Ctrl-C as byte 3 rather than a signal; 1 on success, 0 when
   it is not a terminal. */
JAVA_INT com_codename1_cli_TerminalImpl_enterRawImpl___R_int(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    HANDLE in = GetStdHandle(STD_INPUT_HANDLE);
    DWORD mode;
    if (cn1TermRaw) {
        return 1;
    }
    if (!GetConsoleMode(in, &mode)) {
        return 0;
    }
    cn1TermInstallHandlers();
    cn1TermSavedMode = mode;
    mode &= ~(ENABLE_ECHO_INPUT | ENABLE_LINE_INPUT | ENABLE_PROCESSED_INPUT);
    mode |= ENABLE_VIRTUAL_TERMINAL_INPUT;
    if (!SetConsoleMode(in, mode)) {
        return 0;
    }
    cn1TermSavedInputCP = GetConsoleCP();
    InterlockedExchange(&cn1TermRaw, 1);
    return 1;
#else
    struct termios raw;
    if (cn1TermRaw) {
        return 1;
    }
    if (!isatty(0) || tcgetattr(0, &cn1TermSaved) != 0) {
        return 0;
    }
    cn1TermInstallHandlers();
    raw = cn1TermSaved;
    /* cfmakeraw, except output processing stays on so "\n" still returns the
       carriage: everything printed while a prompt is open keeps working. */
    raw.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL | IXON);
    raw.c_lflag &= ~(ECHO | ECHONL | ICANON | ISIG | IEXTEN);
    raw.c_cflag &= ~(CSIZE | PARENB);
    raw.c_cflag |= CS8;
    raw.c_cc[VMIN] = 1;
    raw.c_cc[VTIME] = 0;
    cn1TermRawMode = raw;
    if (tcsetattr(0, TCSAFLUSH, &raw) != 0) {
        /* tcsetattr succeeds if ANY change applied; put back what did. */
        tcsetattr(0, TCSAFLUSH, &cn1TermSaved);
        return 0;
    }
    cn1TermRaw = 1;
    return 1;
#endif
}

/* Restores the mode enterRawImpl saved; harmless when not raw. */
JAVA_VOID com_codename1_cli_TerminalImpl_restoreImpl__(CODENAME_ONE_THREAD_STATE) {
#ifdef _WIN32
    if (InterlockedExchange(&cn1TermRaw, 0)) {
        SetConsoleMode(GetStdHandle(STD_INPUT_HANDLE), cn1TermSavedMode);
        if (cn1TermSavedInputCP != 0) {
            SetConsoleCP(cn1TermSavedInputCP);
        }
    }
#else
    if (cn1TermRaw) {
        tcsetattr(0, TCSAFLUSH, &cn1TermSaved);
        cn1TermRaw = 0;
    }
#endif
}

#ifdef _WIN32
/* UTF-8 bytes of the last character read from the console, handed out one
   at a time. The console is read as UTF-16 key events rather than through
   ReadFile, which on Windows 10 before 1903 answers 0 bytes -- read as end of
   input -- for any non-ASCII key under code page 65001. With
   ENABLE_VIRTUAL_TERMINAL_INPUT an arrow arrives as the key events of its
   escape sequence, so the decoding above it is the same as on POSIX. */
static unsigned char cn1TermUtf8[64];
static int cn1TermUtf8Len = 0;
static int cn1TermUtf8Pos = 0;
static WCHAR cn1TermHighSurrogate = 0;

static void cn1TermEncode(unsigned int cp) {
    if (cp < 0x80) {
        cn1TermUtf8[0] = (unsigned char)cp;
        cn1TermUtf8Len = 1;
    } else if (cp < 0x800) {
        cn1TermUtf8[0] = (unsigned char)(0xc0 | (cp >> 6));
        cn1TermUtf8[1] = (unsigned char)(0x80 | (cp & 0x3f));
        cn1TermUtf8Len = 2;
    } else if (cp < 0x10000) {
        cn1TermUtf8[0] = (unsigned char)(0xe0 | (cp >> 12));
        cn1TermUtf8[1] = (unsigned char)(0x80 | ((cp >> 6) & 0x3f));
        cn1TermUtf8[2] = (unsigned char)(0x80 | (cp & 0x3f));
        cn1TermUtf8Len = 3;
    } else {
        cn1TermUtf8[0] = (unsigned char)(0xf0 | (cp >> 18));
        cn1TermUtf8[1] = (unsigned char)(0x80 | ((cp >> 12) & 0x3f));
        cn1TermUtf8[2] = (unsigned char)(0x80 | ((cp >> 6) & 0x3f));
        cn1TermUtf8[3] = (unsigned char)(0x80 | (cp & 0x3f));
        cn1TermUtf8Len = 4;
    }
    cn1TermUtf8Pos = 0;
}

/* The next byte from a console handle, -1 at end of input, -2 on timeout.
   Only a key press carrying a character counts: WaitForSingleObject wakes for
   key releases, focus, mouse and resize events too, and those are discarded
   without restarting the wait, so a timeout means what it says. */
static JAVA_INT cn1TermReadConsole(HANDLE in, JAVA_INT timeoutMs) {
    ULONGLONG deadline = timeoutMs < 0 ? 0 : GetTickCount64() + (ULONGLONG)timeoutMs;
    if (cn1TermUtf8Pos < cn1TermUtf8Len) {
        return cn1TermUtf8[cn1TermUtf8Pos++];
    }
    for (;;) {
        INPUT_RECORD rec;
        DWORD n = 0;
        DWORD wait = INFINITE;
        if (timeoutMs >= 0) {
            ULONGLONG now = GetTickCount64();
            wait = now >= deadline ? 0 : (DWORD)(deadline - now);
        }
        if (WaitForSingleObject(in, wait) == WAIT_TIMEOUT) {
            return -2;
        }
        if (!ReadConsoleInputW(in, &rec, 1, &n) || n != 1) {
            return -1;
        }
        if (rec.EventType == KEY_EVENT && rec.Event.KeyEvent.bKeyDown
                && rec.Event.KeyEvent.uChar.UnicodeChar != 0) {
            WCHAR c = rec.Event.KeyEvent.uChar.UnicodeChar;
            if (c >= 0xd800 && c <= 0xdbff) {
                cn1TermHighSurrogate = c;
                continue;
            }
            if (c >= 0xdc00 && c <= 0xdfff) {
                if (cn1TermHighSurrogate == 0) {
                    continue;
                }
                cn1TermEncode(0x10000 + (((unsigned int)cn1TermHighSurrogate - 0xd800) << 10)
                        + ((unsigned int)c - 0xdc00));
                cn1TermHighSurrogate = 0;
            } else {
                cn1TermEncode(c);
            }
            {
                /* Auto-repeat can arrive as one record with a count. */
                int repeat = rec.Event.KeyEvent.wRepeatCount;
                int len = cn1TermUtf8Len;
                while (--repeat > 0 && cn1TermUtf8Len + len <= (int)sizeof(cn1TermUtf8)) {
                    memcpy(cn1TermUtf8 + cn1TermUtf8Len, cn1TermUtf8, (size_t)len);
                    cn1TermUtf8Len += len;
                }
            }
            return cn1TermUtf8[cn1TermUtf8Pos++];
        }
    }
}
#endif

/* One byte from standard input: 0-255, -1 at end of input, -2 when timeoutMs
   passed first (a negative timeout waits for ever). */
JAVA_INT com_codename1_cli_TerminalImpl_readByteImpl___int_R_int(CODENAME_ONE_THREAD_STATE,
        JAVA_INT timeoutMs) {
#ifdef _WIN32
    HANDLE in = GetStdHandle(STD_INPUT_HANDLE);
    unsigned char b;
    DWORD n = 0;
    DWORD mode;
    JAVA_INT result;
    CN1_YIELD_THREAD;
    /* Key events only while raw: in cooked mode the console's own line
       editing and echo must stay in charge, and ReadFile hands back the
       line with its "\r\n" ending. */
    if (cn1TermRaw && GetConsoleMode(in, &mode)) {
        result = cn1TermReadConsole(in, timeoutMs);
    } else {
        result = ReadFile(in, &b, 1, &n, NULL) && n == 1 ? (JAVA_INT)b : -1;
    }
    CN1_RESUME_THREAD;
    return result;
#else
    unsigned char b;
    ssize_t n;
    struct pollfd p;
    int r;
    p.fd = 0;
    p.events = POLLIN;
    p.revents = 0;
    CN1_YIELD_THREAD;
    do {
        r = poll(&p, 1, timeoutMs < 0 ? -1 : (int)timeoutMs);
    } while (r < 0 && errno == EINTR);
    if (r == 0) {
        CN1_RESUME_THREAD;
        return -2;
    }
    do {
        n = read(0, &b, 1);
    } while (n < 0 && errno == EINTR);
    CN1_RESUME_THREAD;
    return n == 1 ? (JAVA_INT)b : -1;
#endif
}
