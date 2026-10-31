import sys
print("builtins" in sys.modules)
print(len.__module__, len.__qualname__, len.__name__)
print(print.__module__, isinstance.__qualname__)
import builtins
print(builtins.len is len)
