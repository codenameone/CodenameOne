#!/usr/bin/env python3
"""Packs the game's art into the app's resources.

The models are CC0 packs (see ART.md): Quaternius's Ultimate Modular Men and
Toon Shooter Game Kit and Ultimate Nature, and Kenney's Blaster Kit and Survival Kit. Unzip them
anywhere and run

    tools/pack-art.py <directory holding the unzipped packs> [<poly haven directory>]
    tools/pack-art.py --smooth   (re-shade the operators already packed)
    tools/pack-art.py --faces    (give the operators already packed a face; --redo paints them again)

The optional second directory holds the Poly Haven (CC0) downloads, one
folder per model under models/ and per surface under textures/; with it the
props in REALISTIC replace their low-poly namesakes and the SURFACES are
written as tex_<name>.jpg.

Every model is written as a self-contained binary glTF,
common/src/main/resources/model_<name>.glb: external textures are embedded
(the core loader reads only embedded images), base64 buffers are decoded, and a
character keeps only the animation clips the game plays, which is most of its
size. Run it again after changing the tables below.
"""
import base64, json, os, struct, sys

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources')

# Operators share one rig, so every one plays the same clips.
CLIPS = ['Idle_Gun', 'Idle_Gun_Shoot', 'Run', 'Run_Back', 'Run_Left', 'Run_Right', 'Run_Shoot',
         'Walk', 'Death', 'HitRecieve', 'HitRecieve_2', 'Wave', 'Roll', 'Idle_Neutral',
         'Punch_Left', 'Punch_Right', 'Kick_Right']
OPERATORS = {
    'op-swat': 'Swat', 'op-dad': 'Beach', 'op-worker': 'Worker', 'op-farmer': 'Farmer',
    'op-suit': 'Suit', 'op-hoodie': 'Casual_Hoodie', 'op-punk': 'Punk', 'op-ranger': 'Adventurer',
}
# Markers are the Toon Shooter kit's guns in dark metal and wood: tactical
# paintball markers look like carbines and pump guns. Kenney's blasters, used
# before, read as plastic toys. The kit's guns lie along X with the muzzle at
# -X; MARKER_YAW turns them to the -Z every other marker model points along.
MARKERS = {'mk-semi': 'SMG', 'mk-pump': 'Shotgun', 'mk-ramp': 'AK',
           'mk-scope': 'scope-small', 'mk-hopper': 'clip-large', 'mk-grenade': 'grenade-a'}
MARKER_YAW = {'mk-semi': -90, 'mk-pump': -90, 'mk-ramp': -90}
PROPS = {
    'sandbags': 'SackTrench', 'sandbags-small': 'SackTrench_Small', 'barrier': 'Barrier_Large',
    'barrier-single': 'Barrier_Single', 'container': 'Container_Long', 'container-small': 'Container_Small',
    'crate': 'Crate', 'boxes': 'CardboardBoxes_3', 'car': 'Debris_BrokenCar', 'tires': 'Debris_Tires',
    'pallet': 'Pallet', 'fence': 'Fence_Long', 'metal-fence': 'MetalFence', 'tree': 'Tree_1',
    'tree-2': 'Tree_3', 'tank': 'Tank', 'dumpster': 'TrashContainer', 'sofa': 'Sofa', 'cone': 'TrafficCone',
    'barrel': 'ExplodingBarrel', 'watertower': 'WaterTank_Platform', 'shack': 'Structure_2',
    'streetlight': 'StreetLight', 'sign': 'Sign', 'planks': 'WoodPlanks',
    'tent': 'tent-canvas', 'workbench': 'workbench', 'rock': 'rock-a', 'campfire': 'campfire-pit',
}
# Greenery from Ultimate Nature, which ships OBJ + MTL only (flat colours, no textures).
NATURE = {
    'grass': 'Grass', 'grass-2': 'Grass_2', 'grass-short': 'Grass_Short', 'flowers': 'Flowers',
    'bush': 'Bush_1', 'bush-2': 'Bush_2', 'bush-berries': 'BushBerries_1', 'plant': 'Plant_2',
    'oak': 'CommonTree_1', 'oak-2': 'CommonTree_3', 'pine': 'PineTree_2', 'pine-2': 'PineTree_4',
    'birch': 'BirchTree_2', 'stump': 'TreeStump_Moss', 'log': 'WoodLog_Moss', 'rock-moss': 'Rock_Moss_2',
}


def find_obj(root, stem):
    for d, _, files in os.walk(root):
        if stem + '.obj' in files:
            return os.path.join(d, stem + '.obj')
    raise SystemExit('not found in the packs: ' + stem + '.obj')


