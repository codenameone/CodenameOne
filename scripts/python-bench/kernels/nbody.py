# Planetary orbits: float arithmetic on lists, attribute-free inner loop.
from _harness import measure

PI = 3.14159265358979323
SOLAR_MASS = 4 * PI * PI
DAYS_PER_YEAR = 365.24


def bodies():
    return [
        [[0.0, 0.0, 0.0], [0.0, 0.0, 0.0], SOLAR_MASS],
        [[4.84143144246472090e+00, -1.16032004402742839e+00, -1.03622044471123109e-01],
         [1.66007664274403694e-03 * DAYS_PER_YEAR, 7.69901118419740425e-03 * DAYS_PER_YEAR,
          -6.90460016972063023e-05 * DAYS_PER_YEAR], 9.54791938424326609e-04 * SOLAR_MASS],
        [[8.34336671824457987e+00, 4.12479856412430479e+00, -4.03523417114321381e-01],
         [-2.76742510726862411e-03 * DAYS_PER_YEAR, 4.99852801234917238e-03 * DAYS_PER_YEAR,
          2.30417297573763929e-05 * DAYS_PER_YEAR], 2.85885980666130812e-04 * SOLAR_MASS],
        [[1.28943695621391310e+01, -1.51111514016986312e+01, -2.23307578892655734e-01],
         [2.96460137564761618e-03 * DAYS_PER_YEAR, 2.37847173959480950e-03 * DAYS_PER_YEAR,
          -2.96589568540237556e-05 * DAYS_PER_YEAR], 4.36624404335156298e-05 * SOLAR_MASS],
        [[1.53796971148509165e+01, -2.59193146099879641e+01, 1.79258772950371181e-01],
         [2.68067772490389322e-03 * DAYS_PER_YEAR, 1.62824170038242295e-03 * DAYS_PER_YEAR,
          -9.51592254519715870e-05 * DAYS_PER_YEAR], 5.15138902046611451e-05 * SOLAR_MASS],
    ]


def energy(system):
    e = 0.0
    n = len(system)
    for i in range(n):
        (x1, y1, z1), (vx, vy, vz), m1 = system[i]
        e += 0.5 * m1 * (vx * vx + vy * vy + vz * vz)
        for j in range(i + 1, n):
            (x2, y2, z2), _, m2 = system[j]
            dx = x1 - x2
            dy = y1 - y2
            dz = z1 - z2
            e -= (m1 * m2) / ((dx * dx + dy * dy + dz * dz) ** 0.5)
    return e


def offset_momentum(system):
    px = py = pz = 0.0
    for _, v, m in system:
        px -= v[0] * m
        py -= v[1] * m
        pz -= v[2] * m
    v = system[0][1]
    v[0] = px / SOLAR_MASS
    v[1] = py / SOLAR_MASS
    v[2] = pz / SOLAR_MASS


def advance(system, dt, steps):
    pairs = []
    for i in range(len(system)):
        for j in range(i + 1, len(system)):
            pairs.append((system[i], system[j]))
    for _ in range(steps):
        for (r1, v1, m1), (r2, v2, m2) in pairs:
            dx = r1[0] - r2[0]
            dy = r1[1] - r2[1]
            dz = r1[2] - r2[2]
            mag = dt * ((dx * dx + dy * dy + dz * dz) ** -1.5)
            b1 = m1 * mag
            b2 = m2 * mag
            v1[0] -= dx * b2
            v1[1] -= dy * b2
            v1[2] -= dz * b2
            v2[0] += dx * b1
            v2[1] += dy * b1
            v2[2] += dz * b1
        for r, v, _ in system:
            r[0] += dt * v[0]
            r[1] += dt * v[1]
            r[2] += dt * v[2]


def work(steps):
    system = bodies()
    offset_momentum(system)
    before = energy(system)
    advance(system, 0.01, steps)
    return "%.9f %.9f" % (before, energy(system))


measure("nbody", work, 300000, 5)
