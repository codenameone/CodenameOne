from typing import Optional, List, Union, ForwardRef, get_args, get_origin

a = get_args(Optional["Author"])
print(a)
print(a[0].__forward_arg__, a[1] is type(None))
print(get_origin(Optional["Author"]) is Union)
print(get_args(List["Book"]))
print(get_args(Union["A", int]))
print(ForwardRef("X") == ForwardRef("X"), ForwardRef("X") == ForwardRef("Y"))
print(len({ForwardRef("X"), ForwardRef("X")}))
print(repr(ForwardRef("Node")))
print(get_args(Optional[int]))


class Mapped:
    def __class_getitem__(cls, item):
        return ("Mapped", item)


m = Mapped[Optional["Author"]]
print(get_args(m[1])[0].__forward_arg__)