def obj_to_gltf(path):
    """An OBJ and its MTL as one glTF mesh, a primitive per material. Blender wrote the
    MTL colours linear; the game's materials take sRGB, so they are encoded here."""
    base = os.path.dirname(path)
    colors, order = {}, []
    v, vn, prims, cur = [], [], {}, None
    for line in open(path):
        t = line.split()
        if not t:
            continue
        if t[0] == 'mtllib':
            name = None
            for m in open(os.path.join(base, ' '.join(t[1:]))):
                mt = m.split()
                if mt and mt[0] == 'newmtl':
                    name = ' '.join(mt[1:])
                elif mt and mt[0] == 'Kd' and name is not None:
                    colors[name] = [max(0.0, min(1.0, float(c))) ** (1 / 2.2) for c in mt[1:4]]
        elif t[0] == 'v':
            v.append([float(c) for c in t[1:4]])
        elif t[0] == 'vn':
            vn.append([float(c) for c in t[1:4]])
        elif t[0] == 'usemtl':
            cur = ' '.join(t[1:])
            if cur not in prims:
                prims[cur] = []
                order.append(cur)
        elif t[0] == 'f':
            if cur is None:
                cur = ''
                prims[cur] = []
                order.append(cur)
            corners = []
            for c in t[1:]:
                parts = c.split('/')
                corners.append((int(parts[0]) - 1, int(parts[2]) - 1 if len(parts) > 2 and parts[2] else -1))
            for i in range(1, len(corners) - 1):
                prims[cur] += [corners[0], corners[i], corners[i + 1]]
    g = {'asset': {'version': '2.0'}, 'scene': 0, 'scenes': [{'nodes': [0]}],
         'nodes': [{'mesh': 0, 'name': os.path.basename(path)[:-4]}], 'meshes': [{'primitives': []}],
         'materials': [], 'accessors': [], 'bufferViews': []}
    out = bytearray()

    def view(data, target):
        while len(out) % 4:
            out.append(0)
        g['bufferViews'].append({'buffer': 0, 'byteOffset': len(out), 'byteLength': len(data), 'target': target})
        out.extend(data)
        return len(g['bufferViews']) - 1

    for name in order:
        corners = prims[name]
        if not corners:
            continue
        pos, nor, idx, seen = [], [], [], {}
        for c in corners:
            if c not in seen:
                seen[c] = len(pos)
                pos.append(v[c[0]])
                nor.append(vn[c[1]] if c[1] >= 0 else [0.0, 1.0, 0.0])
            idx.append(seen[c])
        pb = b''.join(struct.pack('<3f', *p) for p in pos)
        nb = b''.join(struct.pack('<3f', *n) for n in nor)
        ib = struct.pack('<%dI' % len(idx), *idx)
        mins = [min(p[k] for p in pos) for k in range(3)]
        maxs = [max(p[k] for p in pos) for k in range(3)]
        a = len(g['accessors'])
        g['accessors'] += [
            {'bufferView': view(pb, 34962), 'componentType': 5126, 'count': len(pos), 'type': 'VEC3', 'min': mins, 'max': maxs},
            {'bufferView': view(nb, 34962), 'componentType': 5126, 'count': len(nor), 'type': 'VEC3'},
            {'bufferView': view(ib, 34963), 'componentType': 5125, 'count': len(idx), 'type': 'SCALAR'}]
        g['materials'].append({'name': name, 'pbrMetallicRoughness': {
            'baseColorFactor': colors.get(name, [0.8, 0.8, 0.8]) + [1.0], 'metallicFactor': 0.0, 'roughnessFactor': 0.85}})
        g['meshes'][0]['primitives'].append({'attributes': {'POSITION': a, 'NORMAL': a + 1}, 'indices': a + 2,
                                              'material': len(g['materials']) - 1})
    return g, bytes(out)


def pack_obj(root, name, stem):
    g, bin_ = obj_to_gltf(find_obj(root, stem))
    path = os.path.join(OUT, 'model_' + name + '.glb')
    write_glb(g, bin_, path)
    return os.path.getsize(path)


def find(root, stem):
    """The pack's glTF copy of a model (the packs also hold FBX, OBJ and Blend)."""
    for d, _, files in os.walk(root):
        for f in files:
            base, ext = os.path.splitext(f)
            if base == stem and (ext == '.glb' or (ext == '.gltf' and os.path.basename(d) == 'glTF')):
                return os.path.join(d, f)
    raise SystemExit('not found in the packs: ' + stem)


def read(path):
    data = open(path, 'rb').read()
    if path.endswith('.glb'):
        n = struct.unpack('<I', data[12:16])[0]
        g = json.loads(data[20:20 + n])
        rest = data[20 + n:]
        bin_ = rest[8:8 + struct.unpack('<I', rest[0:4])[0]] if rest else b''
        return g, bin_
    g = json.loads(data)
    uri = g['buffers'][0]['uri']
    bin_ = base64.b64decode(uri.split(',', 1)[1]) if uri.startswith('data:') else \
        open(os.path.join(os.path.dirname(path), uri), 'rb').read()
    return g, bin_


def embed_images(g, bin_, base):
    out = bytearray(bin_)
    for img in g.get('images', []):
        if 'uri' not in img:
            continue
        uri = img.pop('uri')
        raw = base64.b64decode(uri.split(',', 1)[1]) if uri.startswith('data:') else \
            open(os.path.join(base, uri), 'rb').read()
        while len(out) % 4:
            out.append(0)
        g['bufferViews'].append({'buffer': 0, 'byteOffset': len(out), 'byteLength': len(raw)})
        out += raw
        img['bufferView'] = len(g['bufferViews']) - 1
        img['mimeType'] = 'image/jpeg' if raw[:2] == b'\xff\xd8' else 'image/png'
    return bytes(out)


