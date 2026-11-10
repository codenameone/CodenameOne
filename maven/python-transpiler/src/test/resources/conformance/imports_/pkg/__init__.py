print("init pkg", __name__)
VALUE = 21
from .a import twice
def helper(x):
    return twice(x) + 1
