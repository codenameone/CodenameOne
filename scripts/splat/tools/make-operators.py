"""Builds the operators: realistic people with faces, dressed, rigged and
animated, written as common/src/main/resources/model_op-<name>.glb.

Run it in Blender (4.2 or newer) with the MPFB extension and the MakeHuman
CC0 asset packs installed (see ART.md):

    blender -b --python tools/make-operators.py -- <quaternius dir> [name ...]

<quaternius dir> holds Quaternius' Universal Animation Library 1 and 2
(Standard, CC0) unzipped. With no names every operator is built.

Bodies, skins, hair and clothes are MakeHuman's (MPFB, CC0 packs only), on
MPFB's game_engine rig, which names its bones as the animation library does.
The clips are retargeted rotation by rotation: each bone takes the source
bone's turn away from its own rest pose, after the target's rest bone is
swung onto the source's (the library rests in a T-pose, MakeHuman in an
A-pose). Moving clips take their upper body from the pistol aim, so a running
operator holds the marker up. The head, hair, eyes and brows go into one mesh,
`<name>_Head`, which the game hides for your own operator in first person.
Each operator gets modelled paintball goggles, is decimated to a triangle
budget the field can skin twelve times a frame, and has its opaque textures
re-encoded as JPEG.
"""
import os
import sys

import addon_utils
import bpy
import bmesh
import json
import math
import struct
import tempfile
from mathutils import Matrix

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources')
ARGS = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
if not ARGS:
    sys.exit('usage: blender -b --python make-operators.py -- <quaternius dir> [name ...]')
QDIR = ARGS[0]
UAL1 = os.path.join(QDIR, 'Universal Animation Library[Standard]', 'Unreal-Godot', 'UAL1_Standard.glb')
UAL2 = os.path.join(QDIR, 'Universal Animation Library 2[Standard]', 'Unreal-Godot', 'UAL2_Standard.glb')

MALE, FEMALE = 1.0, 0.0


def race(caucasian, african, asian):
    return {'caucasian': caucasian, 'african': african, 'asian': asian}


# name: phenotype, skin, hair, eyebrows, clothes (MakeHuman asset names)
OPERATORS = {
    'dad': dict(ph=dict(gender=MALE, age=0.62, weight=0.7, muscle=0.45, race=race(.9, .05, .05)),
                skin='middleage_caucasian_male', hair='short02', brow='eyebrow001',
                clothes=['male_casualsuit03', 'shoes02']),
    'swat': dict(ph=dict(gender=MALE, age=0.45, weight=0.62, muscle=0.6, height=0.62, race=race(.05, .9, .05)),
                 skin='middleage_african_male', hair='short04', brow='eyebrow003',
                 clothes=['male_casualsuit05', 'shoes03']),
    'worker': dict(ph=dict(gender=MALE, age=0.5, weight=0.6, muscle=0.65, height=0.58, race=race(.2, .1, .7)),
                   skin='middleage_asian_male', hair='short04', brow='eyebrow005',
                   clothes=['male_worksuit01', 'toigo_ankle_boots_male']),
    'ranger': dict(ph=dict(gender=MALE, age=0.7, weight=0.55, muscle=0.5, height=0.52, race=race(.85, .05, .1)),
                   skin='old_caucasian_male', hair='short01', brow='eyebrow009',
                   clothes=['toigo_fisherman_sweater', 'toigo_wool_pants', 'toigo_ankle_boots_male']),
    'hoodie': dict(ph=dict(gender=FEMALE, age=0.4, weight=0.45, muscle=0.55, height=0.48, race=race(.8, .1, .1)),
                   skin='young_caucasian_female', hair='ponytail01', brow='eyebrow007',
                   clothes=['female_sportsuit01', 'shoes06']),
    'suit': dict(ph=dict(gender=FEMALE, age=0.55, weight=0.5, muscle=0.5, height=0.55, race=race(.1, .1, .8)),
                 skin='middleage_asian_female', hair='toigo_inverted_bob', brow='eyebrow006',
                 clothes=['female_elegantsuit01', 'toigo_ankle_boots_female']),
    'farmer': dict(ph=dict(gender=FEMALE, age=0.6, weight=0.62, muscle=0.5, height=0.5, race=race(.9, .05, .05)),
                   skin='middleage_caucasian_female', hair='bob02', brow='eyebrow010',
                   clothes=['female_casualsuit02', 'toigo_ankle_boots_female']),
    'punk': dict(ph=dict(gender=FEMALE, age=0.35, weight=0.48, muscle=0.6, height=0.52, race=race(.05, .9, .05)),
                 skin='young_african_female', hair='afro01', brow='eyebrow006',
                 clothes=['joepal_crude_t-shirt_female', 'toigo_harem_pants', 'shoes05']),
}

