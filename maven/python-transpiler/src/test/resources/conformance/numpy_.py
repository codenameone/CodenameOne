import numpy as np

a = np.array([1, 2, 3])
b = np.array([1.5, 2.0, 3.25])
print(repr(a), repr(b), a.dtype, b.dtype, a.shape)
print(a + b, a * 2, a / 2, a // 2, a ** 2, -a, a % 2)
print(repr(a + 1), repr(a + 1.0), repr(a > 1), a == 2)
m = np.arange(12).reshape(3, 4)
print(repr(m))
print(m)
print(m.T, m.T.shape, m[1], m[:, 1], m[1, 2], m[-1, -1], m[::2, ::-1])
print(m.sum(), m.sum(axis=0), m.sum(axis=1), m.mean(), m.mean(axis=0), m.max(), m.min(axis=1))
print(m.argmax(), m.argmin(axis=0), m.std(), m.var(axis=1), m.cumsum(), m.prod(axis=0))
print(m[m > 5], m[[0, 2]], m[:, [1, 3]])
z = np.zeros((2, 3))
z[0] = 5
z[1, 1:] = [7, 8]
z[z == 0] = -1
print(repr(z))
x = np.linspace(0, 1, 5)
print(repr(x), x.sum(), np.sqrt(x), np.exp(x).round(3))
print(repr(np.eye(3)), np.dot(m, np.ones(4)), m @ np.arange(4))
A = np.array([[2.0, 1.0], [1.0, 3.0]])
print(np.linalg.det(A), np.linalg.inv(A), np.linalg.solve(A, np.array([3.0, 5.0])), np.linalg.norm(A))
print(np.where(a > 1, a, 0), np.concatenate([a, a]), np.stack([a, a]), np.vstack([a, a]).shape, np.hstack([a, a]))
print(np.sort(np.array([3, 1, 2])), np.argsort(np.array([3, 1, 2])), np.unique(np.array([3, 1, 3, 2, 1])))
print(repr(np.array([1e-5, 1.0, 1e5])), repr(np.array([0.1, 0.25, 100.0])), repr(np.array([True, False])))
print(repr(np.arange(30, dtype=float) / 7))
print(np.arange(2000))
print(repr(np.arange(24).reshape(2, 3, 4)))
print(np.allclose(A @ np.linalg.inv(A), np.eye(2)), np.isclose(0.1 + 0.2, 0.3), np.array_equal(a, [1, 2, 3]))
print(repr(m.sum()), repr(b.mean()), repr(np.float64(2.5)), type(m.sum()).__name__, float(b[1]))
s = np.array([[1, 2], [3, 4]])
s += 1
s *= 2
print(s, s.tolist(), s.flatten(), s.reshape(-1, 1).shape, np.diff(np.array([1, 4, 9, 16])))
print(np.broadcast_to(np.array([1, 2]), (3, 2)), np.outer([1, 2], [3, 4]), np.clip(np.arange(6), 1, 4))
print(np.median(np.array([3, 1, 4, 1, 5])), np.percentile(np.arange(10), 25), np.histogram([1, 2, 2, 3], bins=2))
try:
    np.array([1, 2]) + np.array([1, 2, 3])
except ValueError as e:
    print("ValueError", e)
try:
    m.reshape(5, 3)
except ValueError as e:
    print("ValueError", e)
try:
    m[5]
except IndexError as e:
    print("IndexError", e)
