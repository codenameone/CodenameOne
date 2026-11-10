# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.

"""cn1console: styled output, tables, trees, progress bars, spinners and
interactive prompts for command-line tools, over Codename One's console
(com.codename1.cli).

Everything degrades by itself: off a terminal styles are dropped, a progress
bar prints its final line instead of redrawing, and a prompt reads plain
lines from standard input, so the same program works interactively, piped
and under CI. A prompt cancelled with Ctrl-C raises KeyboardInterrupt, which
ends the program with status 130 unless it is caught.
"""
import _cn1term

__all__ = ["style", "echo", "width", "size", "is_terminal", "color_enabled",
           "table", "print_table", "tree", "progress", "Progress", "spinner",
           "Spinner", "confirm", "text", "password", "select", "checkbox"]


def is_terminal(fd=1):
    """Whether standard input (0), output (1) or error (2) is a terminal."""
    return _cn1term.isatty(fd)


def color_enabled():
    """Whether escape codes are wanted: a terminal, NO_COLOR unset."""
    return _cn1term.color()


def size():
    """(columns, rows) of the terminal, 80 by 24 when there is none."""
    return _cn1term.size()


def style(text, *styles):
    """`text` with styles applied when color is enabled, else unchanged.
    A style is an attribute (bold, dim, italic, underline, reverse, strike),
    a color (red, bright_blue, gray, "#ff8800") or "on <color>" for the
    background."""
    if not styles:
        return str(text)
    return _cn1term.style(str(text), list(styles))


def echo(*values, style=None, sep=" ", end="\n"):
    """print(), with every value in `style` (a string or a sequence of
    them)."""
    line = sep.join(str(v) for v in values)
    if style:
        styles = [style] if isinstance(style, str) else list(style)
        line = _cn1term.style(line, styles)
    print(line, end=end)


def width(text):
    """Columns `text` takes: escape codes left out, wide characters two."""
    return _cn1term.width(str(text))


def table(rows, headers=(), align=None, border=None):
    """A table as a string. `align` maps a column index to "left", "center"
    or "right"; `border` is rounded (the default where the terminal can
    draw boxes, ascii elsewhere), single, double, ascii, minimal or none.
    Columns shrink and cells wrap to fit the terminal."""
    rows = [[("" if c is None else str(c)) for c in row] for row in rows]
    heads = [str(h) for h in headers]
    count = max([len(heads)] + [len(r) for r in rows])
    aligns = ["left"] * count
    if align:
        for i, a in (align.items() if isinstance(align, dict) else enumerate(align)):
            if 0 <= i < count:
                aligns[i] = a
    return _cn1term.table(heads, rows, aligns, border or "")


def print_table(rows, headers=(), align=None, border=None):
    print(table(rows, headers, align, border), end="")


def tree(label, children=()):
    """A tree as a string. Each child is a label or a (label, children)
    pair."""
    labels = [str(label)]
    depths = [0]

    def walk(items, depth):
        for item in items:
            if isinstance(item, tuple):
                labels.append(str(item[0]))
                depths.append(depth)
                walk(item[1], depth + 1)
            else:
                labels.append(str(item))
                depths.append(depth)

    walk(children, 1)
    return _cn1term.tree(labels, depths)


class _NoBar:
    """What a bar or a spinner is where there is no console at all (a client
    application): every call does nothing, so library code that reports
    progress runs anywhere."""

    def step(self, n):
        pass

    def total(self, n):
        pass

    def message(self, m):
        pass

    def close(self, ok):
        pass

    def finish(self, kind, message):
        pass


