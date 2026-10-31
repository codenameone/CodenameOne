# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""YAML 1.1 loading and dumping with the interface of the PyYAML package.

This is an independent implementation written from the YAML 1.1 specification
and from the documented behaviour of PyYAML. It covers safe loading, the
document/anchor/merge machinery, the implicit type resolvers, the explicit
core tags, and a dumper whose quoting and layout decisions follow PyYAML.
"""

import re
import base64
import datetime

__version__ = "6.0.3"
__with_libyaml__ = False

_BLANKZ = "\0 \t\r\n\x85\u2028\u2029"
_HEXDIGITS = "0123456789abcdefABCDEF"


class _NS:
    pass


# ---------------------------------------------------------------- errors

class Mark:
    def __init__(self, name, index, line, column, buffer, pointer):
        self.name = name
        self.index = index
        self.line = line
        self.column = column
        self.buffer = buffer
        self.pointer = pointer

    def get_snippet(self, indent=4, max_length=75):
        if self.buffer is None:
            return None
        buf = self.buffer
        head = ""
        start = self.pointer
        while start > 0 and buf[start - 1] not in "\0\r\n\x85\u2028\u2029":
            start -= 1
            if self.pointer - start > max_length / 2 - 1:
                head = " ... "
                start += 5
                break
        tail = ""
        end = self.pointer
        while end < len(buf) and buf[end] not in "\0\r\n\x85\u2028\u2029":
            end += 1
            if end - self.pointer > max_length / 2 - 1:
                tail = " ... "
                end -= 5
                break
        snippet = buf[start:end]
        return (" " * indent + head + snippet + tail + "\n"
                + " " * (indent + self.pointer - start + len(head)) + "^")

    def __str__(self):
        snippet = self.get_snippet()
        where = '  in "%s", line %d, column %d' % (self.name, self.line + 1, self.column + 1)
        if snippet is not None:
            where += ":\n" + snippet
        return where


class YAMLError(Exception):
    pass


class MarkedYAMLError(YAMLError):
    def __init__(self, context=None, context_mark=None, problem=None, problem_mark=None, note=None):
        YAMLError.__init__(self)
        self.context = context
        self.context_mark = context_mark
        self.problem = problem
        self.problem_mark = problem_mark
        self.note = note

    def __str__(self):
        lines = []
        if self.context is not None:
            lines.append(self.context)
        if (self.context_mark is not None
                and (self.problem is None or self.problem_mark is None
                     or self.context_mark.name != self.problem_mark.name
                     or self.context_mark.line != self.problem_mark.line
                     or self.context_mark.column != self.problem_mark.column)):
            lines.append(str(self.context_mark))
        if self.problem is not None:
            lines.append(self.problem)
        if self.problem_mark is not None:
            lines.append(str(self.problem_mark))
        if self.note is not None:
            lines.append(self.note)
        return "\n".join(lines)


class ReaderError(YAMLError):
    def __init__(self, name, position, character, encoding, reason):
        YAMLError.__init__(self)
        self.name = name
        self.character = character
        self.position = position
        self.encoding = encoding
        self.reason = reason

    def __str__(self):
        if isinstance(self.character, bytes):
            return ("'%s' codec can't decode byte #x%02x: %s\n  in \"%s\", position %d"
                    % (self.encoding, ord(self.character), self.reason, self.name, self.position))
        return ('unacceptable character #x%04x: %s\n  in "%s", position %d'
                % (self.character, self.reason, self.name, self.position))


class ScannerError(MarkedYAMLError):
    pass


class ParserError(MarkedYAMLError):
    pass


class ComposerError(MarkedYAMLError):
    pass


class ConstructorError(MarkedYAMLError):
    pass


class RepresenterError(YAMLError):
    pass


class EmitterError(YAMLError):
    pass


class SerializerError(YAMLError):
    pass


# ----------------------------------------------------------------- nodes

class Node:
    def __init__(self, tag, value, start_mark, end_mark):
        self.tag = tag
        self.value = value
        self.start_mark = start_mark
        self.end_mark = end_mark

    def __repr__(self):
        return "%s(tag=%r, value=%r)" % (self.__class__.__name__, self.tag, self.value)


class ScalarNode(Node):
    id = "scalar"

    def __init__(self, tag, value, start_mark=None, end_mark=None, style=None):
        Node.__init__(self, tag, value, start_mark, end_mark)
        self.style = style


class CollectionNode(Node):
    def __init__(self, tag, value, start_mark=None, end_mark=None, flow_style=None):
        Node.__init__(self, tag, value, start_mark, end_mark)
        self.flow_style = flow_style


class SequenceNode(CollectionNode):
    id = "sequence"


class MappingNode(CollectionNode):
    id = "mapping"


# ------------------------------------------------------------- scanner

class Token:
    def __init__(self, kind, start_mark, end_mark, value=None, plain=False, style=None):
        self.id = kind
        self.start_mark = start_mark
        self.end_mark = end_mark
        self.value = value
        self.plain = plain
        self.style = style


class _SimpleKey:
    def __init__(self, token_number, required, index, line, column, mark):
        self.token_number = token_number
        self.required = required
        self.index = index
        self.line = line
        self.column = column
        self.mark = mark


_ESC_REPL = {
    "0": "\0", "a": "\x07", "b": "\x08", "t": "\x09", "\t": "\x09", "n": "\x0a",
    "v": "\x0b", "f": "\x0c", "r": "\x0d", "e": "\x1b", " ": " ", '"': '"',
    "\\": "\\", "/": "/", "N": "\x85", "_": "\xa0", "L": "\u2028", "P": "\u2029",
}
_ESC_CODES = {"x": 2, "u": 4, "U": 8}


class _Scanner:
    """Turns the character buffer into the YAML token stream."""

    def _init_scanner(self, text, name):
        self.name = name
        self.buffer = text + "\0"
        self.pointer = 0
        self.index = 0
        self.line = 0
        self.column = 0
        self.done = False
        self.flow_level = 0
        self.tokens = []
        self.tokens_taken = 0
        self.indent = -1
        self.indents = []
        self.allow_simple_key = True
        self.possible_simple_keys = {}
        mark = self._mark()
        self.tokens.append(Token("<stream start>", mark, mark))

    # -- character access
    def _peek(self, k=0):
        i = self.pointer + k
        if i < len(self.buffer):
            return self.buffer[i]
        return "\0"

    def _prefix(self, n=1):
        return self.buffer[self.pointer:self.pointer + n]

    def _forward(self, n=1):
        while n:
            ch = self.buffer[self.pointer]
            self.pointer += 1
            self.index += 1
            if ch in "\n\x85\u2028\u2029" or (ch == "\r" and self.buffer[self.pointer] != "\n"):
                self.line += 1
                self.column = 0
            elif ch != "\ufeff":
                self.column += 1
            n -= 1

    def _mark(self):
        return Mark(self.name, self.index, self.line, self.column, self.buffer, self.pointer)

    # -- token queue
    def _check_token(self, *kinds):
        while self._need_more_tokens():
            self._fetch_more_tokens()
        if self.tokens:
            if not kinds:
                return True
            for k in kinds:
                if self.tokens[0].id == k:
                    return True
        return False

    def _peek_token(self):
        while self._need_more_tokens():
            self._fetch_more_tokens()
        if self.tokens:
            return self.tokens[0]
        return None

    def _get_token(self):
        while self._need_more_tokens():
            self._fetch_more_tokens()
        if self.tokens:
            self.tokens_taken += 1
            return self.tokens.pop(0)
        return None

    def _need_more_tokens(self):
        if self.done:
            return False
        if not self.tokens:
            return True
        self._stale_keys()
        if self._next_key_number() == self.tokens_taken:
            return True
        return False

    def _next_key_number(self):
        best = None
        for level in self.possible_simple_keys:
            key = self.possible_simple_keys[level]
            if best is None or key.token_number < best:
                best = key.token_number
        return best

    def _stale_keys(self):
        for level in list(self.possible_simple_keys):
            key = self.possible_simple_keys[level]
            if key.line != self.line or self.index - key.index > 1024:
                if key.required:
                    raise ScannerError("while scanning a simple key", key.mark,
                                       "could not find expected ':'", self._mark())
                del self.possible_simple_keys[level]

    def _save_key(self):
        required = not self.flow_level and self.indent == self.column
        if self.allow_simple_key:
            self._remove_key()
            number = self.tokens_taken + len(self.tokens)
            self.possible_simple_keys[self.flow_level] = _SimpleKey(
                number, required, self.index, self.line, self.column, self._mark())

    def _remove_key(self):
        if self.flow_level in self.possible_simple_keys:
            key = self.possible_simple_keys[self.flow_level]
            if key.required:
                raise ScannerError("while scanning a simple key", key.mark,
                                   "could not find expected ':'", self._mark())
            del self.possible_simple_keys[self.flow_level]

    def _unwind(self, column):
        if self.flow_level:
            return
        while self.indent > column:
            mark = self._mark()
            self.indent = self.indents.pop()
            self.tokens.append(Token("<block end>", mark, mark))

    def _add_indent(self, column):
        if self.indent < column:
            self.indents.append(self.indent)
            self.indent = column
            return True
        return False

    # -- dispatcher
    def _fetch_more_tokens(self):
        self._scan_to_next_token()
        self._stale_keys()
        self._unwind(self.column)
        ch = self._peek()
        if ch == "\0":
            return self._fetch_stream_end()
        if ch == "%" and self.column == 0:
            return self._fetch_directive()
        if ch == "-" and self.column == 0 and self._prefix(3) == "---" and self._peek(3) in _BLANKZ:
            return self._fetch_document_indicator("<document start>")
        if ch == "." and self.column == 0 and self._prefix(3) == "..." and self._peek(3) in _BLANKZ:
            return self._fetch_document_indicator("<document end>")
        if ch == "[":
            return self._fetch_flow_start("[")
        if ch == "{":
            return self._fetch_flow_start("{")
        if ch == "]":
            return self._fetch_flow_end("]")
        if ch == "}":
            return self._fetch_flow_end("}")
        if ch == ",":
            return self._fetch_flow_entry()
        if ch == "-" and self._peek(1) in _BLANKZ:
            return self._fetch_block_entry()
        if ch == "?" and (self.flow_level or self._peek(1) in _BLANKZ):
            return self._fetch_key()
        if ch == ":" and (self.flow_level or self._peek(1) in _BLANKZ):
            return self._fetch_value()
        if ch == "*":
            return self._fetch_anchor("<alias>")
        if ch == "&":
            return self._fetch_anchor("<anchor>")
        if ch == "!":
            return self._fetch_tag()
        if ch == "|" and not self.flow_level:
            return self._fetch_block_scalar("|")
        if ch == ">" and not self.flow_level:
            return self._fetch_block_scalar(">")
        if ch == "'":
            return self._fetch_flow_scalar("'")
        if ch == '"':
            return self._fetch_flow_scalar('"')
        if self._check_plain():
            return self._fetch_plain()
        raise ScannerError("while scanning for the next token", None,
                           "found character %r that cannot start any token" % ch, self._mark())

    def _check_plain(self):
        ch = self._peek()
        return (ch not in "\0 \t\r\n\x85\u2028\u2029-?:,[]{}#&*!|>'\"%@`"
                or (self._peek(1) not in _BLANKZ
                    and (ch == "-" or (not self.flow_level and ch in "?:"))))

    def _scan_to_next_token(self):
        if self.index == 0 and self._peek() == "\ufeff":
            self._forward()
        found = False
        while not found:
            while self._peek() == " ":
                self._forward()
            if self._peek() == "#":
                while self._peek() not in "\0\r\n\x85\u2028\u2029":
                    self._forward()
            if self._scan_line_break():
                if not self.flow_level:
                    self.allow_simple_key = True
            else:
                found = True

    def _scan_line_break(self):
        ch = self._peek()
        if ch in "\r\n\x85":
            if self._prefix(2) == "\r\n":
                self._forward(2)
            else:
                self._forward()
            return "\n"
        if ch in "\u2028\u2029":
            self._forward()
            return ch
        return ""

    # -- fetchers
    def _fetch_stream_end(self):
        self._unwind(-1)
        self._remove_key()
        self.allow_simple_key = False
        self.possible_simple_keys = {}
        mark = self._mark()
        self.tokens.append(Token("<stream end>", mark, mark))
        self.done = True

    def _fetch_directive(self):
        self._unwind(-1)
        self._remove_key()
        self.allow_simple_key = False
        self.tokens.append(self._scan_directive())

    def _fetch_document_indicator(self, kind):
        self._unwind(-1)
        self._remove_key()
        self.allow_simple_key = False
        start = self._mark()
        self._forward(3)
        self.tokens.append(Token(kind, start, self._mark()))

    def _fetch_flow_start(self, kind):
        self._save_key()
        self.flow_level += 1
        self.allow_simple_key = True
        start = self._mark()
        self._forward()
        self.tokens.append(Token(kind, start, self._mark()))

    def _fetch_flow_end(self, kind):
        self._remove_key()
        if self.flow_level:
            self.flow_level -= 1
        self.allow_simple_key = False
        start = self._mark()
        self._forward()
        self.tokens.append(Token(kind, start, self._mark()))

    def _fetch_flow_entry(self):
        self.allow_simple_key = True
        self._remove_key()
        start = self._mark()
        self._forward()
        self.tokens.append(Token(",", start, self._mark()))

    def _fetch_block_entry(self):
        if not self.flow_level:
            if not self.allow_simple_key:
                raise ScannerError(None, None, "sequence entries are not allowed here", self._mark())
            if self._add_indent(self.column):
                mark = self._mark()
                self.tokens.append(Token("<block sequence start>", mark, mark))
        self.allow_simple_key = True
        self._remove_key()
        start = self._mark()
        self._forward()
        self.tokens.append(Token("-", start, self._mark()))

    def _fetch_key(self):
        if not self.flow_level:
            if not self.allow_simple_key:
                raise ScannerError(None, None, "mapping keys are not allowed here", self._mark())
            if self._add_indent(self.column):
                mark = self._mark()
                self.tokens.append(Token("<block mapping start>", mark, mark))
        self.allow_simple_key = not self.flow_level
        self._remove_key()
        start = self._mark()
        self._forward()
        self.tokens.append(Token("?", start, self._mark()))

    def _fetch_value(self):
        if self.flow_level in self.possible_simple_keys:
            key = self.possible_simple_keys[self.flow_level]
            del self.possible_simple_keys[self.flow_level]
            self.tokens.insert(key.token_number - self.tokens_taken,
                               Token("?", key.mark, key.mark))
            if not self.flow_level:
                if self._add_indent(key.column):
                    self.tokens.insert(key.token_number - self.tokens_taken,
                                       Token("<block mapping start>", key.mark, key.mark))
            self.allow_simple_key = False
        else:
            if not self.flow_level:
                if not self.allow_simple_key:
                    raise ScannerError(None, None, "mapping values are not allowed here", self._mark())
            if not self.flow_level:
                if self._add_indent(self.column):
                    mark = self._mark()
                    self.tokens.append(Token("<block mapping start>", mark, mark))
            self.allow_simple_key = not self.flow_level
            self._remove_key()
        start = self._mark()
        self._forward()
        self.tokens.append(Token(":", start, self._mark()))

    def _fetch_anchor(self, kind):
        self._save_key()
        self.allow_simple_key = False
        self.tokens.append(self._scan_anchor(kind))

    def _fetch_tag(self):
        self._save_key()
        self.allow_simple_key = False
        self.tokens.append(self._scan_tag())

    def _fetch_block_scalar(self, style):
        self.allow_simple_key = True
        self._remove_key()
        self.tokens.append(self._scan_block_scalar(style))

    def _fetch_flow_scalar(self, style):
        self._save_key()
        self.allow_simple_key = False
        self.tokens.append(self._scan_flow_scalar(style))

    def _fetch_plain(self):
        self._save_key()
        self.allow_simple_key = False
        self.tokens.append(self._scan_plain())

    # -- scanners
    def _scan_directive(self):
        start = self._mark()
        self._forward()
        length = 0
        while self._peek(length).isalnum() or self._peek(length) in "-_":
            length += 1
        name = self._prefix(length)
        self._forward(length)
        if name == "YAML":
            while self._peek() == " ":
                self._forward()
            major = self._scan_version_number(start)
            if self._peek() != ".":
                raise ScannerError("while scanning a directive", start,
                                   "expected a digit or '.', but found %r" % self._peek(), self._mark())
            self._forward()
            minor = self._scan_version_number(start)
            value = (major, minor)
            end = self._mark()
        elif name == "TAG":
            while self._peek() == " ":
                self._forward()
            handle = self._scan_tag_handle("directive", start)
            if self._peek() != " ":
                raise ScannerError("while scanning a directive", start,
                                   "expected ' ', but found %r" % self._peek(), self._mark())
            while self._peek() == " ":
                self._forward()
            prefix = self._scan_tag_uri("directive", start)
            value = (handle, prefix)
            end = self._mark()
        else:
            end = self._mark()
            value = None
            while self._peek() not in "\0\r\n\x85\u2028\u2029":
                self._forward()
        while self._peek() == " ":
            self._forward()
        if self._peek() == "#":
            while self._peek() not in "\0\r\n\x85\u2028\u2029":
                self._forward()
        if self._peek() not in "\0\r\n\x85\u2028\u2029":
            raise ScannerError("while scanning a directive", start,
                               "expected a comment or a line break, but found %r" % self._peek(),
                               self._mark())
        self._scan_line_break()
        return Token("<directive>", start, end, (name, value))

    def _scan_version_number(self, start):
        length = 0
        while "0" <= self._peek(length) <= "9":
            length += 1
        if not length:
            raise ScannerError("while scanning a directive", start,
                               "expected a digit, but found %r" % self._peek(), self._mark())
        value = int(self._prefix(length))
        self._forward(length)
        return value

    def _scan_anchor(self, kind):
        start = self._mark()
        what = "alias" if kind == "<alias>" else "anchor"
        self._forward()
        length = 0
        ch = self._peek(length)
        while ("0" <= ch <= "9" or "A" <= ch <= "Z" or "a" <= ch <= "z" or ch in "-_"):
            length += 1
            ch = self._peek(length)
        if not length:
            raise ScannerError("while scanning an %s" % what, start,
                               "expected alphabetic or numeric character, but found %r" % ch,
                               self._mark())
        value = self._prefix(length)
        self._forward(length)
        ch = self._peek()
        if ch not in "\0 \t\r\n\x85\u2028\u2029?:,]}%@`":
            raise ScannerError("while scanning an %s" % what, start,
                               "expected alphabetic or numeric character, but found %r" % ch,
                               self._mark())
        return Token(kind, start, self._mark(), value)

    def _scan_tag(self):
        start = self._mark()
        ch = self._peek(1)
        if ch == "<":
            handle = None
            self._forward(2)
            suffix = self._scan_tag_uri("tag", start)
            if self._peek() != ">":
                raise ScannerError("while parsing a tag", start,
                                   "expected '>', but found %r" % self._peek(), self._mark())
            self._forward()
        elif ch in _BLANKZ:
            handle = None
            suffix = "!"
            self._forward()
        else:
            length = 1
            use_handle = False
            while ch not in _BLANKZ:
                if ch == "!":
                    use_handle = True
                    break
                length += 1
                ch = self._peek(length)
            if use_handle:
                handle = self._scan_tag_handle("tag", start)
            else:
                handle = "!"
                self._forward()
            suffix = self._scan_tag_uri("tag", start)
        ch = self._peek()
        if ch not in "\0 \r\n\x85\u2028\u2029":
            raise ScannerError("while scanning a tag", start,
                               "expected ' ', but found %r" % ch, self._mark())
        return Token("<tag>", start, self._mark(), (handle, suffix))

    def _scan_tag_handle(self, name, start):
        ch = self._peek()
        if ch != "!":
            raise ScannerError("while scanning a %s" % name, start,
                               "expected '!', but found %r" % ch, self._mark())
        length = 1
        ch = self._peek(length)
        if ch != " ":
            while ("0" <= ch <= "9" or "A" <= ch <= "Z" or "a" <= ch <= "z" or ch in "-_"):
                length += 1
                ch = self._peek(length)
            if ch != "!":
                self._forward(length)
                raise ScannerError("while scanning a %s" % name, start,
                                   "expected '!', but found %r" % ch, self._mark())
            length += 1
        value = self._prefix(length)
        self._forward(length)
        return value

    def _scan_tag_uri(self, name, start):
        chunks = []
        length = 0
        ch = self._peek(length)
        while ("0" <= ch <= "9" or "A" <= ch <= "Z" or "a" <= ch <= "z"
               or ch in "-;/?:@&=+$,_.!~*'()[]%"):
            if ch == "%":
                chunks.append(self._prefix(length))
                self._forward(length)
                length = 0
                chunks.append(self._scan_uri_escapes(name, start))
            else:
                length += 1
            ch = self._peek(length)
        if length:
            chunks.append(self._prefix(length))
            self._forward(length)
        if not chunks:
            raise ScannerError("while parsing a %s" % name, start,
                               "expected URI, but found %r" % ch, self._mark())
        return "".join(chunks)

    def _scan_uri_escapes(self, name, start):
        data = []
        mark = self._mark()
        while self._peek() == "%":
            self._forward()
            for k in range(2):
                if self._peek(k) not in _HEXDIGITS:
                    raise ScannerError("while scanning a %s" % name, start,
                                       "expected URI escape sequence of 2 hexadecimal numbers, but found %r"
                                       % self._peek(k), self._mark())
            data.append(int(self._prefix(2), 16))
            self._forward(2)
        try:
            return bytes(data).decode("utf-8")
        except UnicodeDecodeError as exc:
            raise ScannerError("while scanning a %s" % name, start, str(exc), mark)

    def _scan_block_scalar(self, style):
        folded = style == ">"
        chunks = []
        start = self._mark()
        self._forward()
        chomping, increment = self._scan_block_indicators(start)
        self._scan_block_ignored_line(start)
        min_indent = self.indent + 1
        if min_indent < 1:
            min_indent = 1
        if increment is None:
            breaks, max_indent, end = self._scan_block_indentation()
            indent = max(min_indent, max_indent)
        else:
            indent = min_indent + increment - 1
            breaks, end = self._scan_block_breaks(indent)
        line_break = ""
        while self.column == indent and self._peek() != "\0":
            chunks.extend(breaks)
            leading_non_space = self._peek() not in " \t"
            length = 0
            while self._peek(length) not in "\0\r\n\x85\u2028\u2029":
                length += 1
            chunks.append(self._prefix(length))
            self._forward(length)
            line_break = self._scan_line_break()
            breaks, end = self._scan_block_breaks(indent)
            if self.column == indent and self._peek() != "\0":
                if folded and line_break == "\n" and leading_non_space and self._peek() not in " \t":
                    if not breaks:
                        chunks.append(" ")
                else:
                    chunks.append(line_break)
            else:
                break
        if chomping is not False:
            chunks.append(line_break)
        if chomping is True:
            chunks.extend(breaks)
        return Token("<scalar>", start, end, "".join(chunks), False, style)

    def _scan_block_indicators(self, start):
        chomping = None
        increment = None
        ch = self._peek()
        if ch in "+-":
            chomping = ch == "+"
            self._forward()
            ch = self._peek()
            if ch in "0123456789":
                increment = int(ch)
                if increment == 0:
                    raise ScannerError("while scanning a block scalar", start,
                                       "expected indentation indicator in the range 1-9, but found 0",
                                       self._mark())
                self._forward()
        elif ch in "0123456789":
            increment = int(ch)
            if increment == 0:
                raise ScannerError("while scanning a block scalar", start,
                                   "expected indentation indicator in the range 1-9, but found 0",
                                   self._mark())
            self._forward()
            ch = self._peek()
            if ch in "+-":
                chomping = ch == "+"
                self._forward()
        ch = self._peek()
        if ch not in "\0 \r\n\x85\u2028\u2029":
            raise ScannerError("while scanning a block scalar", start,
                               "expected chomping or indentation indicators, but found %r" % ch,
                               self._mark())
        return chomping, increment

    def _scan_block_ignored_line(self, start):
        while self._peek() == " ":
            self._forward()
        if self._peek() == "#":
            while self._peek() not in "\0\r\n\x85\u2028\u2029":
                self._forward()
        ch = self._peek()
        if ch not in "\0\r\n\x85\u2028\u2029":
            raise ScannerError("while scanning a block scalar", start,
                               "expected a comment or a line break, but found %r" % ch, self._mark())
        self._scan_line_break()

    def _scan_block_indentation(self):
        chunks = []
        max_indent = 0
        end = self._mark()
        while self._peek() in " \r\n\x85\u2028\u2029":
            if self._peek() != " ":
                chunks.append(self._scan_line_break())
                end = self._mark()
            else:
                self._forward()
                if self.column > max_indent:
                    max_indent = self.column
        return chunks, max_indent, end

    def _scan_block_breaks(self, indent):
        chunks = []
        end = self._mark()
        while self.column < indent and self._peek() == " ":
            self._forward()
        while self._peek() in "\r\n\x85\u2028\u2029":
            chunks.append(self._scan_line_break())
            end = self._mark()
            while self.column < indent and self._peek() == " ":
                self._forward()
        return chunks, end

    def _scan_flow_scalar(self, style):
        double = style == '"'
        chunks = []
        start = self._mark()
        quote = self._peek()
        self._forward()
        chunks.extend(self._scan_flow_non_spaces(double, start))
        while self._peek() != quote:
            chunks.extend(self._scan_flow_spaces(double, start))
            chunks.extend(self._scan_flow_non_spaces(double, start))
        self._forward()
        return Token("<scalar>", start, self._mark(), "".join(chunks), False, style)

    def _scan_flow_non_spaces(self, double, start):
        chunks = []
        while True:
            length = 0
            while self._peek(length) not in "'\"\\\0 \t\r\n\x85\u2028\u2029":
                length += 1
            if length:
                chunks.append(self._prefix(length))
                self._forward(length)
            ch = self._peek()
            if not double and ch == "'" and self._peek(1) == "'":
                chunks.append("'")
                self._forward(2)
            elif (double and ch == "'") or (not double and ch in '"\\'):
                chunks.append(ch)
                self._forward()
            elif double and ch == "\\":
                self._forward()
                ch = self._peek()
                if ch in _ESC_REPL:
                    chunks.append(_ESC_REPL[ch])
                    self._forward()
                elif ch in _ESC_CODES:
                    length = _ESC_CODES[ch]
                    self._forward()
                    for k in range(length):
                        if self._peek(k) not in _HEXDIGITS:
                            raise ScannerError(
                                "while scanning a double-quoted scalar", start,
                                "expected escape sequence of %d hexadecimal numbers, but found %r"
                                % (length, self._peek(k)), self._mark())
                    code = int(self._prefix(length), 16)
                    chunks.append(chr(code))
                    self._forward(length)
                elif ch in "\r\n\x85\u2028\u2029":
                    self._scan_line_break()
                    chunks.extend(self._scan_flow_breaks(double, start))
                else:
                    raise ScannerError("while scanning a double-quoted scalar", start,
                                       "found unknown escape character %r" % ch, self._mark())
            else:
                return chunks

    def _scan_flow_spaces(self, double, start):
        chunks = []
        length = 0
        while self._peek(length) in " \t":
            length += 1
        whitespaces = self._prefix(length)
        self._forward(length)
        ch = self._peek()
        if ch == "\0":
            raise ScannerError("while scanning a quoted scalar", start,
                               "found unexpected end of stream", self._mark())
        elif ch in "\r\n\x85\u2028\u2029":
            line_break = self._scan_line_break()
            breaks = self._scan_flow_breaks(double, start)
            if line_break != "\n":
                chunks.append(line_break)
            elif not breaks:
                chunks.append(" ")
            chunks.extend(breaks)
        else:
            chunks.append(whitespaces)
        return chunks

    def _scan_flow_breaks(self, double, start):
        chunks = []
        while True:
            prefix = self._prefix(3)
            if (prefix == "---" or prefix == "...") and self._peek(3) in _BLANKZ:
                raise ScannerError("while scanning a quoted scalar", start,
                                   "found unexpected document separator", self._mark())
            while self._peek() in " \t":
                self._forward()
            if self._peek() in "\r\n\x85\u2028\u2029":
                chunks.append(self._scan_line_break())
            else:
                return chunks

    def _scan_plain(self):
        chunks = []
        start = end = self._mark()
        indent = self.indent + 1
        spaces = []
        while True:
            length = 0
            if self._peek() == "#":
                break
            while True:
                ch = self._peek(length)
                if (ch in _BLANKZ
                        or (ch == ":" and self._peek(length + 1) in
                            (_BLANKZ + ",[]{}" if self.flow_level else _BLANKZ))
                        or (self.flow_level and ch in ",?[]{}")):
                    break
                length += 1
            if length == 0:
                break
            self.allow_simple_key = False
            chunks.extend(spaces)
            chunks.append(self._prefix(length))
            self._forward(length)
            end = self._mark()
            spaces = self._scan_plain_spaces()
            if not spaces or self._peek() == "#" or (not self.flow_level and self.column < indent):
                break
        return Token("<scalar>", start, end, "".join(chunks), True)

    def _scan_plain_spaces(self):
        chunks = []
        length = 0
        while self._peek(length) in " \t":
            length += 1
        whitespaces = self._prefix(length)
        self._forward(length)
        ch = self._peek()
        if ch in "\r\n\x85\u2028\u2029":
            line_break = self._scan_line_break()
            self.allow_simple_key = True
            prefix = self._prefix(3)
            if (prefix == "---" or prefix == "...") and self._peek(3) in _BLANKZ:
                return []
            breaks = []
            while self._peek() in " \r\n\x85\u2028\u2029":
                if self._peek() == " ":
                    self._forward()
                else:
                    breaks.append(self._scan_line_break())
                    prefix = self._prefix(3)
                    if (prefix == "---" or prefix == "...") and self._peek(3) in _BLANKZ:
                        return []
            if line_break != "\n":
                chunks.append(line_break)
            elif not breaks:
                chunks.append(" ")
            chunks.extend(breaks)
        elif whitespaces:
            chunks.append(whitespaces)
        return chunks


# ------------------------------------------------------------ resolvers

DEFAULT_SCALAR_TAG = "tag:yaml.org,2002:str"
DEFAULT_SEQUENCE_TAG = "tag:yaml.org,2002:seq"
DEFAULT_MAPPING_TAG = "tag:yaml.org,2002:map"

_RE_BOOL = re.compile(r"^(?:yes|Yes|YES|no|No|NO|true|True|TRUE|false|False|FALSE|on|On|ON|off|Off|OFF)$")
_RE_FLOAT = re.compile(
    r"^(?:[-+]?(?:[0-9][0-9_]*)\.[0-9_]*(?:[eE][-+][0-9]+)?"
    r"|\.[0-9][0-9_]*(?:[eE][-+][0-9]+)?"
    r"|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\.[0-9_]*"
    r"|[-+]?\.(?:inf|Inf|INF)"
    r"|\.(?:nan|NaN|NAN))$")
_RE_INT = re.compile(
    r"^(?:[-+]?0b[0-1_]+"
    r"|[-+]?0[0-7_]+"
    r"|[-+]?(?:0|[1-9][0-9_]*)"
    r"|[-+]?0x[0-9a-fA-F_]+"
    r"|[-+]?[1-9][0-9_]*(?::[0-5]?[0-9])+)$")
_RE_MERGE = re.compile(r"^(?:<<)$")
_RE_NULL = re.compile(r"^(?:~|null|Null|NULL|)$")
_RE_TIMESTAMP = re.compile(
    r"^(?:[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]"
    r"|[0-9][0-9][0-9][0-9]-[0-9][0-9]?-[0-9][0-9]?"
    r"(?:[Tt]|[ \t]+)[0-9][0-9]?:[0-9][0-9]:[0-9][0-9]"
    r"(?:\.[0-9]*)?(?:[ \t]*(?:Z|[-+][0-9][0-9]?(?::[0-9][0-9])?))?)$")
_RE_TS_PARTS = re.compile(
    r"^(?P<year>[0-9][0-9][0-9][0-9])-(?P<month>[0-9][0-9]?)-(?P<day>[0-9][0-9]?)"
    r"(?:(?:[Tt]|[ \t]+)(?P<hour>[0-9][0-9]?):(?P<minute>[0-9][0-9]):(?P<second>[0-9][0-9])"
    r"(?:\.(?P<fraction>[0-9]*))?"
    r"(?:[ \t]*(?P<tz>Z|(?P<tz_sign>[-+])(?P<tz_hour>[0-9][0-9]?)(?::(?P<tz_minute>[0-9][0-9]))?))?)?$")
_RE_VALUE = re.compile(r"^(?:=)$")


def _default_resolvers():
    table = {}

    def add(tag, regexp, first):
        for ch in first:
            table.setdefault(ch, []).append((tag, regexp))

    add("tag:yaml.org,2002:bool", _RE_BOOL, list("yYnNtTfFoO"))
    add("tag:yaml.org,2002:float", _RE_FLOAT, list("-+0123456789."))
    add("tag:yaml.org,2002:int", _RE_INT, list("-+0123456789"))
    add("tag:yaml.org,2002:merge", _RE_MERGE, ["<"])
    add("tag:yaml.org,2002:null", _RE_NULL, ["~", "n", "N", ""])
    add("tag:yaml.org,2002:timestamp", _RE_TIMESTAMP, list("0123456789"))
    add("tag:yaml.org,2002:value", _RE_VALUE, ["="])
    return table


def _resolve(resolvers, kind, value, implicit):
    if kind is ScalarNode and implicit[0]:
        if value == "":
            found = resolvers.get("", [])
        else:
            found = resolvers.get(value[0], [])
        found = list(found) + list(resolvers.get(None, []))
        for tag, regexp in found:
            if regexp.match(value):
                return tag
    if kind is ScalarNode:
        return DEFAULT_SCALAR_TAG
    if kind is SequenceNode:
        return DEFAULT_SEQUENCE_TAG
    return DEFAULT_MAPPING_TAG


def _copy_resolvers(table):
    out = {}
    for k in table:
        out[k] = list(table[k])
    return out


# --------------------------------------------------------------- loader

def _is_hashable(key):
    if isinstance(key, (list, dict, set, bytearray)):
        return False
    if isinstance(key, tuple):
        for item in key:
            if not _is_hashable(item):
                return False
    return True


class _LoaderCore(_Scanner):
    yaml_constructors = {}
    yaml_multi_constructors = {}
    yaml_implicit_resolvers = {}

    def __init__(self, stream):
        self._init_reader(stream)
        self._init_scanner(self._text, self._name)
        self._started = False
        self._first = True
        self._ended = False
        self._pending = False
        self._doc_mark = None
        self.tag_handles = {}
        self.yaml_version = None
        self.anchors = {}
        self.constructed_objects = {}
        self.recursive_objects = {}
        self.state_generators = []
        self.deep_construct = False

    # -- reader
    def _init_reader(self, stream):
        if isinstance(stream, str):
            self._name = "<unicode string>"
            text = stream
        elif isinstance(stream, (bytes, bytearray)):
            self._name = "<byte string>"
            text = self._decode(bytes(stream))
        else:
            self._name = getattr(stream, "name", "<file>")
            data = stream.read()
            if isinstance(data, str):
                text = data
            else:
                text = self._decode(bytes(data))
        i = 0
        n = len(text)
        while i < n:
            o = ord(text[i])
            if not (o == 9 or o == 10 or o == 13 or 0x20 <= o <= 0x7e or o == 0x85
                    or 0xa0 <= o <= 0xd7ff or 0xe000 <= o <= 0xfffd or 0x10000 <= o <= 0x10ffff):
                raise ReaderError(self._name, i, o, "unicode", "special characters are not allowed")
            i += 1
        self._text = text

    def _decode(self, data):
        try:
            if data.startswith(b"\xff\xfe"):
                return data[2:].decode("utf-16-le")
            if data.startswith(b"\xfe\xff"):
                return data[2:].decode("utf-16-be")
            return data.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise ReaderError(self._name, exc.start, data[exc.start:exc.start + 1], "utf-8", exc.reason)

    def dispose(self):
        pass

    # -- parser / composer
    def check_node(self):
        if self._pending:
            return True
        if self._has_document():
            self._pending = True
            return True
        return False

    def get_node(self):
        if self.check_node():
            self._pending = False
            return self._compose_document()
        return None

    def get_single_node(self):
        document = None
        if self.check_node():
            document = self.get_node()
        if self.check_node():
            raise ComposerError("expected a single document in the stream", document.start_mark,
                                "but found another document", self._doc_mark)
        return document

    def _has_document(self):
        if self._ended:
            return False
        if not self._started:
            self._get_token()
            self._started = True
        if self._first:
            self._first = False
            if not self._check_token("<directive>", "<document start>", "<stream end>"):
                self.tag_handles = {"!": "!", "!!": "tag:yaml.org,2002:"}
                self.yaml_version = None
                self._doc_mark = self._peek_token().start_mark
                return True
        while self._check_token("<document end>"):
            self._get_token()
        if self._check_token("<stream end>"):
            self._get_token()
            self._ended = True
            return False
        token = self._peek_token()
        self._doc_mark = token.start_mark
        self._process_directives()
        if not self._check_token("<document start>"):
            token = self._peek_token()
            raise ParserError(None, None,
                              "expected '<document start>', but found %r" % token.id, token.start_mark)
        self._get_token()
        return True

    def _process_directives(self):
        self.yaml_version = None
        self.tag_handles = {}
        while self._check_token("<directive>"):
            token = self._get_token()
            if token.value[0] == "YAML":
                if self.yaml_version is not None:
                    raise ParserError(None, None, "found duplicate YAML directive", token.start_mark)
                major, minor = token.value[1]
                if major != 1:
                    raise ParserError(None, None,
                                      "found incompatible YAML document (version 1.* is required)",
                                      token.start_mark)
                self.yaml_version = token.value[1]
            elif token.value[0] == "TAG":
                handle, prefix = token.value[1]
                if handle in self.tag_handles:
                    raise ParserError(None, None, "duplicate tag handle %r" % handle, token.start_mark)
                self.tag_handles[handle] = prefix
        for key in ("!", "!!"):
            if key not in self.tag_handles:
                self.tag_handles[key] = "!" if key == "!" else "tag:yaml.org,2002:"

    def _compose_document(self):
        self.anchors = {}
        if self._check_token("<directive>", "<document start>", "<document end>", "<stream end>"):
            token = self._peek_token()
            node = self._empty_scalar(token.start_mark, True)
        else:
            node = self._parse_node(True, False)
        if self._check_token("<document end>"):
            self._get_token()
        self.anchors = {}
        return node

    def _empty_scalar(self, mark, implicit_plain=True, anchor=None, tag=None):
        if tag is None or tag == "!":
            tag = _resolve(self.yaml_implicit_resolvers, ScalarNode, "", (True, False))
        node = ScalarNode(tag, "", mark, mark, None)
        if anchor is not None:
            self._register_anchor(anchor, node, mark)
        return node

    def _register_anchor(self, anchor, node, mark):
        if anchor in self.anchors:
            raise ComposerError("found duplicate anchor %r; first occurrence" % anchor,
                                self.anchors[anchor].start_mark, "second occurrence", mark)
        self.anchors[anchor] = node

    def _parse_node(self, block=False, indentless=False):
        if self._check_token("<alias>"):
            token = self._get_token()
            if token.value not in self.anchors:
                raise ComposerError(None, None, "found undefined alias %r" % token.value,
                                    token.start_mark)
            return self.anchors[token.value]
        anchor = None
        tag = None
        start_mark = end_mark = tag_mark = None
        if self._check_token("<anchor>"):
            token = self._get_token()
            start_mark = token.start_mark
            end_mark = token.end_mark
            anchor = token.value
            if self._check_token("<tag>"):
                token = self._get_token()
                tag_mark = token.start_mark
                end_mark = token.end_mark
                tag = token.value
        elif self._check_token("<tag>"):
            token = self._get_token()
            start_mark = tag_mark = token.start_mark
            end_mark = token.end_mark
            tag = token.value
            if self._check_token("<anchor>"):
                token = self._get_token()
                end_mark = token.end_mark
                anchor = token.value
        if tag is not None:
            handle, suffix = tag
            if handle is not None:
                if handle not in self.tag_handles:
                    raise ParserError("while parsing a node", start_mark,
                                      "found undefined tag handle %r" % handle, tag_mark)
                tag = self.tag_handles[handle] + suffix
            else:
                tag = suffix
        if start_mark is None:
            start_mark = end_mark = self._peek_token().start_mark
        implicit = tag is None or tag == "!"
        if indentless and self._check_token("-"):
            end_mark = self._peek_token().end_mark
            node = SequenceNode(self._coll_tag(tag, SequenceNode), [], start_mark, end_mark, False)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            self._parse_indentless_sequence(node)
            return node
        if self._check_token("<scalar>"):
            token = self._get_token()
            end_mark = token.end_mark
            if (token.plain and tag is None) or tag == "!":
                imp = (True, False)
            elif tag is None:
                imp = (False, True)
            else:
                imp = (False, False)
            node_tag = tag
            if tag is None or tag == "!":
                node_tag = _resolve(self.yaml_implicit_resolvers, ScalarNode, token.value, imp)
            node = ScalarNode(node_tag, token.value, start_mark, end_mark, token.style)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            return node
        if self._check_token("["):
            end_mark = self._peek_token().end_mark
            node = SequenceNode(self._coll_tag(tag, SequenceNode), [], start_mark, end_mark, True)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            self._parse_flow_sequence(node)
            return node
        if self._check_token("{"):
            end_mark = self._peek_token().end_mark
            node = MappingNode(self._coll_tag(tag, MappingNode), [], start_mark, end_mark, True)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            self._parse_flow_mapping(node)
            return node
        if block and self._check_token("<block sequence start>"):
            end_mark = self._peek_token().start_mark
            node = SequenceNode(self._coll_tag(tag, SequenceNode), [], start_mark, end_mark, False)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            self._parse_block_sequence(node)
            return node
        if block and self._check_token("<block mapping start>"):
            end_mark = self._peek_token().start_mark
            node = MappingNode(self._coll_tag(tag, MappingNode), [], start_mark, end_mark, False)
            if anchor is not None:
                self._register_anchor(anchor, node, start_mark)
            self._parse_block_mapping(node)
            return node
        if anchor is not None or tag is not None:
            return self._empty_scalar(end_mark, True, anchor, tag)
        what = "block" if block else "flow"
        token = self._peek_token()
        raise ParserError("while parsing a %s node" % what, start_mark,
                          "expected the node content, but found %r" % token.id, token.start_mark)

    def _coll_tag(self, tag, kind):
        if tag is None or tag == "!":
            return _resolve(self.yaml_implicit_resolvers, kind, None, True)
        return tag

    def _parse_block_sequence(self, node):
        token = self._get_token()
        mark = token.start_mark
        while self._check_token("-"):
            token = self._get_token()
            if not self._check_token("-", "<block end>"):
                node.value.append(self._parse_node(True, False))
            else:
                node.value.append(self._empty_scalar(token.end_mark))
        if not self._check_token("<block end>"):
            token = self._peek_token()
            raise ParserError("while parsing a block collection", mark,
                              "expected <block end>, but found %r" % token.id, token.start_mark)
        token = self._get_token()
        node.end_mark = token.end_mark

    def _parse_indentless_sequence(self, node):
        while self._check_token("-"):
            token = self._get_token()
            if not self._check_token("-", "?", ":", "<block end>"):
                node.value.append(self._parse_node(True, False))
            else:
                node.value.append(self._empty_scalar(token.end_mark))
        node.end_mark = self._peek_token().start_mark

    def _parse_block_mapping(self, node):
        token = self._get_token()
        mark = token.start_mark
        while self._check_token("?", ":"):
            if self._check_token("?"):
                token = self._get_token()
                if not self._check_token("?", ":", "<block end>"):
                    key = self._parse_node(True, True)
                else:
                    key = self._empty_scalar(token.end_mark)
            else:
                key = self._empty_scalar(self._peek_token().start_mark)
            if self._check_token(":"):
                token = self._get_token()
                if not self._check_token("?", ":", "<block end>"):
                    value = self._parse_node(True, True)
                else:
                    value = self._empty_scalar(token.end_mark)
            else:
                value = self._empty_scalar(self._peek_token().start_mark)
            node.value.append((key, value))
        if not self._check_token("<block end>"):
            token = self._peek_token()
            raise ParserError("while parsing a block mapping", mark,
                              "expected <block end>, but found %r" % token.id, token.start_mark)
        token = self._get_token()
        node.end_mark = token.end_mark

    def _parse_flow_sequence(self, node):
        token = self._get_token()
        mark = token.start_mark
        first = True
        while not self._check_token("]"):
            if not first:
                if self._check_token(","):
                    self._get_token()
                else:
                    token = self._peek_token()
                    raise ParserError("while parsing a flow sequence", mark,
                                      "expected ',' or ']', but got %r" % token.id, token.start_mark)
            first = False
            if self._check_token("?"):
                token = self._get_token()
                pair = MappingNode(DEFAULT_MAPPING_TAG, [], token.start_mark, token.end_mark, True)
                if not self._check_token(":", ",", "]"):
                    key = self._parse_node(False, False)
                else:
                    key = self._empty_scalar(token.end_mark)
                if self._check_token(":"):
                    token = self._get_token()
                    if not self._check_token(",", "]"):
                        value = self._parse_node(False, False)
                    else:
                        value = self._empty_scalar(token.end_mark)
                else:
                    value = self._empty_scalar(self._peek_token().start_mark)
                pair.value.append((key, value))
                node.value.append(pair)
            elif not self._check_token("]"):
                node.value.append(self._parse_node(False, False))
        token = self._get_token()
        node.end_mark = token.end_mark

    def _parse_flow_mapping(self, node):
        token = self._get_token()
        mark = token.start_mark
        first = True
        while not self._check_token("}"):
            if not first:
                if self._check_token(","):
                    self._get_token()
                else:
                    token = self._peek_token()
                    raise ParserError("while parsing a flow mapping", mark,
                                      "expected ',' or '}', but got %r" % token.id, token.start_mark)
            first = False
            if self._check_token("?"):
                token = self._get_token()
                if not self._check_token(":", ",", "}"):
                    key = self._parse_node(False, False)
                else:
                    key = self._empty_scalar(token.end_mark)
                if self._check_token(":"):
                    token = self._get_token()
                    if not self._check_token(",", "}"):
                        value = self._parse_node(False, False)
                    else:
                        value = self._empty_scalar(token.end_mark)
                else:
                    value = self._empty_scalar(self._peek_token().start_mark)
                node.value.append((key, value))
            elif not self._check_token("}"):
                key = self._parse_node(False, False)
                node.value.append((key, self._empty_scalar(self._peek_token().start_mark)))
        token = self._get_token()
        node.end_mark = token.end_mark

    # -- constructor
    def check_data(self):
        return self.check_node()

    def get_data(self):
        if self.check_node():
            return self.construct_document(self.get_node())
        return None

    def get_single_data(self):
        node = self.get_single_node()
        if node is not None:
            return self.construct_document(node)
        return None

    def construct_document(self, node):
        data = self.construct_object(node)
        while self.state_generators:
            gens = self.state_generators
            self.state_generators = []
            for gen in gens:
                for _ in gen:
                    pass
        self.constructed_objects = {}
        self.recursive_objects = {}
        self.deep_construct = False
        return data

    def construct_object(self, node, deep=False):
        key = id(node)
        if key in self.constructed_objects:
            return self.constructed_objects[key]
        if deep:
            old_deep = self.deep_construct
            self.deep_construct = True
        if key in self.recursive_objects:
            raise ConstructorError(None, None, "found unconstructable recursive node", node.start_mark)
        self.recursive_objects[key] = None
        constructor = None
        tag_suffix = None
        if node.tag in self.yaml_constructors:
            constructor = self.yaml_constructors[node.tag]
        else:
            found = False
            for prefix in self.yaml_multi_constructors:
                if prefix is not None and node.tag.startswith(prefix):
                    tag_suffix = node.tag[len(prefix):]
                    constructor = self.yaml_multi_constructors[prefix]
                    found = True
                    break
            if not found:
                if None in self.yaml_multi_constructors:
                    tag_suffix = node.tag
                    constructor = self.yaml_multi_constructors[None]
                elif None in self.yaml_constructors:
                    constructor = self.yaml_constructors[None]
                elif isinstance(node, ScalarNode):
                    constructor = _LoaderCore.construct_scalar
                elif isinstance(node, SequenceNode):
                    constructor = _LoaderCore.construct_sequence
                else:
                    constructor = _LoaderCore.construct_mapping
        if tag_suffix is None:
            data = constructor(self, node)
        else:
            data = constructor(self, tag_suffix, node)
        if hasattr(data, "__next__"):
            gen = data
            data = next(gen)
            if self.deep_construct:
                for _ in gen:
                    pass
            else:
                self.state_generators.append(gen)
        self.constructed_objects[key] = data
        del self.recursive_objects[key]
        if deep:
            self.deep_construct = old_deep
        return data

    def construct_scalar(self, node):
        if not isinstance(node, ScalarNode):
            raise ConstructorError(None, None, "expected a scalar node, but found %s" % node.id,
                                   node.start_mark)
        return node.value

    def construct_sequence(self, node, deep=False):
        if not isinstance(node, SequenceNode):
            raise ConstructorError(None, None, "expected a sequence node, but found %s" % node.id,
                                   node.start_mark)
        return [self.construct_object(child, deep=deep) for child in node.value]

    def flatten_mapping(self, node):
        merge = []
        index = 0
        while index < len(node.value):
            key_node, value_node = node.value[index]
            if key_node.tag == "tag:yaml.org,2002:merge":
                del node.value[index]
                if isinstance(value_node, MappingNode):
                    self.flatten_mapping(value_node)
                    merge.extend(value_node.value)
                elif isinstance(value_node, SequenceNode):
                    submerge = []
                    for subnode in value_node.value:
                        if not isinstance(subnode, MappingNode):
                            raise ConstructorError("while constructing a mapping", node.start_mark,
                                                   "expected a mapping for merging, but found %s"
                                                   % subnode.id, subnode.start_mark)
                        self.flatten_mapping(subnode)
                        submerge.append(subnode.value)
                    submerge.reverse()
                    for value in submerge:
                        merge.extend(value)
                else:
                    raise ConstructorError("while constructing a mapping", node.start_mark,
                                           "expected a mapping or list of mappings for merging, but found %s"
                                           % value_node.id, value_node.start_mark)
            elif key_node.tag == "tag:yaml.org,2002:value":
                key_node.tag = "tag:yaml.org,2002:str"
                index += 1
            else:
                index += 1
        if merge:
            node.value = merge + node.value

    def construct_mapping(self, node, deep=False):
        if not isinstance(node, MappingNode):
            raise ConstructorError(None, None, "expected a mapping node, but found %s" % node.id,
                                   node.start_mark)
        self.flatten_mapping(node)
        mapping = {}
        for key_node, value_node in node.value:
            key = self.construct_object(key_node, deep=deep)
            if not _is_hashable(key):
                raise ConstructorError("while constructing a mapping", node.start_mark,
                                       "found unhashable key", key_node.start_mark)
            mapping[key] = self.construct_object(value_node, deep=deep)
        return mapping

    def construct_pairs(self, node, deep=False):
        if not isinstance(node, MappingNode):
            raise ConstructorError(None, None, "expected a mapping node, but found %s" % node.id,
                                   node.start_mark)
        pairs = []
        for key_node, value_node in node.value:
            pairs.append((self.construct_object(key_node, deep=deep),
                          self.construct_object(value_node, deep=deep)))
        return pairs

    # -- the standard tags
    def construct_yaml_null(self, node):
        self.construct_scalar(node)
        return None

    def construct_yaml_bool(self, node):
        value = self.construct_scalar(node)
        return value.lower() in ("yes", "true", "on")

    def construct_yaml_int(self, node):
        value = self.construct_scalar(node)
        value = value.replace("_", "")
        sign = 1
        if value[0] == "-":
            sign = -1
        if value[0] in "+-":
            value = value[1:]
        if value == "0":
            return 0
        if value.startswith("0b"):
            return sign * int(value[2:], 2)
        if value.startswith("0x"):
            return sign * int(value[2:], 16)
        if value[0] == "0":
            return sign * int(value, 8)
        if ":" in value:
            digits = [int(part) for part in value.split(":")]
            digits.reverse()
            base = 1
            total = 0
            for digit in digits:
                total += digit * base
                base *= 60
            return sign * total
        return sign * int(value)

    def construct_yaml_float(self, node):
        value = self.construct_scalar(node)
        value = value.replace("_", "").lower()
        sign = 1.0
        if value[0] == "-":
            sign = -1.0
        if value[0] in "+-":
            value = value[1:]
        if value == ".inf":
            return sign * float("inf")
        if value == ".nan":
            return float("nan")
        if ":" in value:
            digits = [float(part) for part in value.split(":")]
            digits.reverse()
            base = 1
            total = 0.0
            for digit in digits:
                total += digit * base
                base *= 60
            return sign * total
        return sign * float(value)

    def construct_yaml_binary(self, node):
        try:
            value = self.construct_scalar(node).encode("ascii")
        except UnicodeEncodeError as exc:
            raise ConstructorError(None, None,
                                   "failed to convert base64 data into ascii: %s" % exc, node.start_mark)
        try:
            return base64.decodebytes(value)
        except Exception as exc:
            raise ConstructorError(None, None, "failed to decode base64 data: %s" % exc,
                                   node.start_mark)

    def construct_yaml_timestamp(self, node):
        value = self.construct_scalar(node)
        match = _RE_TS_PARTS.match(value)
        year = int(match.group("year"))
        month = int(match.group("month"))
        day = int(match.group("day"))
        if not match.group("hour"):
            return datetime.date(year, month, day)
        hour = int(match.group("hour"))
        minute = int(match.group("minute"))
        second = int(match.group("second"))
        fraction = 0
        tzinfo = None
        if match.group("fraction"):
            frac = match.group("fraction")[:6]
            while len(frac) < 6:
                frac += "0"
            fraction = int(frac)
        if match.group("tz_sign"):
            tz_hour = int(match.group("tz_hour"))
            tz_minute = int(match.group("tz_minute") or 0)
            delta = datetime.timedelta(hours=tz_hour, minutes=tz_minute)
            if match.group("tz_sign") == "-":
                delta = -delta
            tzinfo = datetime.timezone(delta)
        elif match.group("tz"):
            tzinfo = datetime.timezone.utc
        return datetime.datetime(year, month, day, hour, minute, second, fraction, tzinfo=tzinfo)

    def construct_yaml_omap(self, node):
        omap = []
        yield omap
        if not isinstance(node, SequenceNode):
            raise ConstructorError("while constructing an ordered map", node.start_mark,
                                   "expected a sequence, but found %s" % node.id, node.start_mark)
        for subnode in node.value:
            if not isinstance(subnode, MappingNode):
                raise ConstructorError("while constructing an ordered map", node.start_mark,
                                       "expected a mapping of length 1, but found %s" % subnode.id,
                                       subnode.start_mark)
            if len(subnode.value) != 1:
                raise ConstructorError("while constructing an ordered map", node.start_mark,
                                       "expected a single mapping item, but found %d items"
                                       % len(subnode.value), subnode.start_mark)
            key_node, value_node = subnode.value[0]
            omap.append((self.construct_object(key_node), self.construct_object(value_node)))

    def construct_yaml_pairs(self, node):
        pairs = []
        yield pairs
        if not isinstance(node, SequenceNode):
            raise ConstructorError("while constructing pairs", node.start_mark,
                                   "expected a sequence, but found %s" % node.id, node.start_mark)
        for subnode in node.value:
            if not isinstance(subnode, MappingNode):
                raise ConstructorError("while constructing pairs", node.start_mark,
                                       "expected a mapping of length 1, but found %s" % subnode.id,
                                       subnode.start_mark)
            if len(subnode.value) != 1:
                raise ConstructorError("while constructing pairs", node.start_mark,
                                       "expected a single mapping item, but found %d items"
                                       % len(subnode.value), subnode.start_mark)
            key_node, value_node = subnode.value[0]
            pairs.append((self.construct_object(key_node), self.construct_object(value_node)))

    def construct_yaml_set(self, node):
        data = set()
        yield data
        value = self.construct_mapping(node)
        data.update(value)

    def construct_yaml_str(self, node):
        return self.construct_scalar(node)

    def construct_yaml_seq(self, node):
        data = []
        yield data
        data.extend(self.construct_sequence(node))

    def construct_yaml_map(self, node):
        data = {}
        yield data
        value = self.construct_mapping(node)
        data.update(value)

    def construct_yaml_tuple(self, node):
        return tuple(self.construct_sequence(node))

    def construct_undefined(self, node):
        raise ConstructorError(None, None,
                               "could not determine a constructor for the tag %r" % node.tag,
                               node.start_mark)

    @classmethod
    def add_constructor(cls, tag, constructor):
        table = dict(cls.yaml_constructors)
        table[tag] = constructor
        cls.yaml_constructors = table

    @classmethod
    def add_multi_constructor(cls, tag_prefix, multi_constructor):
        table = dict(cls.yaml_multi_constructors)
        table[tag_prefix] = multi_constructor
        cls.yaml_multi_constructors = table

    @classmethod
    def add_implicit_resolver(cls, tag, regexp, first):
        table = _copy_resolvers(cls.yaml_implicit_resolvers)
        if first is None:
            first = [None]
        for ch in first:
            table.setdefault(ch, []).append((tag, regexp))
        cls.yaml_implicit_resolvers = table


def _std_constructors():
    c = _LoaderCore
    return {
        "tag:yaml.org,2002:null": c.construct_yaml_null,
        "tag:yaml.org,2002:bool": c.construct_yaml_bool,
        "tag:yaml.org,2002:int": c.construct_yaml_int,
        "tag:yaml.org,2002:float": c.construct_yaml_float,
        "tag:yaml.org,2002:binary": c.construct_yaml_binary,
        "tag:yaml.org,2002:timestamp": c.construct_yaml_timestamp,
        "tag:yaml.org,2002:omap": c.construct_yaml_omap,
        "tag:yaml.org,2002:pairs": c.construct_yaml_pairs,
        "tag:yaml.org,2002:set": c.construct_yaml_set,
        "tag:yaml.org,2002:str": c.construct_yaml_str,
        "tag:yaml.org,2002:seq": c.construct_yaml_seq,
        "tag:yaml.org,2002:map": c.construct_yaml_map,
        None: c.construct_undefined,
    }


class BaseLoader(_LoaderCore):
    pass


class SafeLoader(_LoaderCore):
    pass


class FullLoader(_LoaderCore):
    pass


class UnsafeLoader(_LoaderCore):
    pass


class Loader(_LoaderCore):
    pass


BaseLoader.yaml_constructors = {}
BaseLoader.yaml_multi_constructors = {}
BaseLoader.yaml_implicit_resolvers = {}
for _cls in (SafeLoader, FullLoader, UnsafeLoader, Loader):
    _cls.yaml_constructors = _std_constructors()
    _cls.yaml_multi_constructors = {}
    _cls.yaml_implicit_resolvers = _default_resolvers()
for _cls in (FullLoader, UnsafeLoader, Loader):
    _cls.yaml_constructors["tag:yaml.org,2002:python/tuple"] = _LoaderCore.construct_yaml_tuple

CBaseLoader = BaseLoader
CSafeLoader = SafeLoader
CFullLoader = FullLoader
CUnsafeLoader = UnsafeLoader
CLoader = Loader


def _run_load(stream, loader_cls):
    loader = loader_cls(stream)
    try:
        return loader.get_single_data()
    finally:
        loader.dispose()


def _run_load_all(stream, loader_cls):
    loader = loader_cls(stream)
    try:
        while loader.check_data():
            yield loader.get_data()
    finally:
        loader.dispose()


def load(stream, Loader):
    return _run_load(stream, Loader)


def load_all(stream, Loader):
    return _run_load_all(stream, Loader)


def full_load(stream):
    return _run_load(stream, FullLoader)


def full_load_all(stream):
    return _run_load_all(stream, FullLoader)


def safe_load(stream):
    return _run_load(stream, SafeLoader)


def safe_load_all(stream):
    return _run_load_all(stream, SafeLoader)


def unsafe_load(stream):
    return _run_load(stream, UnsafeLoader)


def unsafe_load_all(stream):
    return _run_load_all(stream, UnsafeLoader)


_LOADER_DEFAULTS = (Loader, FullLoader, UnsafeLoader)


def compose(stream, Loader=Loader):
    loader = Loader(stream)
    try:
        return loader.get_single_node()
    finally:
        loader.dispose()


def compose_all(stream, Loader=Loader):
    loader = Loader(stream)
    try:
        while loader.check_node():
            yield loader.get_node()
    finally:
        loader.dispose()


def add_implicit_resolver(tag, regexp, first=None, Loader=None, Dumper=None):
    if Dumper is None:
        Dumper = _DUMPER_DEFAULT
    if Loader is None:
        for cls in _LOADER_DEFAULTS:
            cls.add_implicit_resolver(tag, regexp, first)
    else:
        Loader.add_implicit_resolver(tag, regexp, first)
    Dumper.add_implicit_resolver(tag, regexp, first)


def add_constructor(tag, constructor, Loader=None):
    if Loader is None:
        for cls in _LOADER_DEFAULTS:
            cls.add_constructor(tag, constructor)
    else:
        Loader.add_constructor(tag, constructor)


def add_multi_constructor(tag_prefix, multi_constructor, Loader=None):
    if Loader is None:
        for cls in _LOADER_DEFAULTS:
            cls.add_multi_constructor(tag_prefix, multi_constructor)
    else:
        Loader.add_multi_constructor(tag_prefix, multi_constructor)


# ---------------------------------------------------------------- dumper

_EMIT_ESC = {
    "\0": "0", "\x07": "a", "\x08": "b", "\x09": "t", "\x0a": "n", "\x0b": "v",
    "\x0c": "f", "\x0d": "r", "\x1b": "e", '"': '"', "\\": "\\", "\x85": "N",
    "\xa0": "_", "\u2028": "L", "\u2029": "P",
}


class _Analysis:
    def __init__(self, scalar, empty, multiline, allow_flow_plain, allow_block_plain,
                 allow_single_quoted, allow_double_quoted, allow_block):
        self.scalar = scalar
        self.empty = empty
        self.multiline = multiline
        self.allow_flow_plain = allow_flow_plain
        self.allow_block_plain = allow_block_plain
        self.allow_single_quoted = allow_single_quoted
        self.allow_double_quoted = allow_double_quoted
        self.allow_block = allow_block


class _Buf:
    def __init__(self):
        self.parts = []

    def write(self, data):
        self.parts.append(data)

    def getvalue(self):
        if self.parts and isinstance(self.parts[0], bytes):
            return b"".join(self.parts)
        return "".join(self.parts)


def _node_children(node):
    if isinstance(node, SequenceNode):
        return list(node.value)
    out = []
    for k, v in node.value:
        out.append(k)
        out.append(v)
    return out


class _DumperCore:
    yaml_representers = {}
    yaml_multi_representers = {}
    yaml_implicit_resolvers = {}

    def __init__(self, stream, default_style=None, default_flow_style=False, canonical=None,
                 indent=None, width=None, allow_unicode=None, line_break=None, encoding=None,
                 explicit_start=None, explicit_end=None, version=None, tags=None, sort_keys=True):
        self.stream = stream
        self.default_style = default_style
        self.default_flow_style = default_flow_style
        self.sort_keys = sort_keys
        self.canonical = bool(canonical)
        self.allow_unicode = bool(allow_unicode)
        self.encoding = encoding
        self.use_explicit_start = explicit_start
        self.use_explicit_end = explicit_end
        self.use_version = version
        self.best_indent = 2
        if indent and 1 < indent < 10:
            self.best_indent = indent
        self.best_width = 80
        if width and width > self.best_indent * 2:
            self.best_width = width
        self.best_line_break = "\n"
        if line_break in ("\r", "\n", "\r\n"):
            self.best_line_break = line_break
        self.tag_prefixes = {"!": "!", "tag:yaml.org,2002:": "!!"}
        if tags:
            for handle in tags:
                self.tag_prefixes[tags[handle]] = handle
        self.represented_objects = {}
        self.object_keeper = []
        self.alias_key = None
        self.anchors = {}
        self.serialized_nodes = {}
        self.last_anchor_id = 0
        self.indents = []
        self.indent = None
        self.flow_level = 0
        self.root_context = False
        self.sequence_context = False
        self.mapping_context = False
        self.simple_key_context = False
        self.line = 0
        self.column = 0
        self.whitespace = True
        self.indention = True
        self.open_ended = False
        self._first_doc = True
        self._opened = False
        self._closed = False

    # ------------------------------------------------------ representer
    def open(self):
        self._opened = True

    def dispose(self):
        pass

    def close(self):
        if self._closed:
            return
        self._closed = True
        if self.open_ended:
            self.write_indicator("...", True)
            self.write_indent()

    def represent(self, data):
        node = self.represent_data(data)
        self.serialize(node)
        self.represented_objects = {}
        self.object_keeper = []
        self.alias_key = None

    def ignore_aliases(self, data):
        if data is None:
            return True
        if isinstance(data, tuple) and data == ():
            return True
        if isinstance(data, (str, bytes, bool, int, float)):
            return True
        return False

    def represent_data(self, data):
        if self.ignore_aliases(data):
            self.alias_key = None
        else:
            self.alias_key = id(data)
        if self.alias_key is not None:
            if self.alias_key in self.represented_objects:
                return self.represented_objects[self.alias_key]
            self.object_keeper.append(data)
        data_type = type(data)
        if data_type in self.yaml_representers:
            return self.yaml_representers[data_type](self, data)
        for base in data_type.__mro__:
            if base in self.yaml_multi_representers:
                return self.yaml_multi_representers[base](self, data)
        if None in self.yaml_multi_representers:
            return self.yaml_multi_representers[None](self, data)
        if None in self.yaml_representers:
            return self.yaml_representers[None](self, data)
        return ScalarNode(None, str(data))

    def represent_scalar(self, tag, value, style=None):
        if style is None:
            style = self.default_style
        node = ScalarNode(tag, value, None, None, style)
        if self.alias_key is not None:
            self.represented_objects[self.alias_key] = node
        return node

    def represent_sequence(self, tag, sequence, flow_style=None):
        value = []
        node = SequenceNode(tag, value, None, None, flow_style)
        if self.alias_key is not None:
            self.represented_objects[self.alias_key] = node
        best_style = True
        for item in sequence:
            node_item = self.represent_data(item)
            if not (isinstance(node_item, ScalarNode) and not node_item.style):
                best_style = False
            value.append(node_item)
        if flow_style is None:
            if self.default_flow_style is not None:
                node.flow_style = self.default_flow_style
            else:
                node.flow_style = best_style
        return node

    def represent_mapping(self, tag, mapping, flow_style=None):
        value = []
        node = MappingNode(tag, value, None, None, flow_style)
        if self.alias_key is not None:
            self.represented_objects[self.alias_key] = node
        best_style = True
        if hasattr(mapping, "items"):
            items = list(mapping.items())
            if self.sort_keys:
                try:
                    items = sorted(items, key=lambda kv: kv[0])
                except TypeError:
                    pass
        else:
            items = list(mapping)
        for item_key, item_value in items:
            node_key = self.represent_data(item_key)
            node_value = self.represent_data(item_value)
            if not (isinstance(node_key, ScalarNode) and not node_key.style):
                best_style = False
            if not (isinstance(node_value, ScalarNode) and not node_value.style):
                best_style = False
            value.append((node_key, node_value))
        if flow_style is None:
            if self.default_flow_style is not None:
                node.flow_style = self.default_flow_style
            else:
                node.flow_style = best_style
        return node

    def represent_undefined(self, data):
        raise RepresenterError("cannot represent an object", data)

    def represent_none(self, data):
        return self.represent_scalar("tag:yaml.org,2002:null", "null")

    def represent_str(self, data):
        return self.represent_scalar("tag:yaml.org,2002:str", data)

    def represent_binary(self, data):
        return self.represent_scalar("tag:yaml.org,2002:binary",
                                     base64.encodebytes(data).decode("ascii"), style="|")

    def represent_bool(self, data):
        return self.represent_scalar("tag:yaml.org,2002:bool", "true" if data else "false")

    def represent_int(self, data):
        return self.represent_scalar("tag:yaml.org,2002:int", str(data))

    def represent_float(self, data):
        if data != data:
            value = ".nan"
        elif data == float("inf"):
            value = ".inf"
        elif data == -float("inf"):
            value = "-.inf"
        else:
            value = repr(data).lower()
            if "." not in value and "e" in value:
                value = value.replace("e", ".0e", 1)
        return self.represent_scalar("tag:yaml.org,2002:float", value)

    def represent_list(self, data):
        return self.represent_sequence("tag:yaml.org,2002:seq", data)

    def represent_tuple(self, data):
        return self.represent_sequence("tag:yaml.org,2002:python/tuple", data)

    def represent_dict(self, data):
        return self.represent_mapping("tag:yaml.org,2002:map", data)

    def represent_set(self, data):
        value = {}
        for key in data:
            value[key] = None
        return self.represent_mapping("tag:yaml.org,2002:set", value)

    def represent_date(self, data):
        return self.represent_scalar("tag:yaml.org,2002:timestamp", data.isoformat())

    def represent_datetime(self, data):
        return self.represent_scalar("tag:yaml.org,2002:timestamp", data.isoformat(" "))

    def represent_yaml_object(self, tag, data, cls, flow_style=None):
        state = dict(data.__dict__)
        return self.represent_mapping(tag, state, flow_style=flow_style)

    @classmethod
    def add_representer(cls, data_type, representer):
        table = dict(cls.yaml_representers)
        table[data_type] = representer
        cls.yaml_representers = table

    @classmethod
    def add_multi_representer(cls, data_type, representer):
        table = dict(cls.yaml_multi_representers)
        table[data_type] = representer
        cls.yaml_multi_representers = table

    @classmethod
    def add_implicit_resolver(cls, tag, regexp, first):
        table = _copy_resolvers(cls.yaml_implicit_resolvers)
        if first is None:
            first = [None]
        for ch in first:
            table.setdefault(ch, []).append((tag, regexp))
        cls.yaml_implicit_resolvers = table

    # ------------------------------------------------------- serializer
    def serialize(self, node):
        self.anchors = {}
        self.serialized_nodes = {}
        self.last_anchor_id = 0
        self._anchor_node(node)
        self._document_start()
        self._emit_node(node, True, False, False, False)
        self.write_indent()
        if self.use_explicit_end:
            self.write_indicator("...", True)
            self.write_indent()
        self.anchors = {}
        self.serialized_nodes = {}
        self.last_anchor_id = 0

    def _anchor_node(self, node):
        key = id(node)
        if key in self.anchors:
            if self.anchors[key] is None:
                self.last_anchor_id += 1
                self.anchors[key] = "id%03d" % self.last_anchor_id
        else:
            self.anchors[key] = None
            if not isinstance(node, ScalarNode):
                for child in _node_children(node):
                    self._anchor_node(child)

    def _document_start(self):
        first = self._first_doc
        self._first_doc = False
        implicit = first and not self.use_explicit_start and not self.use_version
        if self.use_version and self.open_ended:
            self.write_indicator("...", True)
            self.write_indent()
        if self.use_version:
            version = self.use_version
            text = "%d.%d" % (version[0], version[1])
            self._raw_write("%YAML " + text)
            self.write_line_break()
        if not implicit:
            self.write_indent()
            self.write_indicator("---", True)

    def _emit_node(self, node, root, sequence, mapping, simple_key):
        self.root_context = root
        self.sequence_context = sequence
        self.mapping_context = mapping
        self.simple_key_context = simple_key
        key = id(node)
        anchor = self.anchors.get(key)
        if key in self.serialized_nodes:
            self.write_indicator("*" + anchor, True)
            return
        self.serialized_nodes[key] = True
        if isinstance(node, ScalarNode):
            detected = _resolve(self.yaml_implicit_resolvers, ScalarNode, node.value, (True, False))
            default = _resolve(self.yaml_implicit_resolvers, ScalarNode, node.value, (False, True))
            implicit = (node.tag == detected, node.tag == default)
            self.analysis = self._analyze_scalar(node.value)
            self._process_anchor("&", anchor)
            style = self._choose_scalar_style(node, implicit)
            self._process_tag_scalar(node, style, implicit)
            self.increase_indent(flow=True)
            self._process_scalar(node, style)
            self.indent = self.indents.pop()
            return
        if isinstance(node, SequenceNode):
            kind = SequenceNode
        else:
            kind = MappingNode
        implicit = node.tag == _resolve(self.yaml_implicit_resolvers, kind, node.value, True)
        self._process_anchor("&", anchor)
        self._process_tag_collection(node, implicit)
        empty = len(node.value) == 0
        if kind is SequenceNode:
            if self.flow_level or self.canonical or node.flow_style or empty:
                self._flow_sequence(node)
            else:
                self._block_sequence(node)
        else:
            if self.flow_level or self.canonical or node.flow_style or empty:
                self._flow_mapping(node)
            else:
                self._block_mapping(node)

    def _process_anchor(self, indicator, anchor):
        if anchor is None:
            return
        self.write_indicator(indicator + anchor, True)

    def _process_tag_scalar(self, node, style, implicit):
        tag = node.tag
        if (not self.canonical or tag is None) and (
                (style == "" and implicit[0]) or (style != "" and implicit[1])):
            return
        if implicit[0] and tag is None:
            tag = "!"
        if not tag:
            raise EmitterError("tag is not specified")
        self.write_indicator(self._prepare_tag(tag), True)

    def _process_tag_collection(self, node, implicit):
        tag = node.tag
        if (not self.canonical or tag is None) and implicit:
            return
        if not tag:
            raise EmitterError("tag is not specified")
        self.write_indicator(self._prepare_tag(tag), True)

    def _prepare_tag(self, tag):
        if not tag:
            raise EmitterError("tag must not be empty")
        if tag == "!":
            return tag
        handle = None
        suffix = tag
        for prefix in sorted(self.tag_prefixes.keys()):
            if tag.startswith(prefix) and (prefix == "!" or len(prefix) < len(tag)):
                handle = self.tag_prefixes[prefix]
                suffix = tag[len(prefix):]
        chunks = []
        for ch in suffix:
            if ("0" <= ch <= "9" or "A" <= ch <= "Z" or "a" <= ch <= "z"
                    or ch in "-;/?:@&=+$,_.~*'()[]" or (ch == "!" and handle != "!")):
                chunks.append(ch)
            else:
                for b in ch.encode("utf-8"):
                    chunks.append("%%%02X" % b)
        text = "".join(chunks)
        if handle:
            return handle + text
        return "!<%s>" % text

    # -- collections
    def increase_indent(self, flow=False, indentless=False):
        self.indents.append(self.indent)
        if self.indent is None:
            if flow:
                self.indent = self.best_indent
            else:
                self.indent = 0
        elif not indentless:
            self.indent += self.best_indent

    def _block_sequence(self, node):
        indentless = self.mapping_context and not self.indention
        self.increase_indent(flow=False, indentless=indentless)
        for item in node.value:
            self.write_indent()
            self.write_indicator("-", True, indention=True)
            self._emit_node(item, False, True, False, False)
        self.indent = self.indents.pop()

    def _block_mapping(self, node):
        self.increase_indent(flow=False)
        for key_node, value_node in node.value:
            self.write_indent()
            if self._check_simple_key(key_node):
                self._emit_node(key_node, False, False, True, True)
                self.write_indicator(":", False)
                self._emit_node(value_node, False, False, True, False)
            else:
                self.write_indicator("?", True, indention=True)
                self._emit_node(key_node, False, False, True, False)
                self.write_indent()
                self.write_indicator(":", True, indention=True)
                self._emit_node(value_node, False, False, True, False)
        self.indent = self.indents.pop()

    def _flow_sequence(self, node):
        self.write_indicator("[", True, whitespace=True)
        self.increase_indent(flow=True)
        self.flow_level += 1
        first = True
        for item in node.value:
            if not first:
                self.write_indicator(",", False)
            if self.canonical or self.column > self.best_width:
                self.write_indent()
            self._emit_node(item, False, True, False, False)
            first = False
        self.indent = self.indents.pop()
        self.flow_level -= 1
        if self.canonical and not first:
            self.write_indicator(",", False)
            self.write_indent()
        self.write_indicator("]", False)

    def _flow_mapping(self, node):
        self.write_indicator("{", True, whitespace=True)
        self.increase_indent(flow=True)
        self.flow_level += 1
        first = True
        for key_node, value_node in node.value:
            if not first:
                self.write_indicator(",", False)
            if self.canonical or self.column > self.best_width:
                self.write_indent()
            if not self.canonical and self._check_simple_key(key_node):
                self._emit_node(key_node, False, False, True, True)
                self.write_indicator(":", False)
                self._emit_node(value_node, False, False, True, False)
            else:
                self.write_indicator("?", True)
                self._emit_node(key_node, False, False, True, False)
                if self.canonical or self.column > self.best_width:
                    self.write_indent()
                self.write_indicator(":", True)
                self._emit_node(value_node, False, False, True, False)
            first = False
        self.indent = self.indents.pop()
        self.flow_level -= 1
        if self.canonical and not first:
            self.write_indicator(",", False)
            self.write_indent()
        self.write_indicator("}", False)

    def _check_simple_key(self, node):
        key = id(node)
        length = 0
        anchor = self.anchors.get(key)
        if key in self.serialized_nodes:
            if anchor is not None:
                length += len(anchor)
            return length < 128
        if anchor is not None:
            length += len(anchor)
        if node.tag is not None:
            length += len(self._prepare_tag(node.tag))
        if isinstance(node, ScalarNode):
            analysis = self._analyze_scalar(node.value)
            length += len(analysis.scalar)
            return length < 128 and not analysis.empty and not analysis.multiline
        return length < 128 and len(node.value) == 0

    # -- scalars
    def _analyze_scalar(self, scalar):
        if not scalar:
            return _Analysis(scalar, True, False, False, True, True, True, False)
        block_indicators = False
        flow_indicators = False
        line_breaks = False
        special_characters = False
        leading_space = False
        leading_break = False
        trailing_space = False
        trailing_break = False
        break_space = False
        space_break = False
        preceded_by_whitespace = False
        followed_by_whitespace = False
        previous_space = False
        previous_break = False
        if scalar.startswith("---") or scalar.startswith("..."):
            block_indicators = True
            flow_indicators = True
        preceded_by_whitespace = True
        n = len(scalar)
        followed_by_whitespace = n == 1 or scalar[1] in _BLANKZ
        index = 0
        while index < n:
            ch = scalar[index]
            if index == 0:
                if ch in "#,[]{}&*!|>'\"%@`":
                    flow_indicators = True
                    block_indicators = True
                if ch in "?:":
                    flow_indicators = True
                    if followed_by_whitespace:
                        block_indicators = True
                if ch == "-" and followed_by_whitespace:
                    flow_indicators = True
                    block_indicators = True
            else:
                if ch in ",?[]{}":
                    flow_indicators = True
                if ch == ":":
                    flow_indicators = True
                    if followed_by_whitespace:
                        block_indicators = True
                if ch == "#" and preceded_by_whitespace:
                    flow_indicators = True
                    block_indicators = True
            if ch in "\n\x85\u2028\u2029":
                line_breaks = True
            if not (ch == "\n" or "\x20" <= ch <= "\x7e"):
                if ((ch == "\x85" or "\xa0" <= ch <= "\ud7ff" or "\ue000" <= ch <= "\ufffd"
                     or "\U00010000" <= ch < "\U0010ffff") and ch != "\ufeff"):
                    if not self.allow_unicode:
                        special_characters = True
                else:
                    special_characters = True
            if ch == " ":
                if index == 0:
                    leading_space = True
                if index == n - 1:
                    trailing_space = True
                if previous_break:
                    break_space = True
                previous_space = True
                previous_break = False
            elif ch in "\n\x85\u2028\u2029":
                if index == 0:
                    leading_break = True
                if index == n - 1:
                    trailing_break = True
                if previous_space:
                    space_break = True
                previous_space = False
                previous_break = True
            else:
                previous_space = False
                previous_break = False
            index += 1
            preceded_by_whitespace = ch in _BLANKZ
            followed_by_whitespace = index + 1 >= n or scalar[index + 1] in _BLANKZ
        allow_flow_plain = True
        allow_block_plain = True
        allow_single_quoted = True
        allow_double_quoted = True
        allow_block = True
        if leading_space or leading_break or trailing_space or trailing_break:
            allow_flow_plain = False
            allow_block_plain = False
        if trailing_space:
            allow_block = False
        if break_space:
            allow_flow_plain = False
            allow_block_plain = False
            allow_single_quoted = False
        if space_break or special_characters:
            allow_flow_plain = False
            allow_block_plain = False
            allow_single_quoted = False
            allow_block = False
        if line_breaks:
            allow_flow_plain = False
            allow_block_plain = False
        if flow_indicators:
            allow_flow_plain = False
        if block_indicators:
            allow_block_plain = False
        return _Analysis(scalar, False, line_breaks, allow_flow_plain, allow_block_plain,
                         allow_single_quoted, allow_double_quoted, allow_block)

    def _choose_scalar_style(self, node, implicit):
        analysis = self.analysis
        style = node.style
        if style == '"' or self.canonical:
            return '"'
        if not style and implicit[0]:
            if (not (self.simple_key_context and (analysis.empty or analysis.multiline))
                    and ((self.flow_level and analysis.allow_flow_plain)
                         or (not self.flow_level and analysis.allow_block_plain))):
                return ""
        if style and style in "|>":
            if (not self.flow_level and not self.simple_key_context and analysis.allow_block):
                return style
        if not style or style == "'":
            if (analysis.allow_single_quoted
                    and not (self.simple_key_context and analysis.multiline)):
                return "'"
        return '"'

    def _process_scalar(self, node, style):
        analysis = self.analysis
        split = not self.simple_key_context
        if style == '"':
            self._write_double_quoted(analysis.scalar, split)
        elif style == "'":
            self._write_single_quoted(analysis.scalar, split)
        elif style == ">":
            self._write_folded(analysis.scalar)
        elif style == "|":
            self._write_literal(analysis.scalar)
        else:
            self._write_plain(analysis.scalar, split)
        self.analysis = None

    # -- low level output
    def _raw_write(self, data):
        if self.encoding:
            data = data.encode(self.encoding)
        self.stream.write(data)

    def write_indicator(self, indicator, need_whitespace, whitespace=False, indention=False):
        if self.whitespace or not need_whitespace:
            data = indicator
        else:
            data = " " + indicator
        self.whitespace = whitespace
        self.indention = self.indention and indention
        self.column += len(data)
        self.open_ended = False
        self._raw_write(data)

    def write_indent(self):
        indent = self.indent or 0
        if (not self.indention or self.column > indent
                or (self.column == indent and not self.whitespace)):
            self.write_line_break()
        if self.column < indent:
            self.whitespace = True
            data = " " * (indent - self.column)
            self.column = indent
            self._raw_write(data)

    def write_line_break(self, data=None):
        if data is None:
            data = self.best_line_break
        self.whitespace = True
        self.indention = True
        self.line += 1
        self.column = 0
        self._raw_write(data)

    def _put(self, data):
        self.column += len(data)
        self._raw_write(data)

    def _write_single_quoted(self, text, split=True):
        self.write_indicator("'", True)
        spaces = False
        breaks = False
        start = 0
        end = 0
        n = len(text)
        while end <= n:
            ch = None
            if end < n:
                ch = text[end]
            if spaces:
                if ch is None or ch != " ":
                    if (start + 1 == end and self.column > self.best_width and split
                            and start != 0 and end != n):
                        self.write_indent()
                    else:
                        self._put(text[start:end])
                    start = end
            elif breaks:
                if ch is None or ch not in "\n\x85\u2028\u2029":
                    if text[start] == "\n":
                        self.write_line_break()
                    for br in text[start:end]:
                        if br == "\n":
                            self.write_line_break()
                        else:
                            self.write_line_break(br)
                    self.write_indent()
                    start = end
            else:
                if ch is None or ch in " \n\x85\u2028\u2029" or ch == "'":
                    if start < end:
                        self._put(text[start:end])
                        start = end
            if ch == "'":
                self._put("''")
                start = end + 1
            if ch is not None:
                spaces = ch == " "
                breaks = ch in "\n\x85\u2028\u2029"
            end += 1
        self.write_indicator("'", False)

    def _write_double_quoted(self, text, split=True):
        self.write_indicator('"', True)
        start = 0
        end = 0
        n = len(text)
        while end <= n:
            ch = None
            if end < n:
                ch = text[end]
            if (ch is None or ch in "\"\\\x85\u2028\u2029\ufeff"
                    or not ("\x20" <= ch <= "\x7e"
                            or (self.allow_unicode
                                and ("\xa0" <= ch <= "\ud7ff" or "\ue000" <= ch <= "\ufffd")))):
                if start < end:
                    self._put(text[start:end])
                    start = end
                if ch is not None:
                    if ch in _EMIT_ESC:
                        data = "\\" + _EMIT_ESC[ch]
                    elif ch <= "\xff":
                        data = "\\x%02X" % ord(ch)
                    elif ch <= "\uffff":
                        data = "\\u%04X" % ord(ch)
                    else:
                        data = "\\U%08X" % ord(ch)
                    self._put(data)
                    start = end + 1
            if (0 < end < n - 1 and (ch == " " or start >= end)
                    and self.column + (end - start) > self.best_width and split):
                data = text[start:end] + "\\"
                if start < end:
                    start = end
                self._put(data)
                self.write_indent()
                self.whitespace = False
                self.indention = False
                if text[start] == " ":
                    self._put("\\")
            end += 1
        self.write_indicator('"', False)

    def _block_hints(self, text):
        hints = ""
        if text:
            if text[0] in " \n\x85\u2028\u2029":
                hints += str(self.best_indent)
            if text[-1] not in "\n\x85\u2028\u2029":
                hints += "-"
            elif len(text) == 1 or text[-2] in "\n\x85\u2028\u2029":
                hints += "+"
        return hints

    def _write_folded(self, text):
        hints = self._block_hints(text)
        self.write_indicator(">" + hints, True)
        if hints[-1:] == "+":
            self.open_ended = True
        self.write_line_break()
        leading_space = True
        spaces = False
        breaks = True
        start = 0
        end = 0
        n = len(text)
        while end <= n:
            ch = None
            if end < n:
                ch = text[end]
            if breaks:
                if ch is None or ch not in "\n\x85\u2028\u2029":
                    if not leading_space and ch is not None and ch != " " and text[start] == "\n":
                        self.write_line_break()
                    leading_space = ch == " "
                    for br in text[start:end]:
                        if br == "\n":
                            self.write_line_break()
                        else:
                            self.write_line_break(br)
                    if ch is not None:
                        self.write_indent()
                    start = end
            elif spaces:
                if ch != " ":
                    if start + 1 == end and self.column > self.best_width:
                        self.write_indent()
                    else:
                        self._put(text[start:end])
                    start = end
            else:
                if ch is None or ch in " \n\x85\u2028\u2029":
                    self._put(text[start:end])
                    if ch is None:
                        self.write_line_break()
                    start = end
            if ch is not None:
                breaks = ch in "\n\x85\u2028\u2029"
                spaces = ch == " "
            end += 1

    def _write_literal(self, text):
        hints = self._block_hints(text)
        self.write_indicator("|" + hints, True)
        if hints[-1:] == "+":
            self.open_ended = True
        self.write_line_break()
        breaks = True
        start = 0
        end = 0
        n = len(text)
        while end <= n:
            ch = None
            if end < n:
                ch = text[end]
            if breaks:
                if ch is None or ch not in "\n\x85\u2028\u2029":
                    for br in text[start:end]:
                        if br == "\n":
                            self.write_line_break()
                        else:
                            self.write_line_break(br)
                    if ch is not None:
                        self.write_indent()
                    start = end
            else:
                if ch is None or ch in "\n\x85\u2028\u2029":
                    self._put(text[start:end])
                    if ch is None:
                        self.write_line_break()
                    start = end
            if ch is not None:
                breaks = ch in "\n\x85\u2028\u2029"
            end += 1

    def _write_plain(self, text, split=True):
        if self.root_context:
            self.open_ended = True
        if not text:
            return
        if not self.whitespace:
            self._put(" ")
        self.whitespace = False
        self.indention = False
        spaces = False
        breaks = False
        start = 0
        end = 0
        n = len(text)
        while end <= n:
            ch = None
            if end < n:
                ch = text[end]
            if spaces:
                if ch != " ":
                    if start + 1 == end and self.column > self.best_width and split:
                        self.write_indent()
                        self.whitespace = False
                        self.indention = False
                    else:
                        self._put(text[start:end])
                    start = end
            elif breaks:
                if ch is None or ch not in "\n\x85\u2028\u2029":
                    if text[start] == "\n":
                        self.write_line_break()
                    for br in text[start:end]:
                        if br == "\n":
                            self.write_line_break()
                        else:
                            self.write_line_break(br)
                    self.write_indent()
                    self.whitespace = False
                    self.indention = False
                    start = end
            else:
                if ch is None or ch in " \n\x85\u2028\u2029":
                    self._put(text[start:end])
                    start = end
            if ch is not None:
                spaces = ch == " "
                breaks = ch in "\n\x85\u2028\u2029"
            end += 1


def _std_representers(full):
    c = _DumperCore
    table = {
        type(None): c.represent_none,
        str: c.represent_str,
        bytes: c.represent_binary,
        bool: c.represent_bool,
        int: c.represent_int,
        float: c.represent_float,
        list: c.represent_list,
        tuple: c.represent_tuple if full else c.represent_list,
        dict: c.represent_dict,
        set: c.represent_set,
        datetime.date: c.represent_date,
        datetime.datetime: c.represent_datetime,
        None: c.represent_undefined,
    }
    return table


class BaseDumper(_DumperCore):
    pass


class SafeDumper(_DumperCore):
    pass


class Dumper(_DumperCore):
    pass


BaseDumper.yaml_representers = {}
BaseDumper.yaml_multi_representers = {}
BaseDumper.yaml_implicit_resolvers = _default_resolvers()
SafeDumper.yaml_representers = _std_representers(False)
SafeDumper.yaml_multi_representers = {}
SafeDumper.yaml_implicit_resolvers = _default_resolvers()
Dumper.yaml_representers = _std_representers(True)
Dumper.yaml_multi_representers = {}
Dumper.yaml_implicit_resolvers = _default_resolvers()
_DUMPER_DEFAULT = Dumper

CBaseDumper = BaseDumper
CSafeDumper = SafeDumper
CDumper = Dumper


def dump_all(documents, stream=None, Dumper=Dumper, default_style=None, default_flow_style=False,
             canonical=None, indent=None, width=None, allow_unicode=None, line_break=None,
             encoding=None, explicit_start=None, explicit_end=None, version=None, tags=None,
             sort_keys=True):
    getvalue = None
    if stream is None:
        stream = _Buf()
        getvalue = stream.getvalue
    dumper = Dumper(stream, default_style=default_style, default_flow_style=default_flow_style,
                    canonical=canonical, indent=indent, width=width, allow_unicode=allow_unicode,
                    line_break=line_break, encoding=encoding, explicit_start=explicit_start,
                    explicit_end=explicit_end, version=version, tags=tags, sort_keys=sort_keys)
    try:
        dumper.open()
        for data in documents:
            dumper.represent(data)
        dumper.close()
    finally:
        dumper.dispose()
    if getvalue is not None:
        return getvalue()
    return None


def dump(data, stream=None, Dumper=Dumper, **kwds):
    return dump_all([data], stream, Dumper=Dumper, **kwds)


def safe_dump_all(documents, stream=None, **kwds):
    return dump_all(documents, stream, Dumper=SafeDumper, **kwds)


def safe_dump(data, stream=None, **kwds):
    return dump_all([data], stream, Dumper=SafeDumper, **kwds)


def add_representer(data_type, representer, Dumper=Dumper):
    Dumper.add_representer(data_type, representer)


def add_multi_representer(data_type, multi_representer, Dumper=Dumper):
    Dumper.add_multi_representer(data_type, multi_representer)


def _construct_yaml_object(self, node, cls):
    data = cls.__new__(cls)
    yield data
    state = self.construct_mapping(node, deep=True)
    data.__dict__.update(state)


_LoaderCore.construct_yaml_object = _construct_yaml_object


class YAMLObjectMetaclass(type):
    def __init__(cls, name, bases, kwds):
        type.__init__(cls, name, bases, kwds)
        tag = kwds.get("yaml_tag")
        if tag is not None:
            loaders = cls.yaml_loader
            if not isinstance(loaders, list):
                loaders = [loaders]
            for loader in loaders:
                loader.add_constructor(tag, cls.from_yaml)
            cls.yaml_dumper.add_representer(cls, cls.to_yaml)


class YAMLObject(metaclass=YAMLObjectMetaclass):
    yaml_loader = [Loader, FullLoader, UnsafeLoader]
    yaml_dumper = Dumper
    yaml_tag = None
    yaml_flow_style = None

    @classmethod
    def from_yaml(cls, loader, node):
        return loader.construct_yaml_object(node, cls)

    @classmethod
    def to_yaml(cls, dumper, data):
        return dumper.represent_yaml_object(cls.yaml_tag, data, cls, flow_style=cls.yaml_flow_style)


def warnings(settings=None):
    if settings is None:
        return {}


# Submodule-style namespaces, so that yaml.constructor.ConstructorError and
# friends resolve as they do in PyYAML.
error = _NS()
error.YAMLError = YAMLError
error.MarkedYAMLError = MarkedYAMLError
error.Mark = Mark
reader = _NS()
reader.ReaderError = ReaderError
scanner = _NS()
scanner.ScannerError = ScannerError
parser = _NS()
parser.ParserError = ParserError
composer = _NS()
composer.ComposerError = ComposerError
constructor = _NS()
constructor.ConstructorError = ConstructorError
constructor.SafeConstructor = SafeLoader
constructor.FullConstructor = FullLoader
constructor.Constructor = Loader
representer = _NS()
representer.RepresenterError = RepresenterError
representer.SafeRepresenter = SafeDumper
representer.Representer = Dumper
emitter = _NS()
emitter.EmitterError = EmitterError
serializer = _NS()
serializer.SerializerError = SerializerError
nodes = _NS()
nodes.Node = Node
nodes.ScalarNode = ScalarNode
nodes.CollectionNode = CollectionNode
nodes.SequenceNode = SequenceNode
nodes.MappingNode = MappingNode
loader = _NS()
loader.Loader = Loader
loader.SafeLoader = SafeLoader
loader.FullLoader = FullLoader
loader.BaseLoader = BaseLoader
loader.UnsafeLoader = UnsafeLoader
dumper = _NS()
dumper.Dumper = Dumper
dumper.SafeDumper = SafeDumper
dumper.BaseDumper = BaseDumper
