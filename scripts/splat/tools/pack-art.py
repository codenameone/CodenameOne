#!/usr/bin/env python3
"""Packs the game's art into the app's resources.

The models are CC0 packs (see ART.md): Quaternius's Ultimate Modular Men and
Toon Shooter Game Kit and Ultimate Nature, and Kenney's Blaster Kit and Survival Kit. Unzip them
anywhere and run

    tools/pack-art.py <directory holding the unzipped packs> [<poly haven directory>]

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
         'Walk', 'Death', 'HitRecieve', 'Wave', 'Roll', 'Idle_Neutral']
OPERATORS = {
    'op-swat': 'Swat', 'op-dad': 'Beach', 'op-worker': 'Worker', 'op-farmer': 'Farmer',
    'op-suit': 'Suit', 'op-hoodie': 'Casual_Hoodie', 'op-punk': 'Punk', 'op-ranger': 'Adventurer',
}
# Markers are toy-looking blasters: paintball, not firearms.
MARKERS = {'mk-semi': 'blaster-b', 'mk-pump': 'blaster-h', 'mk-ramp': 'blaster-k',
           'mk-scope': 'scope-small', 'mk-hopper': 'clip-large', 'mk-grenade': 'grenade-a'}
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


def pack(root, name, stem, clips=None):
    src = find(root, stem)
    g, bin_ = read(src)
    bin_ = embed_images(g, bin_, os.path.dirname(src))
    if clips is not None:
        bin_ = keep_clips(g, bin_, clips)
    else:
        g['animations'] = []
        bin_ = keep_clips(g, bin_, [])
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


def main():
    if len(sys.argv) not in (2, 3):
        raise SystemExit(__doc__)
    root = sys.argv[1]
    real = sys.argv[2] if len(sys.argv) == 3 else None
    os.makedirs(OUT, exist_ok=True)
    total = 0
    for name, stem in OPERATORS.items():
        total += pack(root, name, stem, CLIPS)
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
