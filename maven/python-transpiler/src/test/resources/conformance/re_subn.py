import re
print(re.sub(r"\s+", " ", "a  b\tc  "), re.subn("x*", "-", "abc"), re.sub("a", "b", "aaa", count=2))
print(re.sub(r"(\w)(\d)", r"\2\1", "a1 b2 \U0001F600c3 d"), re.subn("z", "y", "abc"), re.sub("", "-", ""))
print(re.sub("(a)|b", r"[\1]", "ab"), re.sub("\U0001F600", "x", "a\U0001F600b"))