class Progress:
    """A progress bar. Iterate it to track an iterable, or use it as a
    context manager and call `advance()`; leaving the block with an
    exception marks it failed. The bar appears on first use, so one made and
    never used draws nothing.

    A loop that can `break` belongs in a `with` block --
    `with progress(items) as bar: for x in bar: ...` -- because the
    generator a bare loop leaves behind is not closed until it is collected,
    and the bar stays on screen until then. Leaving the block early counts
    as finished."""

    def __init__(self, iterable=None, total=None, label="", unit=None):
        if total is None and iterable is not None:
            try:
                total = len(iterable)
            except TypeError:
                total = 0
        self._iterable = iterable
        self._total = total or 0
        self._label = label
        self._unit = unit
        self._bar = None
        self._closed = False

    def _get(self):
        if self._bar is None:
            try:
                self._bar = _cn1term.progress(self._label, self._total, self._unit)
            except OSError:
                self._bar = _NoBar()
        return self._bar

    def advance(self, n=1):
        self._get().step(n)

    def update(self, n=1):
        """tqdm's name for advance()."""
        self._get().step(n)

    def set_total(self, total):
        self._total = total
        if self._bar is not None:
            self._bar.total(total)

    def set_description(self, message):
        self._get().message(str(message))

    def close(self, ok=True):
        if not self._closed:
            self._closed = True
            if self._bar is not None:
                self._bar.close(ok)

    def __enter__(self):
        self._get()
        return self

    def __exit__(self, exc_type, exc, tb):
        self.close(exc_type is None)
        return False

    def __iter__(self):
        if self._iterable is None:
            raise TypeError("Progress made without an iterable is not iterable")
        bar = self._get()
        ok = False
        try:
            for item in self._iterable:
                yield item
                bar.step(1)
            ok = True
        except GeneratorExit:
            # A `break` out of the loop: the work stopped, it did not fail.
            ok = True
            raise
        finally:
            self.close(ok)


def progress(iterable=None, total=None, label="", unit=None):
    """`for x in progress(items, label="copy"):` shows a bar while the loop
    runs. unit="bytes" shows sizes and rates in bytes."""
    return Progress(iterable, total, label, unit)


class Spinner:
    """An animated spinner, as a context manager: it stops when the block
    ends, with a check mark, or a cross if the block raised."""

    def __init__(self, message=""):
        self._message = message
        self._spinner = None
        self._done = False

    def start(self):
        if self._spinner is None:
            try:
                self._spinner = _cn1term.spinner(self._message)
            except OSError:
                self._spinner = _NoBar()
        return self

    @property
    def text(self):
        return self._message

    @text.setter
    def text(self, message):
        self._message = message
        if self._spinner is not None:
            self._spinner.message(message)

    def _finish(self, kind, message):
        if self._spinner is not None and not self._done:
            self._done = True
            self._spinner.finish(kind, message)

    def succeed(self, message=None):
        self._finish("succeed", self._message if message is None else message)

    def fail(self, message=None):
        self._finish("fail", self._message if message is None else message)

    def warn(self, message=None):
        self._finish("warn", self._message if message is None else message)

    def info(self, message=None):
        self._finish("info", self._message if message is None else message)

    def stop(self):
        self._finish("stop", None)

    def __enter__(self):
        return self.start()

    def __exit__(self, exc_type, exc, tb):
        if exc_type is None:
            self.succeed()
        else:
            self.fail()
        return False


def spinner(message=""):
    return Spinner(message)


def confirm(question, default=None):
    """A yes or no question; Enter takes `default`. With None, Enter alone
    is not an answer and the question waits for y or n."""
    return _cn1term.confirm(question, default)


def text(question, default=None):
    """A line of text; Enter takes `default`."""
    return _cn1term.text(question, default)


def password(question):
    """A line typed without being shown."""
    return _cn1term.password(question)


def select(question, choices, default=None):
    """One of `choices`, picked with the arrow keys (or by number when
    standard input is not a terminal). `default` is a choice, or an int
    index (negative counts from the end) when no choice equals it."""
    choices = list(choices)
    labels = [str(c) for c in choices]
    index = _default_index(choices, default)
    return choices[_cn1term.select(question, labels, index)]


def checkbox(question, choices, default=()):
    """Any of `choices`, toggled with space; returns them in order.
    `default` lists the ones checked at the start (a single choice is
    taken as a list of one)."""
    choices = list(choices)
    labels = [str(c) for c in choices]
    if isinstance(default, (str, int)):
        default = [default]
    pre = []
    for d in default:
        i = _default_index(choices, d)
        if i >= 0 and i not in pre:
            pre.append(i)
    return [choices[i] for i in _cn1term.multiselect(question, labels, pre)]


def _default_index(choices, default):
    """The position of `default`: an element equal to it wins, so a list of
    numbers is matched by value; otherwise an int is an index, negative
    from the end. -1 when it matches nothing."""
    if default is None:
        return -1
    for i, c in enumerate(choices):
        if c == default:
            return i
    if isinstance(default, int) and not isinstance(default, bool):
        if -len(choices) <= default < 0:
            return default + len(choices)
        if 0 <= default < len(choices):
            return default
    return -1
