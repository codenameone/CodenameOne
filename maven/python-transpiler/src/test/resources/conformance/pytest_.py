import io
import sys
import contextlib
import pytest


def show(label, value):
    print(label, value)


def main_checks():
    # raises: context manager, excinfo, match
    with pytest.raises(ValueError) as info:
        int("zz")
    show("type", info.type.__name__)
    show("value", str(info.value))
    show("typename", info.typename)
    with pytest.raises(KeyError, match="missing"):
        raise KeyError("missing key")
    r = pytest.raises(ZeroDivisionError, lambda: 1 / 0)
    show("call", r.type.__name__)
    try:
        with pytest.raises(ValueError):
            pass
    except BaseException as e:
        show("no raise", type(e).__name__ + ": " + str(e))
    try:
        with pytest.raises(ValueError, match="abc"):
            raise ValueError("xyz")
    except AssertionError as e:
        show("no match", str(e).splitlines()[0])
    try:
        with pytest.raises(ValueError):
            raise TypeError("other")
    except TypeError:
        show("passes through", "TypeError")

    # approx
    show("approx1", 0.1 + 0.2 == pytest.approx(0.3))
    show("approx2", 1.0 == pytest.approx(1.1))
    show("approx3", 1.0 == pytest.approx(1.1, abs=0.2))
    show("approx4", 100 == pytest.approx(101, rel=0.02))
    show("approx5", [0.1 + 0.2, 1.0] == pytest.approx([0.3, 1.0]))
    show("approx6", {"a": 0.1 + 0.2} == pytest.approx({"a": 0.3}))
    show("approx7", (1, 2) == pytest.approx((1, 3)))
    show("approx8", pytest.approx(3.0) != 3.5)
    show("approx9", float("nan") == pytest.approx(float("nan"), nan_ok=True))
    show("approx10", 1 == pytest.approx(1.0))

    # fail / skip / xfail raise their own exceptions
    for label, fn, args in (("fail", pytest.fail, ("boom",)), ("skip", pytest.skip, ("later",)),
                            ("xfail", pytest.xfail, ("known",))):
        try:
            fn(*args)
        except BaseException as e:
            show(label, type(e).__name__ + ": " + str(e))


@pytest.fixture
def numbers():
    return [1, 2, 3]


@pytest.fixture
def total(numbers):
    return sum(numbers)


EVENTS = []


@pytest.fixture
def resource():
    EVENTS.append("open")
    yield "res"
    EVENTS.append("close")


@pytest.fixture(autouse=True)
def always():
    EVENTS.append("auto")


def test_plain():
    assert 1 + 1 == 2


def test_fixture_chain(numbers, total):
    assert total == 6
    assert numbers == [1, 2, 3]


def test_yield_fixture(resource):
    assert resource == "res"
    assert "open" in EVENTS and "close" not in EVENTS


@pytest.mark.parametrize("a, b, expected", [(1, 2, 3), (2, 3, 5), (10, 5, 15)])
def test_add(a, b, expected):
    assert a + b == expected


@pytest.mark.parametrize("n", [1, 2])
@pytest.mark.parametrize("s", ["x", "y"])
def test_stacked(n, s):
    assert n in (1, 2) and s in ("x", "y")


@pytest.mark.parametrize("v", [pytest.param(1, id="one"), pytest.param(2, id="two"),
                               pytest.param(3, marks=pytest.mark.skip(reason="no"))])
def test_params_ids(v):
    assert v < 3


@pytest.mark.parametrize("x", [1, 2, 3])
def test_some_fail(x):
    assert x != 2, "x was two"


def test_failing():
    assert 1 == 2


def test_error_in_body():
    raise RuntimeError("oops")


@pytest.mark.skip(reason="not now")
def test_skipped():
    assert False


@pytest.mark.skipif(True, reason="cond")
def test_skipif_true():
    assert False


@pytest.mark.skipif(False, reason="cond")
def test_skipif_false():
    assert True


def test_skip_inside():
    pytest.skip("inside")


@pytest.mark.xfail(reason="known bug")
def test_xfail_fails():
    assert 1 == 2


@pytest.mark.xfail
def test_xfail_passes():
    assert True


def test_fail_call():
    pytest.fail("explicit")


def test_raises_and_approx():
    with pytest.raises(ZeroDivisionError):
        1 / 0
    assert 0.1 + 0.2 == pytest.approx(0.3)


def test_monkeypatch(monkeypatch):
    d = {"a": 1}
    monkeypatch.setitem(d, "a", 2)
    assert d["a"] == 2


class TestGroup:
    def setup_method(self, method):
        self.base = 10

    @pytest.fixture
    def extra(self):
        return 5

    def test_method(self):
        assert self.base == 10

    def test_method_fixture(self, extra):
        assert self.base + extra == 15

    @pytest.mark.parametrize("k", [1, 2])
    def test_method_param(self, k):
        assert k > 0

    def test_method_failing(self):
        assert self.base == 11


class Counter:
    def __init__(self):
        self.counts = {}

    def pytest_runtest_logreport(self, report):
        if report.when == "call" or (report.when == "setup" and report.outcome != "passed"):
            key = report.outcome
            if hasattr(report, "wasxfail"):
                key = "xpassed" if report.outcome == "passed" else "xfailed"
            elif report.when == "setup" and report.outcome == "failed":
                key = "error"
            self.counts[key] = self.counts.get(key, 0) + 1


def summary(counts):
    parts = []
    for key, word in (("failed", "failed"), ("passed", "passed"), ("skipped", "skipped"),
                      ("xfailed", "xfailed"), ("xpassed", "xpassed"), ("error", "errors")):
        n = counts.get(key, 0)
        if n:
            parts.append("%d %s" % (n, word))
    return "=== " + ", ".join(parts) + " ==="


if __name__ == "__main__":
    main_checks()
    if hasattr(pytest, "run_module"):
        # Our runner prints per test results and the summary line.
        buf = io.StringIO()
        with contextlib.redirect_stdout(buf):
            code = pytest.run_module(sys.modules["__main__"], quiet=True)
        print(buf.getvalue().strip())
        print("exit", code)
    else:
        counter = Counter()
        with contextlib.redirect_stdout(io.StringIO()):
            code = pytest.main([__file__, "-q", "-p", "no:cacheprovider"], plugins=[counter])
        print(summary(counter.counts))
        print("exit", int(code))
