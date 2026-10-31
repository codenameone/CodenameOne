# textwrap.
import textwrap

text = ("The quick brown fox jumps over the lazy dog. A well-known pangram -- used by typographers -- "
        "contains every letter of the alphabet.  Supercalifragilisticexpialidocious is long; "
        "so is pseudo-pseudo-hypoparathyroidism.\tTabs\tand\nnewlines too.")
for w in [10, 25, 40, 79]:
    print(w, textwrap.wrap(text, w))
print(textwrap.fill(text, 30, initial_indent="* ", subsequent_indent="  "))
print(textwrap.wrap(text, 20, break_long_words=False))
print(textwrap.wrap(text, 20, break_on_hyphens=False))
print(textwrap.wrap(text, 30, fix_sentence_endings=True))
print(textwrap.wrap("  leading and trailing   ", 10), textwrap.wrap("  leading   ", 10, drop_whitespace=False))
print(textwrap.wrap(text, 30, max_lines=3), textwrap.wrap(text, 30, max_lines=1, placeholder="..."))
print(textwrap.shorten("Hello  world!", width=12), textwrap.shorten("Hello world!", width=11), textwrap.shorten("Hello world", 50))
print(textwrap.wrap("", 10), textwrap.wrap("abc", 1), textwrap.wrap("a-b-c-d-e-f", 4))
print(repr(textwrap.dedent("""
    def f():
        return 1
      x
""")), repr(textwrap.dedent("  a\n\tb\n")), repr(textwrap.dedent("   \n  a\n  b")))
print(repr(textwrap.indent("a\n\nb\n", "> ")), repr(textwrap.indent("a\n\nb", "+", lambda l: True)))
try:
    textwrap.wrap("x", 0)
except ValueError as e:
    print(e)