def keep_clips(g, bin_, clips):
    g['animations'] = [a for a in g.get('animations', []) if a.get('name') in clips]
    # Rebuild the buffer from the accessors still referenced, dropping the rest.
    used = set()
    def acc(i):
        used.add(i)
    for m in g['meshes']:
        for p in m['primitives']:
            for v in p['attributes'].values():
                acc(v)
            if 'indices' in p:
                acc(p['indices'])
    for s in g.get('skins', []):
        if 'inverseBindMatrices' in s:
            acc(s['inverseBindMatrices'])
    for a in g['animations']:
        for s in a['samplers']:
            acc(s['input'])
            acc(s['output'])
    views = {}
    for i in used:
        a = g['accessors'][i]
        if 'bufferView' in a:
            views[a['bufferView']] = True
    for img in g.get('images', []):
        if 'bufferView' in img:
            views[img['bufferView']] = True
    out = bytearray()
    remap = {}
    new_views = []
    for v in sorted(views):
        bv = dict(g['bufferViews'][v])
        chunk = bin_[bv.get('byteOffset', 0):bv.get('byteOffset', 0) + bv['byteLength']]
        while len(out) % 4:
            out.append(0)
        bv['byteOffset'] = len(out)
        bv['buffer'] = 0
        out += chunk
        remap[v] = len(new_views)
        new_views.append(bv)
    amap = {}
    new_acc = []
    for i in sorted(used):
        a = dict(g['accessors'][i])
        if 'bufferView' in a:
            a['bufferView'] = remap[a['bufferView']]
        amap[i] = len(new_acc)
        new_acc.append(a)
    for m in g['meshes']:
        for p in m['primitives']:
            p['attributes'] = {k: amap[v] for k, v in p['attributes'].items()}
            if 'indices' in p:
                p['indices'] = amap[p['indices']]
            p.pop('targets', None)
    for s in g.get('skins', []):
        if 'inverseBindMatrices' in s:
            s['inverseBindMatrices'] = amap[s['inverseBindMatrices']]
    for a in g['animations']:
        for s in a['samplers']:
            s['input'] = amap[s['input']]
            s['output'] = amap[s['output']]
    for img in g.get('images', []):
        if 'bufferView' in img:
            img['bufferView'] = remap[img['bufferView']]
    g['bufferViews'] = new_views
    g['accessors'] = new_acc
    return bytes(out)


def write_glb(g, bin_, path):
    g['buffers'] = [{'byteLength': len(bin_)}]
    js = json.dumps(g, separators=(',', ':')).encode()
    js += b' ' * ((4 - len(js) % 4) % 4)
    bin_ += b'\0' * ((4 - len(bin_) % 4) % 4)
    total = 12 + 8 + len(js) + 8 + len(bin_)
    with open(path, 'wb') as f:
        f.write(struct.pack('<III', 0x46546C67, 2, total))
        f.write(struct.pack('<II', len(js), 0x4E4F534A) + js)
        f.write(struct.pack('<II', len(bin_), 0x004E4942) + bin_)


# Quaternius' operators are flat shaded: every corner is stored once per face
# with that face's normal, so each polygon lights as a facet. Averaging the
# normals of the corners that share a position -- across faces that meet at
# less than CREASE degrees, so a boot sole or a cuff stays sharp -- shades them
# as smooth surfaces. Positions and UVs are untouched; only NORMAL changes.
CREASE = 65.0


def smooth_normals(g, bin_):
    import math
    buf = bytearray(bin_)
    lim = math.cos(math.radians(CREASE))

    def where(i):
        a = g['accessors'][i]
        v = g['bufferViews'][a['bufferView']]
        return a, v.get('byteOffset', 0) + a.get('byteOffset', 0), v.get('byteStride', 0)

    for mesh in g.get('meshes', []):
        for prim in mesh['primitives']:
            at = prim['attributes']
            if 'NORMAL' not in at or 'indices' not in prim or prim.get('mode', 4) != 4:
                continue
            pa, po, ps = where(at['POSITION'])
            na, no, ns = where(at['NORMAL'])
            ia, io_, _ = where(prim['indices'])
            if pa['componentType'] != 5126 or na['componentType'] != 5126:
                continue
            ps = ps or 12
            ns = ns or 12
            n = pa['count']
            pos = [struct.unpack_from('<3f', buf, po + k * ps) for k in range(n)]
            fmt = {5121: 'B', 5123: 'H', 5125: 'I'}[ia['componentType']]
            idx = struct.unpack_from('<%d%s' % (ia['count'], fmt), buf, io_)
            faces = []
            touching = [[] for _ in range(n)]
            for f in range(0, len(idx) - 2, 3):
                a, b, c = idx[f], idx[f + 1], idx[f + 2]
                p0, p1, p2 = pos[a], pos[b], pos[c]
                u = [p1[i] - p0[i] for i in range(3)]
                w = [p2[i] - p0[i] for i in range(3)]
                cr = (u[1] * w[2] - u[2] * w[1], u[2] * w[0] - u[0] * w[2], u[0] * w[1] - u[1] * w[0])
                faces.append(cr)
                for v in (a, b, c):
                    touching[v].append(len(faces) - 1)
            groups = {}
            for k in range(n):
                groups.setdefault(tuple(round(x, 4) for x in pos[k]), []).append(k)

            def unit(v):
                l = math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2])
                return (v[0] / l, v[1] / l, v[2] / l) if l > 1e-12 else None

            for members in groups.values():
                around = set()
                for k in members:
                    around.update(touching[k])
                around = [f for f in around if unit(faces[f]) is not None]
                for k in members:
                    own = [unit(faces[f]) for f in touching[k] if unit(faces[f]) is not None]
                    if not own:
                        continue
                    ref = unit([sum(o[i] for o in own) for i in range(3)]) or own[0]
                    acc = [0.0, 0.0, 0.0]
                    for f in around:
                        fu = unit(faces[f])
                        if fu[0] * ref[0] + fu[1] * ref[1] + fu[2] * ref[2] >= lim:
                            for i in range(3):
                                acc[i] += faces[f][i]
                    out = unit(acc) or ref
                    struct.pack_into('<3f', buf, no + k * ns, *out)
    return bytes(buf)