# game clip <- (library clip, clip the upper body comes from, or None)
CLIPS = {
    'Idle': ('Idle_Loop', None), 'Aim': ('Pistol_Idle_Loop', None),
    'AimUp': ('Pistol_Aim_Up', None), 'AimDown': ('Pistol_Aim_Down', None),
    'AimFwd': ('Pistol_Aim_Neutral', None), 'Shoot': ('Pistol_Shoot', None),
    'Reload': ('Pistol_Reload', None),
    'Walk': ('Walk_Loop', 'Pistol_Aim_Neutral'), 'Run': ('Jog_Fwd_Loop', 'Pistol_Aim_Neutral'),
    'Sprint': ('Sprint_Loop', None),
    'CrouchIdle': ('Crouch_Idle_Loop', 'Pistol_Aim_Neutral'),
    'CrouchWalk': ('Crouch_Fwd_Loop', 'Pistol_Aim_Neutral'),
    'Death': ('Death01', None), 'HitChest': ('Hit_Chest', None), 'HitHead': ('Hit_Head', None),
    'Knockback': ('Hit_Knockback', None), 'Roll': ('Roll', None), 'Dance': ('Dance_Loop', None),
    'No': ('Idle_No_Loop', None), 'Yes': ('Yes', None), 'Throw': ('OverhandThrow', None),
}
# the library's names for the two bones MPFB spells differently
SOURCE_NAME = {'Root': 'root', 'head': 'Head'}
UPPER = 'spine_02'  # this bone and all under it come from the upper-body clip
TORSO = ('Root', 'pelvis', 'spine', 'neck', 'head')
HEAD_BONES = ('head',)
TEXTURE = 512


def asset(kind, name):
    return '%s/%s.%s' % (name, name, kind)


# Every asset above says CC0 in all of its files. Several community assets
# say CC0 in the .mhclo and AGPL3 in the .obj (MakeClothes' export header);
# those are left out, as are CC-BY ones such as hair02 and hair03.


def build(name, spec):
    from bl_ext.blender_org.mpfb.services.humanservice import HumanService
    ph = {'age': .5, 'cupsize': .5, 'firmness': .5, 'gender': .5, 'height': .5, 'muscle': .5,
          'proportions': .5, 'weight': .5}
    ph.update(spec['ph'])
    info = {'name': name, 'phenotype': ph, 'rig': 'game_engine', 'skin_material_type': 'MAKESKIN',
            'skin_mhmat': asset('mhmat', spec['skin']), 'eyes': 'low-poly/low-poly.mhclo',
            'eyes_material_type': 'MAKESKIN', 'eyebrows': asset('mhclo', spec['brow']),
            'eyelashes': 'eyelashes01/eyelashes01.mhclo', 'hair': asset('mhclo', spec['hair']),
            'proxy': '', 'clothes': [asset('mhclo', c) for c in spec['clothes']], 'targets': [],
            'expressions': [], 'makeup': [], 'teeth': '', 'tongue': '', 'color_adjustments': {},
            'eyes_material_settings': {}, 'skin_material_settings': {}}
    settings = HumanService.get_default_deserialization_settings()
    settings['subdiv_levels'] = 0
    body = HumanService.deserialize_from_dict(info, settings)
    return body.parent, body


