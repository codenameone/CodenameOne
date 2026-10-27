# Java interop: from java.<package> import <Class>. CPython cannot import
# java.*, so java_interop.out is written by hand -- do not regenerate it.
from java.java.util import ArrayList, HashMap, Collections
from java.java.lang import StringBuilder, Integer, Math

class Bag:
    def __init__(self):
        self.items = []
    def add(self, x):
        self.items.append(x * 10)

b = Bag()
b.add(3)
print(b.items)

l = ArrayList()
l.add(5)
l.add("x")
l.add(2.5)
l.add(None)
print(l.size(), len(l), l.get(0), l.get(1), l.get(2), l.get(3))
print(l)
nums = ArrayList()
for v in [5, 3, 9, 1]:
    nums.add(v)
Collections.sort(nums, lambda a, b: b - a)
print([x for x in nums], nums[0], nums[-1], 9 in nums)
sb = StringBuilder()
sb.append("a").append(1).append(True).append(2.5).append("z")
print(sb.toString(), sb.length(), str(sb))
print(Integer.parseInt("42") + 1, Integer.MAX_VALUE, Math.max(3, 7), Math.max(2.5, 1.0))
m = HashMap()
m.put("k", 1)
m.put("j", [1, 2])
print(m.get("k"), m["k"], len(m), "k" in m)
print(isinstance(l, ArrayList), isinstance(sb, ArrayList), isinstance(b, ArrayList))
try:
    Integer.parseInt("nope")
except RuntimeError as e:
    print("caught", "NumberFormatException" in str(e))
try:
    l.get("bad")
except TypeError as e:
    print("type error:", e)
l2 = ArrayList([1, 2, 3])
print(l2.size(), l2.equals(ArrayList([1, 2, 3])), l2 == ArrayList([1, 2, 3]))
