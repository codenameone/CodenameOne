import fastlib
from fastlib import _core

print(fastlib.total([1, 2, 3, 4]))
print(fastlib.scaled([1.5, 2.5], 2))
print(fastlib.escape("<a&b>"))
print(_core.VERSION, _core.join_words(["c", "ext"]))
print(fastlib.info({"k": 1}))
try:
    _core.fast_sum("x")
except TypeError as e:
    print("TypeError", e)