def import_library(path):
    before = set(bpy.data.objects)
    actions = set(bpy.data.actions)
    bpy.ops.import_scene.gltf(filepath=path)
    new = set(bpy.data.objects) - before
    arm = [o for o in new if o.type == 'ARMATURE'][0]
    clips = {}
    for a in set(bpy.data.actions) - actions:
        clips[a.name.split('|')[-1]] = a
    return arm, clips, new


def rot(m):
    return m.to_quaternion().to_matrix().to_4x4()


def under(bone, root):
    while bone is not None:
        if bone.name == root:
            return True
        bone = bone.parent
    return False


def sample(arm, action, frame):
    arm.animation_data_create()
    arm.animation_data.action = action
    bpy.context.scene.frame_set(frame)
    return {pb.name: pb.matrix.copy() for pb in arm.pose.bones}


def retarget(tgt, libs):
    src_any = libs[0][0]
    k = tgt.data.bones['head'].head_local.z / src_any.data.bones['Head'].head_local.z
    tgt.animation_data_create()
    for pb in tgt.pose.bones:
        # MPFB rigs default to Euler; matrix_basis would write Euler and the
        # quaternion keys below would hold the rest pose
        pb.rotation_mode = 'QUATERNION'
    bones = list(tgt.data.bones)  # parents before children
    for game, (clip, over) in CLIPS.items():
        src, acts = next((a, c) for a, c, _ in libs if clip in c)
        act = acts[clip]
        f0, f1 = [int(round(v)) for v in act.frame_range]
        osrc, oact = (None, None)
        if over:
            osrc, oacts = next((a, c) for a, c, _ in libs if over in c)
            oact = oacts[over]
            o0, o1 = [int(round(v)) for v in oact.frame_range]
        out = bpy.data.actions.new(game)
        tgt.animation_data.action = out
        for f in range(f0, f1 + 1):
            pose = sample(src, act, f)
            opose = sample(osrc, oact, o0 + (f - f0) % max(1, o1 - o0 + 1)) if oact else None
            placed = {}
            for b in bones:
                sn = SOURCE_NAME.get(b.name, b.name)
                pb = tgt.pose.bones[b.name]
                rest = b.matrix_local
                parent_rest = b.parent.matrix_local if b.parent else Matrix()
                rel = parent_rest.inverted() @ rest
                if sn not in src.data.bones:
                    placed[b.name] = (placed[b.parent.name] if b.parent else Matrix()) @ rel
                    continue
                m = (opose if opose and under(b, UPPER) else pose)[sn]
                srest = src.data.bones[sn].matrix_local
                swing = Matrix()
                if not b.name.startswith(TORSO):
                    swing = rest.to_3x3().col[1].normalized().rotation_difference(
                        srest.to_3x3().col[1].normalized()).to_matrix().to_4x4()
                r = rot(m) @ rot(srest).inverted() @ swing @ rot(rest)
                moves = b.parent is None or b.name == 'pelvis'
                if moves:
                    pos = rest.translation + (m.translation - srest.translation) * k
                else:
                    pos = (placed[b.parent.name] @ rel).translation
                world = Matrix.Translation(pos) @ r
                placed[b.name] = world
                if b.parent:
                    pb.matrix_basis = rel.inverted() @ (placed[b.parent.name].inverted() @ world)
                else:
                    pb.matrix_basis = rest.inverted() @ world
                pb.keyframe_insert('rotation_quaternion', frame=f - f0)
                if moves:
                    pb.keyframe_insert('location', frame=f - f0)
        track = tgt.animation_data.nla_tracks.new()
        track.name = game
        slot = tgt.animation_data.action_slot if hasattr(tgt.animation_data, 'action_slot') else None
        strip = track.strips.new(game, 0, out)
        if slot is not None and hasattr(strip, 'action_slot'):
            # Blender 4.4+ keys an action per slot; a strip without one plays
            # nothing, and the exporter writes the rest pose
            strip.action_slot = slot
        tgt.animation_data.action = None


