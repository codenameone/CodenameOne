z = 3 + 4j
w = complex(1, -2)
print(z, w, 1j, -1j, 0j, 2.5j, complex(), complex(2), complex(0, -0.0), complex(-0.0, 0))
print(z + w, z - w, z * w, z / w, z ** 2, z ** -1, z ** 0.5)
print([round(v.real, 9) + round(v.imag, 9) * 1j for v in (2 ** z, w ** w)])
print(abs(z), z.real, z.imag, z.conjugate(), -z, +z, z == complex(3, 4), z != 3, complex(3) == 3, complex(3) == 3.0)
print(1 + z, 2.5 * z, z * 2, 1 / z, z - 1, 10 - z, bool(0j), bool(1j), type(z).__name__, isinstance(z, complex))
print(complex("1+2j"), complex(" (3-4j) "), complex("5j"), complex("-j"), complex("2.5"), complex("1e3-1e-2j"))
print(hash(complex(3, 0)) == hash(3), hash(1j), hash(complex(1, 1)), {z: 1}[complex(3, 4)])
print(repr(complex(float("inf"), float("nan"))), complex(1e300, 1e300) * 1, z == z.conjugate().conjugate())
print([complex(i, i) for i in range(3)], sum([1j, 2, 3.5]), complex(complex(1, 2), complex(3, 4)))
for bad in [lambda: z / 0, lambda: z < w, lambda: complex("1+"), lambda: complex("1", 2), lambda: 0j ** -1,
            lambda: complex([])]:
    try:
        print(bad())
    except (ZeroDivisionError, TypeError, ValueError) as e:
        print(type(e).__name__, e)
