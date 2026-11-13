# Terminal queries with standard streams that are not terminals, as under a
# pipe or CI: CPython's os, shutil, sys, getpass and input() behaviour.
import os
import shutil
import sys

print("isatty", sys.stdin.isatty(), sys.stdout.isatty(), sys.stderr.isatty())
print("fileno", sys.stdin.fileno(), sys.stdout.fileno(), sys.stderr.fileno())
try:
    os.get_terminal_size(1)
    print("size")
except OSError as e:
    print("OSError", e.errno)
size = shutil.get_terminal_size((91, 17))
print(size.columns == int(os.getenv("COLUMNS") or 91), size.lines == int(os.getenv("LINES") or 17))
print(size[0] == size.columns, len(size))
try:
    input("name? ")
except EOFError as e:
    print("EOFError", e)
print(repr(sys.stdin.readline()))
print(repr(sys.stdin.read()), sys.stdin.readlines())