def apply_masks(obj):
    bpy.context.view_layer.objects.active = obj
    if obj.data.shape_keys:
        # the body's shape is its phenotype keys mixed; fix it before cutting
        bpy.ops.object.shape_key_remove(all=True, apply_mix=True)
    for m in list(obj.modifiers):
        if m.type == 'MASK':
            bpy.ops.object.modifier_apply(modifier=m.name)


def _mat(name, rgb, rough, metal=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    b = m.node_tree.nodes['Principled BSDF']
    b.inputs['Base Color'].default_value = (rgb[0], rgb[1], rgb[2], 1.0)
    b.inputs['Roughness'].default_value = rough
    b.inputs['Metallic'].default_value = metal
    return m


def goggles(name, arm):
    """Models paintball goggles around the eyes, skinned to the head bone.

    No CC0 MakeHuman asset is a paintball mask, and a player without one
    reads as wrong at a glance. A smoked lens wraps the face at eye level
    between two rails and two side frames, and a strap closes the loop behind the head.
    """
    eyes = next(o for o in arm.children if o.type == 'MESH' and 'low-poly' in o.name)
    pts = [eyes.matrix_world @ v.co for v in eyes.data.vertices]
    cx = sum(p.x for p in pts) / len(pts)
    cz = sum(p.z for p in pts) / len(pts)
    front = min(p.y for p in pts)  # MakeHuman faces -Y
    width = max(p.x for p in pts) - min(p.x for p in pts)
    r = max(width * 0.95, 0.08)
    cy = front + r * 0.7
    cz -= 0.006
    seg = 28
    bm = bmesh.new()
    lens_faces, frame_faces, strap_faces = [], [], []

    def band(radius, h, a0, a1, z, faces, thick):
        rings = []
        for i in range(seg + 1):
            a = a0 + (a1 - a0) * i / seg
            ring = []
            for rr, zz in ((radius, z - h / 2), (radius, z + h / 2),
                           (radius + thick, z + h / 2), (radius + thick, z - h / 2)):
                ring.append(bm.verts.new((cx + math.sin(a) * rr, cy - math.cos(a) * rr, zz)))
            rings.append(ring)
        for i in range(seg):
            for k in range(4):
                f = bm.faces.new((rings[i][k], rings[i + 1][k], rings[i + 1][(k + 1) % 4], rings[i][(k + 1) % 4]))
                faces.append(f)

    band(r + 0.004, 0.056, -1.05, 1.05, cz, lens_faces, 0.003)
    for z in (cz - 0.031, cz + 0.031):
        band(r, 0.012, -1.1, 1.1, z, frame_faces, 0.014)
    for a0 in (-1.35, 1.05):
        band(r - 0.004, 0.07, a0, a0 + 0.3, cz, frame_faces, 0.016)
    band(r * 0.98, 0.024, 1.35, 2 * math.pi - 1.35, cz + 0.006, strap_faces, 0.004)
    for f in lens_faces:
        f.material_index = 1
    for f in strap_faces:
        f.material_index = 2
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces[:])
    me = bpy.data.meshes.new(name + '.goggles')
    bm.to_mesh(me)
    bm.free()
    for f in me.polygons:
        f.use_smooth = True
    me.materials.append(_mat(name + '.goggle_frame', (0.03, 0.035, 0.04), 0.45))
    me.materials.append(_mat(name + '.goggle_lens', (0.02, 0.025, 0.03), 0.08, 0.6))
    me.materials.append(_mat(name + '.goggle_strap', (0.06, 0.06, 0.06), 0.9))
    ob = bpy.data.objects.new(name + '.goggles', me)
    bpy.context.scene.collection.objects.link(ob)
    ob.parent = arm
    g = ob.vertex_groups.new(name='head')
    g.add(range(len(me.vertices)), 1.0, 'REPLACE')
    ob.modifiers.new('Armature', 'ARMATURE').object = arm
    return ob