# The operators' faces are two dark dots and two brows: Quaternius models no
# nose or mouth. add_face appends one more primitive to the head mesh -- a
# grid laid over the front of the face, 1.5 mm in front of whatever surface
# (skin, eye, brow) is there, skinned wholly to the head joint -- and paints a
# face on a transparent texture for it. FACES varies the eyes and the mouth.
FACES = {
    'op-swat': dict(iris=(70, 95, 120), mouth='stern', brow=2),
    'op-dad': dict(iris=(90, 60, 30), mouth='smile', moustache=True),
    'op-worker': dict(iris=(60, 110, 70), mouth='smile'),
    'op-farmer': dict(iris=(80, 105, 140), mouth='stern', moustache=True, stubble=True),
    'op-suit': dict(iris=(50, 50, 60), mouth='stern', brow=1),
    'op-hoodie': dict(iris=(100, 70, 40), mouth='smile'),
    'op-punk': dict(iris=(60, 140, 120), mouth='smirk', liner=True),
    'op-ranger': dict(iris=(70, 100, 150), mouth='grin'),
}
FACE_GRID = (21, 17)


def _accessor(g, bin_, data, ctype, typ, count, bounds=None, target=None):
    while len(bin_) % 4:
        bin_.append(0)
    g['bufferViews'].append({'buffer': 0, 'byteOffset': len(bin_), 'byteLength': len(data)})
    bin_ += data
    a = {'bufferView': len(g['bufferViews']) - 1, 'componentType': ctype, 'count': count, 'type': typ}
    if bounds:
        a['min'], a['max'] = bounds
    g['accessors'].append(a)
    return len(g['accessors']) - 1


