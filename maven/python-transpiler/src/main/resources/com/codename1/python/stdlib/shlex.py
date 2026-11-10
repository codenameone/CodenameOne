# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath
# exception, as the rest of the Codename One Python runtime.
"""Lexical analysis of shell-style syntax."""

import re
from io import StringIO

__all__ = ["shlex", "split", "quote", "join"]


class shlex:
    def __init__(self, instream=None, infile=None, posix=False, punctuation_chars=False):
        if isinstance(instream, str):
            instream = StringIO(instream)
        if instream is not None:
            self.instream = instream
            self.infile = infile
        else:
            import sys
            self.instream = sys.stdin
            self.infile = None
        self.posix = posix
        self.eof = None if posix else ""
        self.commenters = "#"
        self.wordchars = "abcdfeghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
        if self.posix:
            self.wordchars += ("ßàáâãäåæçèéêë"
                               "ìíîïðñòóôõöøù"
                               "úûüýþÿÀÁÂÃÄÅÆ"
                               "ÇÈÉÊËÌÍÎÏÐÑÒÓ"
                               "ÔÕÖØÙÚÛÜÝÞ")
        self.whitespace = " \t\r\n"
        self.whitespace_split = False
        self.quotes = "'\""
        self.escape = "\\"
        self.escapedquotes = '"'
        self.state = " "
        self.pushback = []
        self.lineno = 1
        self.debug = 0
        self.token = ""
        self.filestack = []
        self.source = None
        if not punctuation_chars:
            punctuation_chars = ""
        elif punctuation_chars is True:
            punctuation_chars = "();<>|&"
        self._punctuation_chars = punctuation_chars
        if punctuation_chars:
            self._pushback_chars = []
            self.wordchars += "~-./*?="
            t = self.wordchars.maketrans(dict.fromkeys(punctuation_chars))
            self.wordchars = self.wordchars.translate(t)

    @property
    def punctuation_chars(self):
        return self._punctuation_chars

    def push_token(self, tok):
        self.pushback.insert(0, tok)

    def get_token(self):
        if self.pushback:
            return self.pushback.pop(0)
        raw = self.read_token()
        while raw == self.eof:
            if not self.filestack:
                return self.eof
            self.instream, self.infile, self.lineno = self.filestack.pop(0)
            raw = self.get_token()
        return raw

    def read_token(self):
        quoted = False
        escapedstate = " "
        while True:
            if self.punctuation_chars and self._pushback_chars:
                nextchar = self._pushback_chars.pop()
            else:
                nextchar = self.instream.read(1)
            if nextchar == "\n":
                self.lineno += 1
            if self.state is None:
                self.token = ""
                break
            elif self.state == " ":
                if not nextchar:
                    self.state = None
                    break
                elif nextchar in self.whitespace:
                    if self.token or (self.posix and quoted):
                        break
                    else:
                        continue
                elif nextchar in self.commenters:
                    self.instream.readline()
                    self.lineno += 1
                elif self.posix and nextchar in self.escape:
                    escapedstate = "a"
                    self.state = nextchar
                elif nextchar in self.wordchars:
                    self.token = nextchar
                    self.state = "a"
                elif nextchar in self.punctuation_chars:
                    self.token = nextchar
                    self.state = "c"
                elif nextchar in self.quotes:
                    if not self.posix:
                        self.token = nextchar
                    self.state = nextchar
                elif self.whitespace_split:
                    self.token = nextchar
                    self.state = "a"
                else:
                    self.token = nextchar
                    if self.token or (self.posix and quoted):
                        break
                    else:
                        continue
            elif self.state in self.quotes:
                quoted = True
                if not nextchar:
                    raise ValueError("No closing quotation")
                if nextchar == self.state:
                    if not self.posix:
                        self.token += nextchar
                        self.state = " "
                        break
                    else:
                        self.state = "a"
                elif (self.posix and nextchar in self.escape and self.state in self.escapedquotes):
                    escapedstate = self.state
                    self.state = nextchar
                else:
                    self.token += nextchar
            elif self.state in self.escape:
                if not nextchar:
                    raise ValueError("No escaped character")
                if (escapedstate in self.quotes and nextchar != self.state and nextchar != escapedstate):
                    self.token += self.state
                self.token += nextchar
                self.state = escapedstate
            elif self.state in ("a", "c"):
                if not nextchar:
                    self.state = None
                    break
                elif nextchar in self.whitespace:
                    self.state = " "
                    if self.token or (self.posix and quoted):
                        break
                    else:
                        continue
                elif nextchar in self.commenters:
                    self.instream.readline()
                    self.lineno += 1
                    if self.posix:
                        self.state = " "
                        if self.token or (self.posix and quoted):
                            break
                        else:
                            continue
                elif self.state == "c":
                    if nextchar in self.punctuation_chars:
                        self.token += nextchar
                    else:
                        if nextchar not in self.whitespace:
                            self._pushback_chars.append(nextchar)
                        self.state = " "
                        break
                elif self.posix and nextchar in self.quotes:
                    self.state = nextchar
                elif self.posix and nextchar in self.escape:
                    escapedstate = "a"
                    self.state = nextchar
                elif (nextchar in self.wordchars or nextchar in self.quotes
                      or (self.whitespace_split and nextchar not in self.punctuation_chars)):
                    self.token += nextchar
                else:
                    if self.punctuation_chars:
                        self._pushback_chars.append(nextchar)
                    else:
                        self.pushback.insert(0, nextchar)
                    self.state = " "
                    if self.token or (self.posix and quoted):
                        break
                    else:
                        continue
        result = self.token
        self.token = ""
        if self.posix and not quoted and result == "":
            result = None
        return result

    def __iter__(self):
        return self

    def __next__(self):
        token = self.get_token()
        if token == self.eof:
            raise StopIteration
        return token


def split(s, comments=False, posix=True):
    if s is None:
        raise ValueError("s argument must not be None")
    lex = shlex(s, posix=posix)
    lex.whitespace_split = True
    if not comments:
        lex.commenters = ""
    return list(lex)


def join(split_command):
    return " ".join(quote(arg) for arg in split_command)


_find_unsafe = re.compile(r"[^\w@%+=:,./-]", re.ASCII).search


def quote(s):
    if not s:
        return "''"
    if not isinstance(s, str):
        raise TypeError("expected string object, got %r" % type(s).__name__)
    if _find_unsafe(s) is None:
        return s
    return "'" + s.replace("'", "'\"'\"'") + "'"