# Triangle budget per mesh. Up to twelve operators are skinned on the CPU
# every frame, so a MakeHuman character at 30k triangles is four times what
# the field can afford; at these counts the face and clothes still read.
BUDGET = {'_Head': 4000, '.body': 4000}
OTHER_BUDGET = 1500


def thin(arm):
    """Decimates every mesh of the operator to its triangle budget."""
    for o in arm.children:
        if o.type != 'MESH':
            continue
        limit = next((v for k, v in BUDGET.items() if o.name.endswith(k)), OTHER_BUDGET)
        bpy.context.view_layer.objects.active = o
        tris = sum(len(f.vertices) - 2 for f in o.data.polygons)
        if tris <= limit:
            continue
        d = o.modifiers.new('thin', 'DECIMATE')
        d.ratio = limit / float(tris)
        d.use_collapse_triangulate = True
        bpy.ops.object.modifier_move_to_index(modifier='thin', index=0)
        bpy.ops.object.modifier_apply(modifier='thin')


def split_head(name, arm, body):
    """Moves the head off the body into `<name>_Head`, with hair, eyes and brows."""
    apply_masks(body)
    groups = [body.vertex_groups[g].index for g in HEAD_BONES if g in body.vertex_groups]
    bpy.ops.object.select_all(action='DESELECT')
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='DESELECT')
    bpy.ops.object.mode_set(mode='OBJECT')
    for v in body.data.vertices:
        w = sum(g.weight for g in v.groups if g.group in groups)
        v.select = w > 0.5
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.separate(type='SELECTED')
    bpy.ops.object.mode_set(mode='OBJECT')
    head = [o for o in bpy.context.selected_objects if o != body][0]
    parts = [o for o in arm.children if o.type == 'MESH'
             and any(t in o.name for t in ('goggles', 'helmet', 'cap', 'glasses', 'low-poly', 'eyebrow', 'eyelashes', OPERATORS[name]['hair']))]
    bpy.ops.object.select_all(action='DESELECT')
    for o in parts + [head]:
        o.select_set(True)
    bpy.context.view_layer.objects.active = head
    bpy.ops.object.join()
    head.name = name + '_Head'
    head.data.name = name + '_Head'


CUTOUT = ('eyelash', 'eyebrow', 'hair', 'bob', 'ponytail', 'afro', 'toigo_inverted')


def alpha_mode(mat):
    # MakeHuman marks every material alpha blended, which exports as BLEND:
    # triangles of one mesh then sort against each other and skin shows
    # through clothes in blocks. Opaque everywhere except the strands, which
    # become a cutout (the exporter writes MASK for a Round before Alpha).
    bsdf = next((n for n in mat.node_tree.nodes if n.type == 'BSDF_PRINCIPLED'), None)
    if bsdf is None:
        return
    sock = bsdf.inputs['Alpha']
    links = list(sock.links)
    if not any(c in mat.name for c in CUTOUT):
        for l in links:
            mat.node_tree.links.remove(l)
        sock.default_value = 1.0
        return
    if links:
        src = links[0].from_socket
        mat.node_tree.links.remove(links[0])
        rnd = mat.node_tree.nodes.new('ShaderNodeMath')
        rnd.operation = 'ROUND'
        mat.node_tree.links.new(src, rnd.inputs[0])
        mat.node_tree.links.new(rnd.outputs[0], sock)