def _head_triangles(g, bin_, hj):
    """Head-weighted triangles of the Skin, Eye and Eyebrows primitives,
    plus the head primitive's mesh index and the Eye vertices."""
    def arr(ai, fmt, k):
        a = g['accessors'][ai]
        v = g['bufferViews'][a['bufferView']]
        return struct.unpack_from('<%d%s' % (a['count'] * k, fmt), bin_, v.get('byteOffset', 0) + a.get('byteOffset', 0))
    tris, eyes, tops, mesh = [], [], [], None
    for mi, m in enumerate(g['meshes']):
        for pr in m['primitives']:
            mat = g['materials'][pr['material']]['name']
            if mat not in ('Skin', 'Eye', 'Eyebrows'):
                continue
            at = pr['attributes']
            P = arr(at['POSITION'], 'f', 3)
            J = arr(at['JOINTS_0'], 'B', 4)
            W = arr(at['WEIGHTS_0'], 'f', 4)
            on = [any(J[4 * k + j] == hj and W[4 * k + j] > 0.5 for j in range(4)) for k in range(len(P) // 3)]
            if not any(on):
                continue
            mesh = mi
            idx = arr(pr['indices'], {5121: 'B', 5123: 'H', 5125: 'I'}[g['accessors'][pr['indices']]['componentType']], 1)
            for f in range(0, len(idx) - 2, 3):
                t = idx[f:f + 3]
                if all(on[k] for k in t):
                    tris.append([P[3 * k:3 * k + 3] for k in t])
            if mat == 'Eye':
                eyes += [P[3 * k:3 * k + 3] for k in range(len(on)) if on[k]]
    return tris, eyes, mesh


def _front_z(tris, x, y):
    best = None
    for t in tris:
        (ax, ay, az), (bx, by, bz), (cx, cy, cz) = t
        d = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
        if abs(d) < 1e-12:
            continue
        l1 = ((by - cy) * (x - cx) + (cx - bx) * (y - cy)) / d
        l2 = ((cy - ay) * (x - cx) + (ax - cx) * (y - cy)) / d
        l3 = 1 - l1 - l2
        if l1 >= -1e-6 and l2 >= -1e-6 and l3 >= -1e-6:
            z = l1 * az + l2 * bz + l3 * cz
            if best is None or z > best:
                best = z
    return best


def _paint_face(spec, skin, brow, layout, size=256):
    """RGBA texture; layout maps metres on the face plane to pixels."""
    from PIL import Image, ImageDraw
    k = 4
    big = size * k
    im = Image.new('RGBA', (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    X = lambda x: (x - layout['x0']) / (layout['x1'] - layout['x0']) * big
    Y = lambda y: (layout['y1'] - y) / (layout['y1'] - layout['y0']) * big
    m = big / (layout['x1'] - layout['x0'])
    dark = tuple(int(c * 0.45) for c in skin) + (255,)
    hair = tuple(brow) + (255,)
    ex, ey, ew, eh = layout['ex'], layout['ey'], layout['ew'], layout['eh']
    # soft cheeks
    for s in (-1, 1):
        cx, cy = X(s * ex * 1.05), Y(ey - 0.042)
        for r, a in ((0.024, 14), (0.018, 18), (0.012, 22)):
            d.ellipse([cx - r * m, cy - r * m * 0.8, cx + r * m, cy + r * m * 0.8], fill=(205, 90, 80, a))
    # nose: a shaded bridge, a lit tip and two nostrils
    nx, ny = X(0), Y(ey - 0.043)
    d.polygon([(nx - 0.004 * m, Y(ey - 0.01)), (nx + 0.004 * m, Y(ey - 0.01)),
               (nx + 0.011 * m, ny), (nx - 0.011 * m, ny)], fill=dark[:3] + (38,))
    d.ellipse([nx - 0.011 * m, ny - 0.008 * m, nx + 0.011 * m, ny + 0.008 * m], fill=(255, 235, 220, 55))
    for s in (-1, 1):
        d.ellipse([nx + s * 0.0075 * m - 0.0035 * m, ny + 0.002 * m, nx + s * 0.0075 * m + 0.0035 * m, ny + 0.0085 * m],
                  fill=dark[:3] + (170,))
    d.arc([nx - 0.012 * m, ny - 0.006 * m, nx + 0.012 * m, ny + 0.012 * m], 20, 160, fill=dark[:3] + (120,), width=int(0.0012 * m))
    # eyes
    for s in (-1, 1):
        cx, cy = X(s * ex), Y(ey)
        w, h = ew * 1.55 * m, eh * 1.2 * m
        d.ellipse([cx - w, cy - h, cx + w, cy + h], fill=(245, 243, 238, 255), outline=dark, width=int(0.0016 * m))
        ir = h * 0.92
        d.ellipse([cx - ir, cy - ir, cx + ir, cy + ir], fill=spec['iris'] + (255,))
        pr = ir * 0.5
        d.ellipse([cx - pr, cy - pr, cx + pr, cy + pr], fill=(12, 10, 10, 255))
        d.ellipse([cx - ir * 0.55, cy - ir * 0.7, cx - ir * 0.15, cy - ir * 0.3], fill=(255, 255, 255, 235))
        lw = int((0.003 if spec.get('liner') else 0.0018) * m)
        d.arc([cx - w, cy - h * 1.15, cx + w, cy + h * 1.0], 195, 345, fill=(25, 15, 15, 255), width=lw)
        if spec.get('liner'):
            d.arc([cx - w, cy - h * 0.9, cx + w, cy + h * 1.3], 15, 165, fill=(25, 15, 15, 230), width=lw)
    # mouth
    my = Y(ey - 0.078)
    lip = (150, 70, 70, 255)
    st = spec['mouth']
    wd = 0.026 * m
    lw = int(0.0032 * m)
    if st == 'stern':
        d.line([(X(0) - wd, my), (X(0) + wd, my)], fill=dark, width=lw)
        d.line([(X(0) - wd * 0.7, my + 0.004 * m), (X(0) + wd * 0.7, my + 0.004 * m)], fill=(150, 80, 75, 110), width=int(0.004 * m))
    elif st == 'smile':
        d.arc([X(0) - wd, my - 0.02 * m, X(0) + wd, my + 0.012 * m], 15, 165, fill=dark, width=lw)
        d.arc([X(0) - wd * 0.8, my - 0.012 * m, X(0) + wd * 0.8, my + 0.018 * m], 25, 155, fill=(150, 80, 75, 120), width=int(0.004 * m))
    elif st == 'smirk':
        d.line([(X(0) - wd, my + 0.003 * m), (X(0) + wd * 0.1, my + 0.003 * m)], fill=dark, width=lw)
        d.arc([X(0) - wd * 0.2, my - 0.014 * m, X(0) + wd * 1.3, my + 0.012 * m], 0, 95, fill=dark, width=lw)
    elif st == 'grin':
        box = [X(0) - wd, my - 0.014 * m, X(0) + wd, my + 0.014 * m]
        d.chord(box, 0, 180, fill=(250, 248, 244, 255), outline=dark, width=lw)
        d.line([(X(0) - wd * 0.95, my), (X(0) + wd * 0.95, my)], fill=dark, width=int(lw * 0.7))
    if spec.get('moustache'):
        my2 = Y(ey - 0.066)
        for s in (-1, 1):
            d.polygon([(X(0), my2 - 0.004 * m), (X(0) + s * 0.04 * m, my2 + 0.002 * m),
                       (X(0) + s * 0.034 * m, my2 + 0.011 * m), (X(0) + s * 0.012 * m, my2 + 0.009 * m),
                       (X(0), my2 + 0.004 * m)], fill=hair)
    if spec.get('stubble'):
        import random
        rnd = random.Random(7)
        for _ in range(520):
            x = rnd.uniform(-0.058, 0.058)
            y = ey - rnd.uniform(0.055, 0.108)
            if abs(x) < 0.03 and abs(y - (ey - 0.078)) < 0.008:
                continue
            r = rnd.uniform(0.0007, 0.0013) * m
            d.ellipse([X(x) - r, Y(y) - r, X(x) + r, Y(y) + r], fill=brow + (rnd.randint(70, 130),))
    if spec.get('brow'):
        for s in (-1, 1):
            d.line([(X(s * ex * 0.55), Y(ey + 0.026)), (X(s * ex * 1.5), Y(ey + 0.026 - 0.006 * spec['brow']))],
                   fill=hair, width=int(0.003 * m))
    return im.resize((size, size), Image.LANCZOS)


def add_face(g, bin_, name):
    """Returns the new buffer; a model that has a Face material is left alone."""
    import io, math
    if any(mt.get('name') == 'Face' for mt in g['materials']):
        return bin_
    hj = g['skins'][0]['joints'].index(next(i for i, n in enumerate(g['nodes']) if n.get('name') == 'Head'))
    tris, eyes, mesh = _head_triangles(g, bin_, hj)
    if not eyes or mesh is None:
        # the SWAT operator wears a visored helmet: no eyes are modelled, nothing to draw on
        print('no eyes in', name, '- left as it is')
        return bin_
    byname = {mt['name']: mt['pbrMetallicRoughness']['baseColorFactor'] for mt in g['materials']}
    lin = lambda c: int(round(255 * (c ** (1 / 2.2))))
    skin = tuple(lin(c) for c in byname['Skin'][:3])
    brow = tuple(lin(c) for c in byname.get('Eyebrows', [0.05, 0.04, 0.03])[:3])
    ex = sum(abs(p[0]) for p in eyes) / len(eyes)
    ey = sum(p[1] for p in eyes) / len(eyes)
    ew = (max(p[0] for p in eyes if p[0] > 0) - min(p[0] for p in eyes if p[0] > 0)) / 2
    eh = (max(p[1] for p in eyes) - min(p[1] for p in eyes)) / 2
    x1 = 0.078
    y1, y0 = ey + 0.034, ey - 0.112
    cols, rows = FACE_GRID
    zs = [[None] * cols for _ in range(rows)]
    for r in range(rows):
        for c in range(cols):
            x = -x1 + 2 * x1 * c / (cols - 1)
            y = y1 - (y1 - y0) * r / (rows - 1)
            z = _front_z(tris, x, y)
            zs[r][c] = z
    known = [z for row in zs for z in row if z is not None]
    for r in range(rows):
        for c in range(cols):
            if zs[r][c] is None:
                # off the skin (a hair or hat covers it): sink to the nearest edge value
                near = [zs[rr][cc] for rr in range(rows) for cc in range(cols) if zs[rr][cc] is not None
                        and abs(rr - r) + abs(cc - c) <= 3]
                zs[r][c] = min(near) if near else min(known)
    # the grid is smoothed once so the eye bumps do not ripple the nose
    sm = [[sum(zs[min(rows - 1, max(0, r + a))][min(cols - 1, max(0, c + b))]
               for a in (-1, 0, 1) for b in (-1, 0, 1)) / 9 for c in range(cols)] for r in range(rows)]
    for r in range(rows):
        for c in range(cols):
            zs[r][c] = max(sm[r][c], zs[r][c]) + 0.0015
    pos, nor, uv = [], [], []
    for r in range(rows):
        for c in range(cols):
            x = -x1 + 2 * x1 * c / (cols - 1)
            y = y1 - (y1 - y0) * r / (rows - 1)
            dzx = (zs[r][min(cols - 1, c + 1)] - zs[r][max(0, c - 1)]) / (2 * x1 / (cols - 1) * (min(cols - 1, c + 1) - max(0, c - 1)))
            dzy = (zs[max(0, r - 1)][c] - zs[min(rows - 1, r + 1)][c]) / ((y1 - y0) / (rows - 1) * (min(rows - 1, r + 1) - max(0, r - 1)))
            l = math.sqrt(dzx * dzx + dzy * dzy + 1)
            pos.append((x, y, zs[r][c]))
            nor.append((-dzx / l, -dzy / l, 1 / l))
            uv.append((c / (cols - 1), r / (rows - 1)))
    idx = []
    for r in range(rows - 1):
        for c in range(cols - 1):
            i = r * cols + c
            idx += [i, i + cols, i + 1, i + 1, i + cols, i + cols + 1]
    n = len(pos)
    out = bytearray(bin_)
    lo = [min(p[i] for p in pos) for i in range(3)]
    hi = [max(p[i] for p in pos) for i in range(3)]
    a_pos = _accessor(g, out, struct.pack('<%df' % (3 * n), *[v for p in pos for v in p]), 5126, 'VEC3', n, (lo, hi))
    a_nor = _accessor(g, out, struct.pack('<%df' % (3 * n), *[v for p in nor for v in p]), 5126, 'VEC3', n)
    a_uv = _accessor(g, out, struct.pack('<%df' % (2 * n), *[v for p in uv for v in p]), 5126, 'VEC2', n)
    a_j = _accessor(g, out, bytes([hj, 0, 0, 0]) * n, 5121, 'VEC4', n)
    a_w = _accessor(g, out, struct.pack('<4f', 1, 0, 0, 0) * n, 5126, 'VEC4', n)
    a_i = _accessor(g, out, struct.pack('<%dH' % len(idx), *idx), 5123, 'SCALAR', len(idx))
    tex = _paint_face(FACES[name], skin, brow, dict(x0=-x1, x1=x1, y0=y0, y1=y1, ex=ex, ey=ey, ew=ew, eh=eh))
    png = io.BytesIO()
    tex.save(png, 'PNG', optimize=True)
    while len(out) % 4:
        out.append(0)
    g['bufferViews'].append({'buffer': 0, 'byteOffset': len(out), 'byteLength': len(png.getvalue())})
    out += png.getvalue()
    g.setdefault('images', []).append({'bufferView': len(g['bufferViews']) - 1, 'mimeType': 'image/png'})
    g.setdefault('textures', []).append({'source': len(g['images']) - 1})
    g['materials'].append({'name': 'Face', 'doubleSided': True, 'alphaMode': 'BLEND',
                           'pbrMetallicRoughness': {'baseColorTexture': {'index': len(g['textures']) - 1},
                                                    'baseColorFactor': [1, 1, 1, 1],
                                                    'metallicFactor': 0, 'roughnessFactor': 0.6}})
    g['meshes'][mesh]['primitives'].append({
        'attributes': {'POSITION': a_pos, 'NORMAL': a_nor, 'TEXCOORD_0': a_uv, 'JOINTS_0': a_j, 'WEIGHTS_0': a_w},
        'indices': a_i, 'material': len(g['materials']) - 1})
    return bytes(out)


def strip_face(g, bin_):
    """Undoes add_face, which appended everything it made at the end."""
    mi = next((i for i, mt in enumerate(g['materials']) if mt.get('name') == 'Face'), None)
    if mi is None:
        return bin_
    for m in g['meshes']:
        prim = [p for p in m['primitives'] if p['material'] == mi]
        if prim:
            m['primitives'].remove(prim[0])
            first = g['accessors'][prim[0]['attributes']['POSITION']]['bufferView']
    g['materials'].pop(mi)
    g['textures'].pop()
    g['images'].pop()
    cut = g['bufferViews'][first]['byteOffset']
    del g['bufferViews'][first:]
    del g['accessors'][-6:]
    if not g['images']:
        del g['images'], g['textures']
    return bin_[:cut]


def faces_packed(redo=False):
    """Gives the operators already in the resources their faces; redo paints them again."""
    for name in OPERATORS:
        path = os.path.join(OUT, 'model_' + name + '.glb')
        g, bin_ = read(path)
        if any(mt.get('name') == 'Face' for mt in g['materials']):
            if not redo:
                print('has a face', name)
                continue
            bin_ = strip_face(g, bin_)
        write_glb(g, add_face(g, bin_, name), path)
        print('face', name)


def grip(g, bin_):
    """Where a hand holds a gun: over its lowest point, the bottom of the
    pistol grip, a third of the way up. The kit's origins are wherever the
    modeller left them -- the shotgun's is a metre from its grip -- and the
    game hangs a held marker on the wrist by its origin."""
    pts = []
    for mesh in g['meshes']:
        for prim in mesh['primitives']:
            a = g['accessors'][prim['attributes']['POSITION']]
            bv = g['bufferViews'][a['bufferView']]
            off = bv.get('byteOffset', 0) + a.get('byteOffset', 0)
            stride = bv.get('byteStride', 12)
            for i in range(a['count']):
                pts.append(struct.unpack_from('<3f', bin_, off + i * stride))
    lo = min(p[1] for p in pts)
    hi = max(p[1] for p in pts)
    low = [p for p in pts if p[1] < lo + (hi - lo) * 0.08]
    gx = sum(p[0] for p in low) / len(low)
    gz = sum(p[2] for p in low) / len(low)
    return gx, lo + (hi - lo) * 0.35, gz


def turn(g, bin_, degrees):
    """Parents the scene's roots under one node turned `degrees` about Y,
    moved so the grip is at the origin."""
    import math
    gx, gy, gz = grip(g, bin_)
    r = math.radians(degrees)
    # the grip, turned the same way, then moved back to the origin
    tx = -(gx * math.cos(r) + gz * math.sin(r))
    tz = -(-gx * math.sin(r) + gz * math.cos(r))
    half = r / 2
    scene = g['scenes'][g.get('scene', 0)]
    g['nodes'].append({'name': 'turn', 'rotation': [0, math.sin(half), 0, math.cos(half)],
                       'translation': [tx, -gy, tz], 'children': scene['nodes']})
    scene['nodes'] = [len(g['nodes']) - 1]


def pack(root, name, stem, clips=None, smooth=False):
    src = find(root, stem)
    g, bin_ = read(src)
    bin_ = embed_images(g, bin_, os.path.dirname(src))
    if clips is not None:
        bin_ = keep_clips(g, bin_, clips)
    else:
        g['animations'] = []
        bin_ = keep_clips(g, bin_, [])
    if smooth:
        bin_ = smooth_normals(g, bin_)
    if name in FACES:
        bin_ = add_face(g, bin_, name)
    if name in MARKER_YAW:
        turn(g, bin_, MARKER_YAW[name])
        # the kit paints its guns in light greys that read as plastic in
        # the sun: darkened to gunmetal, wood left as it is
        for mat in g.get('materials', []):
            if 'Grey' in mat.get('name', '') or mat.get('name') == 'Black':
                pbr = mat.setdefault('pbrMetallicRoughness', {})
                c = pbr.get('baseColorFactor', [1, 1, 1, 1])
                pbr['baseColorFactor'] = [c[0] * 0.35, c[1] * 0.36, c[2] * 0.4, c[3]]
                pbr['metallicFactor'] = 0.6
                pbr['roughnessFactor'] = 0.45
    path = os.path.join(OUT, 'model_' + name + '.glb')
    write_glb(g, bin_, path)
    return os.path.getsize(path)


# Poly Haven (CC0) photo-scanned props, replacing the low-poly ones by name.
# Only the base colour is kept -- the engine reads no normal or ARM maps --
# shrunk to 512 px so a prop costs tens of KB, not megabytes.
REALISTIC = {
    'car': 'covered_car', 'barrier': 'concrete_road_barrier_02', 'barrier-single': 'concrete_road_barrier',
    'sofa': 'sofa_02', 'barrel': 'barrel_03', 'streetlight': 'street_lamp_01',
    'boxes': 'cardboard_box_01',
}
# Tiling ground and wall surfaces, base colour only.
SURFACES = {'asphalt': 'asphalt_02', 'concrete': 'concrete_floor_02', 'grass': 'grass_ground',
            'gravel': 'gravel_road', 'iron': 'corrugated_iron'}


def shrink(raw, size=512, quality=82):
    from PIL import Image
    import io
    im = Image.open(io.BytesIO(raw)).convert('RGB')
    im.thumbnail((size, size), Image.LANCZOS)
    out = io.BytesIO()
    im.save(out, 'JPEG', quality=quality, optimize=True)
    return out.getvalue()


def pack_real(real, name, stem):
    src = os.path.join(real, 'models', stem, stem + '_1k.gltf')
    g, bin_ = read(src)
    base = os.path.dirname(src)
    keep = set()
    for m in g.get('materials', []):
        pbr = m.setdefault('pbrMetallicRoughness', {})
        if 'baseColorTexture' in pbr:
            keep.add(g['textures'][pbr['baseColorTexture']['index']]['source'])
        pbr.pop('metallicRoughnessTexture', None)
        for k in ('normalTexture', 'occlusionTexture', 'emissiveTexture'):
            m.pop(k, None)
    for i, img in enumerate(g.get('images', [])):
        path = os.path.join(base, img['uri'])
        raw = open(path, 'rb').read() if i in keep else b''
        if i not in keep:
            # a 1x1 placeholder keeps every texture index valid
            from PIL import Image
            import io
            o = io.BytesIO()
            Image.new('RGB', (1, 1), (128, 128, 255)).save(o, 'JPEG')
            raw = o.getvalue()
        img['uri'] = 'data:image/jpeg;base64,' + base64.b64encode(shrink(raw)).decode()
    bin_ = embed_images(g, bin_, base)
    g['animations'] = []
    bin_ = keep_clips(g, bin_, [])
    path = os.path.join(OUT, 'model_' + name + '.glb')
    write_glb(g, bin_, path)
    return os.path.getsize(path)


def pack_surface(real, name, stem):
    raw = open(os.path.join(real, 'textures', stem, stem + '_diff_1k.jpg'), 'rb').read()
    path = os.path.join(OUT, 'tex_' + name + '.jpg')
    open(path, 'wb').write(shrink(raw))
    return os.path.getsize(path)


def smooth_packed():
    """Re-shades the operators already in the resources; the normals come
    from the faces alone, so this equals packing them again."""
    for name in OPERATORS:
        path = os.path.join(OUT, 'model_' + name + '.glb')
        g, bin_ = read(path)
        write_glb(g, smooth_normals(g, bin_), path)
        print('smoothed', name)


def main():
    if sys.argv[1:] in (['--faces'], ['--faces', '--redo']):
        faces_packed(len(sys.argv) == 3)
        return
    if sys.argv[1:] == ['--smooth']:
        smooth_packed()
        return
    if len(sys.argv) not in (2, 3):
        raise SystemExit(__doc__)
    root = sys.argv[1]
    real = sys.argv[2] if len(sys.argv) == 3 else None
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for name, stem in OPERATORS.items():
        total += pack(root, name, stem, CLIPS, smooth=True)
    for table in (MARKERS, PROPS):
        for name, stem in table.items():
            total += pack(root, name, stem)
    for name, stem in NATURE.items():
        total += pack_obj(root, name, stem)
    if real is not None:
        for name, stem in REALISTIC.items():
            total += pack_real(real, name, stem)
        for name, stem in SURFACES.items():
            total += pack_surface(real, name, stem)
    print('packed %d models, %d KB' % (len(OPERATORS) + len(MARKERS) + len(PROPS) + len(NATURE), total // 1024))


if __name__ == '__main__':
    main()