def export(name, arm):
    bpy.ops.object.select_all(action='DESELECT')
    arm.select_set(True)
    for o in arm.children:
        o.select_set(True)
    for mat in bpy.data.materials:
        # MakeHuman normal maps come out as dark blocks per UV island once
        # exported, and at 512 px they add nothing a phone can see
        if mat.node_tree:
            for n in list(mat.node_tree.nodes):
                if n.type == 'NORMAL_MAP':
                    mat.node_tree.nodes.remove(n)
        if mat.node_tree:
            alpha_mode(mat)
    for img in bpy.data.images:
        if img.size[0] > TEXTURE:
            img.scale(TEXTURE, max(1, int(TEXTURE * img.size[1] / img.size[0])))
    path = os.path.join(OUT, 'model_op-%s.glb' % name)
    bpy.ops.export_scene.gltf(filepath=path, use_selection=True, export_format='GLB',
                              export_animations=True, export_animation_mode='NLA_TRACKS',
                              export_apply=True, export_image_format='AUTO', export_yup=True)
    jpeg_opaque(path)
    print('wrote', path, os.path.getsize(path))


def jpeg_opaque(path):
    """Re-encodes the PNG textures of opaque materials in a written GLB as JPEG.

    The exporter writes every MakeHuman texture as PNG, alpha or not; an
    opaque skin or shirt is a quarter of the size as a JPEG. Strands keep
    their PNG, whose alpha is the cutout.
    """
    with open(path, 'rb') as f:
        data = f.read()
    jlen = struct.unpack_from('<I', data, 12)[0]
    gltf = json.loads(data[20:20 + jlen])
    blen = struct.unpack_from('<I', data, 20 + jlen)[0]
    blob = data[28 + jlen:28 + jlen + blen]
    views = [blob[v.get('byteOffset', 0):v.get('byteOffset', 0) + v['byteLength']] for v in gltf['bufferViews']]
    cut = set()
    for m in gltf.get('materials', []):
        if m.get('alphaMode') == 'MASK':
            t = m.get('pbrMetallicRoughness', {}).get('baseColorTexture')
            if t is not None:
                cut.add(gltf['textures'][t['index']]['source'])
    scene = bpy.context.scene
    scene.render.image_settings.file_format = 'JPEG'
    scene.render.image_settings.quality = 85
    tmp = tempfile.mkdtemp()
    for i, im in enumerate(gltf.get('images', [])):
        if i in cut or im.get('mimeType') != 'image/png':
            continue
        src = os.path.join(tmp, '%d.png' % i)
        dst = os.path.join(tmp, '%d.jpg' % i)
        with open(src, 'wb') as f:
            f.write(views[im['bufferView']])
        img = bpy.data.images.load(src)
        img.save_render(dst, scene=scene)
        bpy.data.images.remove(img)
        with open(dst, 'rb') as f:
            jpg = f.read()
        if len(jpg) < len(views[im['bufferView']]):
            views[im['bufferView']] = jpg
            im['mimeType'] = 'image/jpeg'
    out = bytearray()
    for v, data_ in zip(gltf['bufferViews'], views):
        out += b'\0' * (-len(out) % 4)
        v['byteOffset'] = len(out)
        v['byteLength'] = len(data_)
        out += data_
    out += b'\0' * (-len(out) % 4)
    gltf['buffers'][0]['byteLength'] = len(out)
    js = json.dumps(gltf, separators=(',', ':')).encode('utf-8')
    js += b' ' * (-len(js) % 4)
    with open(path, 'wb') as f:
        f.write(struct.pack('<III', 0x46546C67, 2, 28 + len(js) + len(out)))
        f.write(struct.pack('<I', len(js)) + b'JSON' + js)
        f.write(struct.pack('<I', len(out)) + b'BIN\0' + bytes(out))


def main():
    names = ARGS[1:] or list(OPERATORS)
    for name in names:
        bpy.ops.wm.read_factory_settings(use_empty=True)
        addon_utils.enable('bl_ext.blender_org.mpfb')
        arm, body = build(name, OPERATORS[name])
        libs = [import_library(UAL1), import_library(UAL2)]
        retarget(arm, libs)
        for _, _, objs in libs:
            for o in objs:
                bpy.data.objects.remove(o, do_unlink=True)
        goggles(name, arm)
        split_head(name, arm, body)
        thin(arm)
        export(name, arm)


main()
