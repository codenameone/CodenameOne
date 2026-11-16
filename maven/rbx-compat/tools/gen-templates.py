#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath exception.
"""Writes the starter templates the studio offers (obby, rainbow obby, tycoon, arena,
simulator, racing) as .rbxlx place files. They are original: layouts and
scripts written for Brickyard. Re-run after editing; the output is checked in.

    maven/rbx-compat/tools/gen-templates.py
"""
import os
from xml.sax.saxutils import escape

MAT = {"Plastic": 256, "SmoothPlastic": 272, "Neon": 288, "Wood": 512, "Concrete": 816, "Brick": 848,
       "Metal": 1088, "Grass": 1280, "LeafyGrass": 1284, "Sand": 1296, "Ice": 1536, "Glass": 1568, "ForceField": 1584,
       "CrackedLava": 804, "Slate": 800, "DiamondPlate": 1056, "Asphalt": 1376, "Water": 2048, "Fabric": 1312, "WoodPlanks": 528, "Basalt": 788}

class Place:
    def __init__(self):
        self.n = 0
        self.out = []

    def ref(self):
        self.n += 1
        return "RBX%d" % self.n

IDENTITY = (1, 0, 0, 0, 1, 0, 0, 0, 1)
# A cylinder's axis is X; this stands it upright (axis along Y).
UPRIGHT = (0, -1, 0, 1, 0, 0, 0, 0, 1)

def yaw(deg):
    import math
    c = math.cos(math.radians(deg))
    s = math.sin(math.radians(deg))
    return (c, 0, s, 0, 1, 0, -s, 0, c)

def cf(x, y, z, r=IDENTITY):
    return ("<X>%g</X><Y>%g</Y><Z>%g</Z>" % (x, y, z)) + "".join(
        "<R%d%d>%.6g</R%d%d>" % (i // 3, i % 3, r[i], i // 3, i % 3) for i in range(9))

def rgb(c):
    return 0xff000000 | c

def part(pl, name, pos, size, color=0xa3a2a5, mat="Plastic", cls="Part", anchored=True, collide=True,
         transparency=0, tags=(), attrs=None, shape=1, extra="", rot=IDENTITY, children=""):
    p = ['<Item class="%s" referent="%s"><Properties>' % (cls, pl.ref()),
         '<string name="Name">%s</string>' % escape(name),
         '<bool name="Anchored">%s</bool>' % ("true" if anchored else "false"),
         '<bool name="CanCollide">%s</bool>' % ("true" if collide else "false"),
         '<CoordinateFrame name="CFrame">%s</CoordinateFrame>' % cf(pos[0], pos[1], pos[2], rot),
         '<Vector3 name="size"><X>%g</X><Y>%g</Y><Z>%g</Z></Vector3>' % size,
         '<Color3uint8 name="Color3uint8">%d</Color3uint8>' % rgb(color),
         '<token name="Material">%d</token>' % MAT[mat]]
    if cls == "Part":
        p.append('<token name="shape">%d</token>' % shape)
    if transparency:
        p.append('<float name="Transparency">%g</float>' % transparency)
    if attrs:
        import base64, struct
        b = struct.pack("<I", len(attrs))
        for k, v in attrs.items():
            b += struct.pack("<I", len(k)) + k.encode() + bytes([0x06]) + struct.pack("<d", v)
        p.append('<BinaryString name="AttributesSerialize">%s</BinaryString>' % base64.b64encode(b).decode())
    if tags:
        import base64
        p.append('<BinaryString name="Tags">%s</BinaryString>'
                 % base64.b64encode("\0".join(tags).encode()).decode())
    p.append(extra)
    p.append('</Properties>' + children + '</Item>')
    return "".join(p)

def c3(name, c):
    return '<Color3 name="%s"><R>%g</R><G>%g</G><B>%g</B></Color3>' % (
        name, ((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0)

def fire(pl, size=4, heat=9, color=0xff8a2a, secondary=0xffd36b):
    return ('<Item class="Fire" referent="%s"><Properties><string name="Name">Fire</string>'
            '<float name="size_xml">%g</float><float name="heat_xml">%g</float>%s%s</Properties></Item>'
            % (pl.ref(), size, heat, c3("Color", color), c3("SecondaryColor", secondary)))

def smoke(pl, color=0x9e9e9e, size=3, rise=2):
    return ('<Item class="Smoke" referent="%s"><Properties><string name="Name">Smoke</string>'
            '<float name="size_xml">%g</float><float name="riseVelocity_xml">%g</float>%s</Properties></Item>'
            % (pl.ref(), size, rise, c3("Color", color)))

def sparkles(pl, color):
    return ('<Item class="Sparkles" referent="%s"><Properties><string name="Name">Sparkles</string>%s'
            '</Properties></Item>' % (pl.ref(), c3("SparkleColor", color)))

def emitter(pl, a, b, rate=20, speed=(4, 7), life=(1, 2), size=0.6, accel=0):
    return ('<Item class="ParticleEmitter" referent="%s"><Properties><string name="Name">Particles</string>'
            '<float name="Rate">%g</float><NumberRange name="Speed">%g %g </NumberRange>'
            '<NumberRange name="Lifetime">%g %g </NumberRange>'
            '<ColorSequence name="Color">0 %g %g %g 0 1 %g %g %g 0 </ColorSequence>'
            '<NumberSequence name="Size">0 %g 0 1 %g 0 </NumberSequence>'
            '<Vector3 name="Acceleration"><X>0</X><Y>%g</Y><Z>0</Z></Vector3></Properties></Item>'
            % (pl.ref(), rate, speed[0], speed[1], life[0], life[1],
               ((a >> 16) & 255) / 255.0, ((a >> 8) & 255) / 255.0, (a & 255) / 255.0,
               ((b >> 16) & 255) / 255.0, ((b >> 8) & 255) / 255.0, (b & 255) / 255.0, size, size, accel))

def torch(pl, ws, x, y, z):
    """A post with a flame on it: a real Fire effect, not a glowing ball."""
    ws.append(part(pl, "TorchPost", (x, y + 2, z), (4, 0.5, 0.5), 0x4e342e, "Wood", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "TorchBowl", (x, y + 4.2, z), (0.6, 1.4, 1.4), 0x37474f, "Metal", shape=2, rot=UPRIGHT,
                   children=fire(pl, 2.2, 8)))

def script(pl, name, src, cls="Script"):
    return ('<Item class="%s" referent="%s"><Properties><string name="Name">%s</string>'
            '<ProtectedString name="Source"><![CDATA[%s]]></ProtectedString></Properties></Item>'
            % (cls, pl.ref(), name, src.strip() + "\n"))

def service(pl, cls, body="", props=""):
    return ('<Item class="%s" referent="%s"><Properties><string name="Name">%s</string>%s</Properties>%s</Item>'
            % (cls, pl.ref(), cls, props, body))

def folder(pl, name, body):
    return ('<Item class="Folder" referent="%s"><Properties><string name="Name">%s</string></Properties>%s</Item>'
            % (pl.ref(), name, body))

def lighting(pl, clock, effects="", brightness=2.5):
    return service(pl, "Lighting", effects, '<float name="ClockTime">%g</float>' % clock
                   + '<float name="Brightness">%g</float>' % brightness)

def color_correction(pl, saturation, contrast):
    return ('<Item class="ColorCorrectionEffect" referent="%s"><Properties><string name="Name">Grade</string>'
            '<float name="Saturation">%g</float><float name="Contrast">%g</float></Properties></Item>'
            % (pl.ref(), saturation, contrast))

def sun_rays(pl, intensity):
    return ('<Item class="SunRaysEffect" referent="%s"><Properties><string name="Name">SunRays</string>'
            '<float name="Intensity">%g</float></Properties></Item>' % (pl.ref(), intensity))

def tree(pl, ws, x, z, ground, h=6, tags=()):
    ws.append(part(pl, "Trunk", (x, ground + h / 2, z), (h, 1.4, 1.4), 0x6d4c33, "Wood", shape=2, rot=UPRIGHT,
                   tags=tags))
    ws.append(part(pl, "Leaves", (x, ground + h + 1.5, z), (6, 6, 6), 0x3d7a35, "LeafyGrass", shape=0, tags=tags))
    ws.append(part(pl, "Leaves", (x + 1.6, ground + h + 0.2, z + 1), (4, 4, 4), 0x4a8a3a, "LeafyGrass", shape=0,
                   tags=tags))
    ws.append(part(pl, "Leaves", (x - 1.4, ground + h + 0.6, z - 1.2), (4.4, 4.4, 4.4), 0x447f37, "LeafyGrass",
                   shape=0, tags=tags))

def bloom(pl, intensity, threshold):
    return ('<Item class="BloomEffect" referent="%s"><Properties><string name="Name">Bloom</string>'
            '<float name="Intensity">%g</float><float name="Threshold">%g</float></Properties></Item>'
            % (pl.ref(), intensity, threshold))

def atmosphere(pl, density, color):
    c = color
    return ('<Item class="Atmosphere" referent="%s"><Properties><string name="Name">Atmosphere</string>'
            '<float name="Density">%g</float><Color3 name="Color"><R>%g</R><G>%g</G><B>%g</B></Color3>'
            '</Properties></Item>' % (pl.ref(), density, ((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0,
                                      (c & 255) / 255.0))

def place(ws, rest):
    return ('<roblox xmlns:xmime="http://www.w3.org/2005/05/xmlmime" version="4">\n'
            '<Item class="Workspace" referent="RBX0"><Properties><string name="Name">Workspace</string>'
            '<float name="Gravity">196.2</float><float name="FallenPartsDestroyHeight">-100</float>'
            '</Properties>\n' + "\n".join(ws) + '\n</Item>\n' + "\n".join(rest) + '\n</roblox>\n')

LEADER = """
local function stats(player, fields)
    local ls = Instance.new("Folder")
    ls.Name = "leaderstats"
    for _, f in ipairs(fields) do
        local v = Instance.new("IntValue")
        v.Name = f
        v.Parent = ls
    end
    ls.Parent = player
    return ls
end
local function playerOf(hit)
    local model = hit.Parent
    if model == nil then return nil end
    return game:GetService("Players"):GetPlayerFromCharacter(model)
end
"""


def lamp(pl, ws, x, z, ground):
    ws.append(part(pl, "LampPost", (x, ground + 4, z), (8, 0.5, 0.5), 0x263238, "Metal", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "Lamp", (x, ground + 8.3, z), (1.2, 1.2, 1.2), 0xfff3c4, "Neon", shape=0))

# Parts tagged Scenery are the backdrop: the studio frames a place without
# them, so a new creator sees the play area and not the horizon.
SCENERY = ("Scenery",)

def hills(pl, ws, radius, base, count=10, color=0x6f8f5a, mat="Grass", trees=True):
    """Rolling hills around the play area, and a belt of trees in front of them.
    Each hill is a flattened sphere sunk into the ground, so only its cap shows."""
    import math
    for i in range(count):
        a = math.radians(i * 360.0 / count + 13)
        h = 22 + (i * 37) % 26
        w = 90 + (i * 53) % 60
        r = radius + (i * 29) % 40
        ws.append(part(pl, "Hill", (math.sin(a) * r, base - h * 0.15, math.cos(a) * r), (w, h, w * 0.8),
                       color, mat, shape=0, rot=yaw(math.degrees(a)), tags=SCENERY))
    if not trees:
        return
    n = 28
    for i in range(n):
        a = math.radians(i * 360.0 / n + (i * 17) % 9)
        r = radius * 0.62 + (i * 23) % 26
        tree(pl, ws, math.sin(a) * r, math.cos(a) * r, base, h=6 + (i * 5) % 5, tags=SCENERY)

def fence(pl, ws, x0, z0, x1, z1, ground, step=8):
    import math
    n = max(1, int(math.hypot(x1 - x0, z1 - z0) / step))
    for i in range(n + 1):
        t = i / float(n)
        ws.append(part(pl, "FencePost", (x0 + (x1 - x0) * t, ground + 1.5, z0 + (z1 - z0) * t), (0.6, 3, 0.6),
                       0x8d6e63, "Wood"))
    length = math.hypot(x1 - x0, z1 - z0)
    ang = math.degrees(math.atan2(x1 - x0, z1 - z0))
    for y in (1.2, 2.4):
        ws.append(part(pl, "FenceRail", ((x0 + x1) / 2, ground + y, (z0 + z1) / 2), (0.3, 0.3, length), 0xa1887f,
                       "Wood", rot=yaw(ang)))

def trim(pl, ws, name, pos, size, color):
    """Thin glowing strips along the top edges of a box platform."""
    x, y, z = pos
    w, h, d = size
    t = y + h / 2 + 0.06
    ws.append(part(pl, name, (x, t, z - d / 2 + 0.15), (w, 0.12, 0.3), color, "Neon", collide=False))
    ws.append(part(pl, name, (x, t, z + d / 2 - 0.15), (w, 0.12, 0.3), color, "Neon", collide=False))
    ws.append(part(pl, name, (x - w / 2 + 0.15, t, z), (0.3, 0.12, d - 0.6), color, "Neon", collide=False))
    ws.append(part(pl, name, (x + w / 2 - 0.15, t, z), (0.3, 0.12, d - 0.6), color, "Neon", collide=False))

def obby_script(name):
    """Kill bricks, checkpoints, the finish, spinners and gliders: the rules
    every obby template shares."""
    return LEADER + """
local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local RunService = game:GetService("RunService")
local TweenService = game:GetService("TweenService")
-- An obby is retried constantly: back on the last checkpoint quickly.
Players.RespawnTime = 1.5
Players.PlayerAdded:Connect(function(p)
    local ls = stats(p, {"Stage", "Wins"})
end)
for _, p in ipairs(Players:GetPlayers()) do stats(p, {"Stage", "Wins"}) end
for _, brick in ipairs(CollectionService:GetTagged("KillBrick")) do
    brick.Touched:Connect(function(hit)
        local h = hit.Parent and hit.Parent:FindFirstChildOfClass("Humanoid")
        if h then h.Health = 0 end
    end)
end
for _, cp in ipairs(CollectionService:GetTagged("Checkpoint")) do
    cp.Touched:Connect(function(hit)
        local p = playerOf(hit)
        if p and p.leaderstats.Stage.Value < cp:GetAttribute("Stage") then
            p.leaderstats.Stage.Value = cp:GetAttribute("Stage")
            p.RespawnLocation = nil
            p:SetAttribute("Checkpoint", cp.Name)
        end
    end)
end
for _, f in ipairs(CollectionService:GetTagged("Finish")) do
    f.Touched:Connect(function(hit)
        local p = playerOf(hit)
        if p and not p:GetAttribute("Finished") then
            p:SetAttribute("Finished", true)
            p.leaderstats.Wins.Value += 1
        end
    end)
end
local spinners = CollectionService:GetTagged("Spin")
RunService.Heartbeat:Connect(function(dt)
    for _, s in ipairs(spinners) do
        s.CFrame = s.CFrame * CFrame.Angles(0, dt * 1.6, 0)
    end
end)
for _, m in ipairs(CollectionService:GetTagged("Glide")) do
    local info = TweenInfo.new(2.5, Enum.EasingStyle.Sine, Enum.EasingDirection.InOut, -1, true)
    TweenService:Create(m, info, {CFrame = m.CFrame + Vector3.new(16, 0, 0)}):Play()
end
print("TEMPLATE READY %s")
""" % name

def obby():
    pl = Place()
    ws = []
    # The world under the course: open sea with rock stacks rising from it.
    # Falling in is the obby's fail state, so the sea is a kill brick too.
    ws.append(part(pl, "Sea", (0, -14, -90), (700, 2, 700), 0x1d6f93, "Water", tags=("KillBrick",)))
    for i, (x, z, h, w) in enumerate([(-40, -20, 30, 12), (38, -60, 44, 16), (-52, -110, 52, 14),
                                       (46, -150, 36, 18), (-30, -190, 60, 20), (70, -10, 24, 10),
                                       (-80, -70, 40, 22), (90, -120, 56, 24), (10, -240, 70, 26)]):
        ws.append(part(pl, "Rock", (x, -14 + h / 2, z), (w, h, w), 0x6b625a, "Slate", rot=yaw(i * 23)))
        ws.append(part(pl, "Rock", (x + w * 0.3, -14 + h * 0.8, z - w * 0.2), (w * 0.6, h * 0.5, w * 0.6),
                       0x7d736a, "Slate", rot=yaw(i * 41 + 10)))
    # The spawn island.
    ws.append(part(pl, "Island", (0, -8, 0), (30, 12, 30), 0x4f4741, "Basalt"))
    ws.append(part(pl, "Grass", (0, -1, 0), (32, 2, 32), 0x5b8f3c, "Grass"))
    ws.append(part(pl, "Start", (0, 0.5, 4), (8, 1, 8), 0x8c8580, "Slate", cls="SpawnLocation"))
    for x, z in ((-10, 8), (10, 10), (-11, -6), (11, -4)):
        tree(pl, ws, x, z, 0)
    for x, z in ((-6, -13), (6, -13)):
        torch(pl, ws, x, 0, z)
    ws.append(part(pl, "Sign", (8.5, 4, -13), (6, 4, 0.6), 0x3a2a1e, "Wood"))
    ws.append(part(pl, "SignStripe", (8.5, 4.6, -13.35), (5, 0.8, 0.1), 0xffc400, "Neon"))
    # Stage 1: stepping pads over the lava.
    pads = [0x2196f3, 0xffc107, 0xe91e63, 0x00bcd4, 0x8bc34a]
    # The first stage teaches the jump, so the drums are wide, in a straight
    # line and rising gently to the deck: run and jump, nothing else. The later stages
    # bring the zigzags and the climbs.
    for i in range(5):
        x = 0
        y = 0.75 + i * 0.75  # the last drum tops out level with the deck
        ws.append(part(pl, "Step%d" % (i + 1), (x, y, -22 - i * 10), (1, 8, 8), 0x3b3532,
                       "Basalt", shape=2, rot=UPRIGHT))
        # A glowing band around the drum's side, just under its top face.
        ws.append(part(pl, "StepRim%d" % (i + 1), (x, y + 0.22, -22 - i * 10), (0.18, 8.25, 8.25), pads[i],
                       "Neon", shape=2, rot=UPRIGHT, collide=False))
    ws.append(part(pl, "Pool1", (0, -2, -44), (24, 1, 44), 0x1d7fa0, "Water", tags=("KillBrick",)))
    # Checkpoint 1.
    cp1z = -76
    ws.append(part(pl, "Stage1", (0, 3, cp1z), (14, 2, 14), 0x4a4440, "Slate"))
    trim(pl, ws, "Stage1Trim", (0, 3, cp1z), (14, 2, 14), 0xffa040)
    for x in (-6, 6):
        torch(pl, ws, x, 4, cp1z + 6)
    ws.append(part(pl, "Checkpoint1", (0, 4.1, cp1z), (6, 0.2, 6), 0x00e676, "Neon", collide=False,
                   tags=("Checkpoint",), attrs={"Stage": 1}))
    # Stage 2: a ramp, then a narrow beam.
    ws.append(part(pl, "Ramp", (0, 6, cp1z - 12), (6, 4, 10), 0x5a524c, "Slate", cls="WedgePart",
                   rot=yaw(180)))
    ws.append(part(pl, "Beam", (0, 7.5, cp1z - 30), (2, 1, 26), 0x8a6a4a, "WoodPlanks"))
    ws.append(part(pl, "Pool2", (0, 3, cp1z - 30), (16, 1, 26), 0x1d7fa0, "Water", tags=("KillBrick",)))
    # Stage 3: a platform with a spinning kill bar.
    sp = cp1z - 54
    ws.append(part(pl, "SpinDeck", (0, 7, sp), (18, 2, 18), 0x4a4440, "Slate"))
    trim(pl, ws, "SpinDeckTrim", (0, 7, sp), (18, 2, 18), 0xffa040)
    ws.append(part(pl, "SpinHub", (0, 9, sp), (2, 2, 2), 0x37474f, "Metal", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "Spinner", (0, 9, sp), (16, 0.8, 0.8), 0xff1744, "Neon", tags=("KillBrick", "Spin")))
    ws.append(part(pl, "Checkpoint2", (0, 8.1, sp + 6), (5, 0.2, 5), 0x00e676, "Neon", collide=False,
                   tags=("Checkpoint",), attrs={"Stage": 2}))
    # Stage 4: a platform that glides across the gap.
    mv = sp - 22
    ws.append(part(pl, "Mover", (-8, 7.5, mv), (7, 1, 7), 0x6d7378, "DiamondPlate", tags=("Glide",)))
    ws.append(part(pl, "Pool3", (0, 2, mv - 6), (40, 1, 30), 0x1d7fa0, "Water", tags=("KillBrick",)))
    # Stage 5: a spiral climb around a pillar.
    top = mv - 24
    ws.append(part(pl, "Pillar", (0, 12, top), (40, 6, 6), 0x8d6e63, "Slate", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "Checkpoint3", (0, 8.6, top + 9), (5, 0.2, 5), 0x00e676, "Neon", collide=False,
                   tags=("Checkpoint",), attrs={"Stage": 3}))
    ws.append(part(pl, "Landing", (0, 7.5, top + 9), (8, 2, 8), 0x4a4440, "Slate"))
    trim(pl, ws, "LandingTrim", (0, 7.5, top + 9), (8, 2, 8), 0xffa040)
    import math
    for i in range(8):
        a = math.radians(90 + i * 50)
        ws.append(part(pl, "Spiral%d" % (i + 1), (math.cos(a) * 6.5, 9.5 + i * 2.6, top + math.sin(a) * 6.5),
                       (4, 1, 4), 0x3b3532, "Basalt", rot=yaw(-i * 50)))
    # The finish: a gold deck with an arch and a trophy.
    fy = 9.5 + 8 * 2.6
    ws.append(part(pl, "Finish", (0, fy, top - 12), (14, 1, 14), 0xd4a72c, "Metal", tags=("Finish",)))
    for x in (-6, 6):
        ws.append(part(pl, "ArchPost", (x, fy + 5, top - 17), (1.2, 10, 1.2), 0xfff176, "Neon"))
    ws.append(part(pl, "ArchTop", (0, fy + 10.4, top - 17), (13.2, 1.2, 1.2), 0xfff176, "Neon"))
    ws.append(part(pl, "TrophyBase", (0, fy + 1, top - 12), (1.2, 2, 2), 0x5d4037, "Wood", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "Trophy", (0, fy + 3, top - 12), (2.4, 2.4, 2.4), 0xffd54f, "Metal", shape=0,
                   children=sparkles(pl, 0xfff59d)))
    for x in (-6, 6):
        ws.append(part(pl, "Confetti", (x, fy + 10.4, top - 17), (1, 1, 1), 0xffffff, "Neon", transparency=1,
                       collide=False, children=emitter(pl, 0xff4081, 0x40c4ff, 14, (3, 6), (1.5, 2.5), 0.5, -6)))
    src = obby_script("obby")
    # Golden hour at sea: a low sun over the water, a light salt haze, and
    # torches lit for the evening.
    rest = [lighting(pl, 17.8, bloom(pl, 0.9, 0.95) + atmosphere(pl, 0.25, 0xf2b38a)
                     + color_correction(pl, 0.12, 0.12) + sun_rays(pl, 0.2), 2.6),
            service(pl, "ServerScriptService", script(pl, "Obby", src))]
    return place(ws, rest)

RAINBOW = [0xff3b3b, 0xff9a1f, 0xffd91a, 0x3fd65a, 0x2f8bff, 0x9b4dff]

def rainbow():
    """A classic sky obby: plastic bricks floating over a baseplate, one
    colour per stage from red to purple, and a rainbow bridge to the end."""
    import math
    pl = Place()
    ws = []
    # The ground far below. Falling is the fail state, so it is a kill brick.
    ws.append(part(pl, "Baseplate", (0, -40, -120), (600, 2, 600), 0x4caf50, "SmoothPlastic", tags=("KillBrick",)))
    # Blocky clouds off to both sides, clear of the line of sight down the course.
    for i in range(16):
        side = -1 if i % 2 else 1
        x = side * (45 + (i * 31) % 50)
        z = 20 - i * 16
        y = 6 + (i * 13) % 26
        w = 14 + (i * 7) % 12
        ws.append(part(pl, "Cloud", (x, y, z), (w, 4, w * 0.6), 0xffffff, "SmoothPlastic", collide=False,
                       tags=SCENERY))
        ws.append(part(pl, "Cloud", (x + w * 0.3, y + 2.5, z), (w * 0.5, 4, w * 0.45), 0xffffff, "SmoothPlastic",
                       collide=False, tags=SCENERY))
    # The lobby: a grey pad with a white spawn.
    ws.append(part(pl, "Lobby", (0, -1, 6), (24, 2, 24), 0xa3a2a5, "SmoothPlastic"))
    ws.append(part(pl, "Start", (0, 0.5, 8), (8, 1, 8), 0xf2f3f3, "SmoothPlastic", cls="SpawnLocation"))
    for i, c in enumerate(RAINBOW):
        ws.append(part(pl, "LobbyStripe", (-10 + i * 4, 0.06, -5), (4, 0.12, 2), c, "Neon", collide=False))
    y, z = 0.0, -4.0

    def pad(name, x, yy, zz, w, d, c, **kw):
        ws.append(part(pl, name, (x, yy, zz), (w, 1, d), c, "SmoothPlastic", **kw))

    def checkpoint(stage, c, yy, zz):
        ws.append(part(pl, "Stage%d" % stage, (0, yy - 0.5, zz), (12, 2, 12), c, "SmoothPlastic"))
        ws.append(part(pl, "Checkpoint%d" % stage, (0, yy + 0.6, zz), (6, 0.2, 6), 0xffffff, "Neon",
                       collide=False, tags=("Checkpoint",), attrs={"Stage": stage}))

    # Stage 1, red: plain jumps, gently rising.
    c = RAINBOW[0]
    for i in range(5):
        y += 0.8
        z -= 9
        pad("RedJump%d" % (i + 1), (i % 2) * 4 - 2, y, z, 5, 5, c)
    z -= 11
    checkpoint(1, c, y, z)
    # Stage 2, orange: a strip crossed by kill bricks.
    c = RAINBOW[1]
    z -= 6
    ws.append(part(pl, "OrangeStrip", (0, y - 0.5, z - 14), (6, 1, 30), c, "SmoothPlastic"))
    for i in range(4):
        ws.append(part(pl, "KillBrick", (0, y + 0.3, z - 4 - i * 7), (6, 0.6, 1.6), 0xff1f1f, "Neon",
                       tags=("KillBrick",)))
    z -= 37
    checkpoint(2, c, y, z)
    # Stage 3, yellow: narrow beams, each a step to the side of the last.
    c = RAINBOW[2]
    for i in range(4):
        z -= 9
        y += 0.6
        ws.append(part(pl, "YellowBeam%d" % (i + 1), ((i % 2) * 3 - 1.5, y - 0.5, z), (1.6, 1, 7), c,
                       "SmoothPlastic"))
    z -= 12
    checkpoint(3, c, y, z)
    # Stage 4, green: a platform that glides across the gap.
    c = RAINBOW[3]
    z -= 13
    pad("GreenGlider", -8, y, z, 6, 6, c, tags=("Glide",))
    z -= 13
    checkpoint(4, c, y, z)
    # Stage 5, blue: a wide deck with a spinning bar.
    c = RAINBOW[4]
    z -= 15
    ws.append(part(pl, "BlueDeck", (0, y - 0.5, z), (18, 1, 18), c, "SmoothPlastic"))
    ws.append(part(pl, "SpinHub", (0, y + 1, z), (2, 2, 2), 0xf2f3f3, "SmoothPlastic", shape=2, rot=UPRIGHT))
    ws.append(part(pl, "Spinner", (0, y + 1, z), (17, 0.8, 0.8), 0xff1f1f, "Neon", tags=("KillBrick", "Spin")))
    z -= 18
    checkpoint(5, c, y, z)
    # Stage 6, purple: small steps climbing up.
    c = RAINBOW[5]
    for i in range(6):
        z -= 6
        y += 1.4
        pad("PurpleStep%d" % (i + 1), (i % 3 - 1) * 3, y, z, 3, 3, c)
    # The rainbow bridge: a band of every colour, side by side.
    z -= 6
    for i, col in enumerate(RAINBOW):
        ws.append(part(pl, "Rainbow", (-5 + i * 2, y - 0.5, z - 12), (2, 1, 22), col, "SmoothPlastic"))
    z -= 30
    ws.append(part(pl, "Finish", (0, y - 0.5, z), (14, 1, 14), 0xffd700, "SmoothPlastic", tags=("Finish",)))
    for i, col in enumerate(RAINBOW):
        h = 13 - i
        ws.append(part(pl, "Arch", (-h / 2.0, y + h / 2.0, z - 5), (1, h, 1), col, "SmoothPlastic"))
        ws.append(part(pl, "Arch", (h / 2.0, y + h / 2.0, z - 5), (1, h, 1), col, "SmoothPlastic"))
        ws.append(part(pl, "Arch", (0, y + h, z - 5), (h + 1, 1, 1), col, "SmoothPlastic"))
    ws.append(part(pl, "Trophy", (0, y + 2, z), (2.4, 2.4, 2.4), 0xffd700, "SmoothPlastic", shape=0,
                   children=sparkles(pl, 0xffffff)))
    # Noon under a clear sky, no haze: the flat, bright look of a classic brick game.
    rest = [lighting(pl, 13.5, color_correction(pl, 0.15, 0.05), 3.0),
            service(pl, "ServerScriptService", script(pl, "Obby", obby_script("rainbow")))]
    return place(ws, rest)

def tycoon():
    pl = Place()
    ws = [part(pl, "Spawn", (0, 0, 20), (10, 1, 10), 0x607d8b, "Concrete", cls="SpawnLocation"),
          part(pl, "Floor", (0, -1, 0), (80, 1, 80), 0x8bc34a, "Grass"),
          part(pl, "Dropper", (-20, 10, 0), (4, 4, 4), 0x455a64, "Metal"),
          part(pl, "Conveyor", (-10, 1, 0), (24, 1, 4), 0x212121, "DiamondPlate",
               extra='<Vector3 name="AssemblyLinearVelocity"><X>12</X><Y>0</Y><Z>0</Z></Vector3>'),
          part(pl, "Collector", (4, 2, 0), (4, 3, 6), 0x00bcd4, "Neon", collide=False, transparency=0.4),
          part(pl, "BuyDropper", (-6, 0.6, 12), (4, 0.2, 4), 0x4caf50, "Neon", collide=False,
               attrs={"Price": 25}),
          part(pl, "BuyUpgrade", (2, 0.6, 12), (4, 0.2, 4), 0xffc107, "Neon", collide=False,
               attrs={"Price": 60})]
    # The factory hall around the line, the yard and the countryside.
    ws.append(part(pl, "HallWall", (-26, 6, 0), (1, 12, 26), 0xb0503c, "Brick"))
    ws.append(part(pl, "HallWall", (-12, 6, -13), (28, 12, 1), 0xb0503c, "Brick"))
    for x in (-25, -12, 1):
        ws.append(part(pl, "Beam", (x, 12.5, 0), (1, 1, 26), 0x5d4037, "Wood"))
    ws.append(part(pl, "Roof", (-12, 13.2, -6), (28, 0.4, 14), 0x3e4a52, "Slate"))
    ws.append(part(pl, "Chimney", (-22, 17, -9), (3, 10, 3), 0x8d4a3a, "Brick"))
    ws.append(part(pl, "ChimneyTop", (-22, 22.4, -9), (1, 1, 1), 0xffffff, transparency=1, collide=False,
                   children=smoke(pl, 0xb0b0b0, 3, 3)))
    ws.append(part(pl, "Path", (0, -0.45, 14), (6, 0.1, 22), 0xbcaaa4, "Concrete"))
    fence(pl, ws, -38, -38, 38, -38, -0.5)
    fence(pl, ws, -38, 38, -38, -38, -0.5)
    fence(pl, ws, 38, -38, 38, 38, -0.5)
    for x, z in ((12, -6), (12, 22), (-30, 26), (28, 30), (30, -26), (-32, -30)):
        tree(pl, ws, x, z, -0.5)
    for x, z in ((6, 20), (-6, 20), (16, 8)):
        lamp(pl, ws, x, z, -0.5)
    ws.append(part(pl, "Countryside", (0, -1.6, 0), (600, 1, 600), 0x7cb342, "Grass"))
    hills(pl, ws, 200, -1)
    src = LEADER + """
local Players = game:GetService("Players")
local owner = nil
local droppers = 1
local value = 5
local function claim(p)
    if owner == nil then owner = p end
    stats(p, {"Cash"})
end
Players.PlayerAdded:Connect(claim)
for _, p in ipairs(Players:GetPlayers()) do claim(p) end
local ws = workspace
local drops = Instance.new("Folder")
drops.Name = "Drops"
drops.Parent = ws
ws.Collector.Touched:Connect(function(hit)
    if hit.Parent == drops and owner then
        owner.leaderstats.Cash.Value += hit:GetAttribute("Value") or value
        hit:Destroy()
    end
end)
local function pad(part, onBuy)
    local busy = false
    part.Touched:Connect(function(hit)
        local p = playerOf(hit)
        if p == nil or p ~= owner or busy then return end
        local price = part:GetAttribute("Price")
        if p.leaderstats.Cash.Value >= price then
            busy = true
            p.leaderstats.Cash.Value -= price
            onBuy()
            part:SetAttribute("Price", price * 2)
            task.wait(1)
            busy = false
        end
    end)
end
pad(ws.BuyDropper, function() droppers += 1 end)
pad(ws.BuyUpgrade, function() value *= 2 end)
task.spawn(function()
    while true do
        for i = 1, droppers do
            local d = Instance.new("Part")
            d.Size = Vector3.new(1, 1, 1)
            d.Color = Color3.fromRGB(255, 200, 40)
            d.Material = Enum.Material.Neon
            d.CFrame = ws.Dropper.CFrame * CFrame.new((i - 1) * 0.3, -3, 0)
            d:SetAttribute("Value", value)
            d.Parent = drops
            game:GetService("Debris"):AddItem(d, 30)
        end
        task.wait(1.5)
    end
end)
print("TEMPLATE READY tycoon")
"""
    # A clear summer afternoon: crisp air, warm light on the brick.
    rest = [lighting(pl, 15.8, bloom(pl, 0.5, 1.2) + atmosphere(pl, 0.08, 0xdbe9ff) + color_correction(pl, 0.1, 0.1)
                     + sun_rays(pl, 0.12), 2.8),
            service(pl, "ServerScriptService", script(pl, "Tycoon", src))]
    return place(ws, rest)

def arena():
    pl = Place()
    ws = [part(pl, "Ring", (0, 0, 0), (60, 2, 60), 0x37474f, "Slate"),
          part(pl, "RingEdge", (0, 1.05, 0), (61, 0.15, 61), 0x00e5ff, "Neon", collide=False, transparency=0.6),
          part(pl, "SpawnA", (-20, 1.5, 0), (6, 1, 6), 0xe53935, "Neon", cls="SpawnLocation"),
          part(pl, "SpawnB", (20, 1.5, 0), (6, 1, 6), 0x1e88e5, "Neon", cls="SpawnLocation"),
          part(pl, "Lake", (0, -6, 0), (400, 1, 400), 0x14324a, "Water", tags=("KillBrick",)),
          part(pl, "Spinner", (0, 2.5, 0), (40, 1, 2), 0xffeb3b, "Neon", tags=("Spinner",)),
          part(pl, "Pad", (0, 1.1, 18), (5, 0.2, 5), 0x76ff03, "ForceField", collide=False, tags=("JumpPad",))]
    # Stone columns and torches around the ring, rocks in the lava.
    import math
    for i in range(12):
        a = math.radians(i * 30 + 15)
        x = math.sin(a) * 33
        z = math.cos(a) * 33
        ws.append(part(pl, "Column", (x, -2, z), (16, 3, 3), 0x9e9e9e, "Concrete", shape=2, rot=UPRIGHT))
        ws.append(part(pl, "Brazier", (x, 6.6, z), (1, 2.4, 2.4), 0x263238, "Metal", shape=2, rot=UPRIGHT,
                       children=fire(pl, 3, 10)))
    for x, c in ((-20, 0xe53935), (20, 0x1e88e5)):
        ws.append(part(pl, "BannerPole", (x, 6, -6), (0.5, 10, 0.5), 0x424242, "Metal"))
        ws.append(part(pl, "Banner", (x, 8.5, -4.9), (0.2, 4, 2.4), c, "Fabric"))
    for i in range(10):
        a = math.radians(i * 36 + 7)
        r = 60 + (i * 13) % 30
        h = 14 + (i * 7) % 18
        ws.append(part(pl, "Rock", (math.sin(a) * r, -6 + h / 2, math.cos(a) * r), (10, h, 10), 0x5d544c,
                       "Slate", rot=yaw(i * 33), tags=SCENERY))
    src = LEADER + """
local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local CollectionService = game:GetService("CollectionService")
local function setup(p)
    stats(p, {"Points", "KOs"})
    local function watch(c)
        local h = c:WaitForChild("Humanoid")
        h.Died:Connect(function()
            p.leaderstats.KOs.Value += 1
        end)
    end
    p.CharacterAdded:Connect(watch)
    if p.Character then task.spawn(watch, p.Character) end
end
Players.PlayerAdded:Connect(setup)
for _, p in ipairs(Players:GetPlayers()) do setup(p) end
for _, b in ipairs(CollectionService:GetTagged("KillBrick")) do
    b.Touched:Connect(function(hit)
        local h = hit.Parent and hit.Parent:FindFirstChildOfClass("Humanoid")
        if h then h.Health = 0 end
    end)
end
local spinner = workspace.Spinner
local angle = 0
RunService.Heartbeat:Connect(function(dt)
    angle += dt * 1.5
    spinner.CFrame = CFrame.new(0, 2.5, 0) * CFrame.Angles(0, angle, 0)
end)
spinner.Touched:Connect(function(hit)
    local h = hit.Parent and hit.Parent:FindFirstChildOfClass("Humanoid")
    if h then h:TakeDamage(10) end
end)
workspace.Pad.Touched:Connect(function(hit)
    local root = hit.Parent and hit.Parent:FindFirstChild("HumanoidRootPart")
    if root then root.AssemblyLinearVelocity = Vector3.new(0, 90, 0) end
end)
task.spawn(function()
    while true do
        task.wait(5)
        for _, p in ipairs(Players:GetPlayers()) do
            local c = p.Character
            local h = c and c:FindFirstChildOfClass("Humanoid")
            if h and h.Health > 0 then p.leaderstats.Points.Value += 1 end
        end
    end
end)
print("TEMPLATE READY arena")
"""
    # Night over a lake: moonlight, the braziers' flames and the neon ring
    # carry the light, and bloom makes them glow.
    rest = [lighting(pl, 22.5, bloom(pl, 1.4, 0.8) + atmosphere(pl, 0.25, 0x203050) + color_correction(pl, 0.1, 0.18),
                     1.6),
            service(pl, "ServerScriptService", script(pl, "Arena", src))]
    return place(ws, rest)

def simulator():
    pl = Place()
    ws = [part(pl, "Field", (0, -1, 0), (120, 1, 120), 0xdce775, "Sand"),
          part(pl, "Spawn", (0, 0, 0), (8, 1, 8), 0x8d6e63, "Wood", cls="SpawnLocation"),
          part(pl, "SellPad", (0, 0.1, -20), (8, 0.2, 8), 0x00e5ff, "Neon", collide=False),
          part(pl, "UpgradePad", (12, 0.1, -20), (8, 0.2, 8), 0xff4081, "Neon", collide=False,
               attrs={"Price": 20})]
    # Crystal outcrops, a shop and hills around the field.
    import math
    crystals = [0xb388ff, 0x40c4ff, 0x69f0ae, 0xff80ab]
    for i in range(14):
        a = math.radians(i * 360.0 / 14)
        r = 38 + (i % 3) * 7
        x = math.sin(a) * r
        z = math.cos(a) * r
        ws.append(part(pl, "Outcrop", (x, 0.5, z), (8, 3, 8), 0x8d8478, "Slate", rot=yaw(i * 29)))
        for k in range(3):
            h = 5 + ((i + k) * 3) % 6
            ws.append(part(pl, "Crystal", (x + (k - 1) * 2, 2 + h / 2, z + ((k * 7) % 3 - 1)), (1.6, h, 1.6),
                           crystals[(i + k) % 4], "Neon", rot=yaw(i * 17 + k * 40),
                           children=sparkles(pl, crystals[(i + k) % 4]) if k == 1 else ""))
    ws.append(part(pl, "Shop", (0, 3.5, -30), (14, 7, 8), 0xd7ccc8, "WoodPlanks" if "WoodPlanks" in MAT else "Wood"))
    ws.append(part(pl, "ShopRoof", (0, 8, -30), (16, 2, 10), 0xc62828, "SmoothPlastic", cls="WedgePart"))
    ws.append(part(pl, "ShopSign", (0, 5.5, -25.9), (8, 1.6, 0.3), 0x00e5ff, "Neon"))
    for x, z in ((-20, 10), (22, 14), (-18, -16), (24, -12)):
        tree(pl, ws, x, z, -0.5)
    ws.append(part(pl, "Desert", (0, -1.6, 0), (600, 1, 600), 0xd4c070, "Sand"))
    hills(pl, ws, 190, -1, color=0xc8a46a, mat="Sand", trees=False)
    src = LEADER + """
local Players = game:GetService("Players")
local gems = Instance.new("Folder")
gems.Name = "Gems"
gems.Parent = workspace
local rng = Random.new(7)
local function setup(p)
    stats(p, {"Gems", "Coins", "Power"})
    p.leaderstats.Power.Value = 1
end
Players.PlayerAdded:Connect(setup)
for _, p in ipairs(Players:GetPlayers()) do setup(p) end
local function spawnGem()
    local g = Instance.new("Part")
    g.Shape = Enum.PartType.Ball
    g.Size = Vector3.new(1.5, 1.5, 1.5)
    g.Anchored = true
    g.CanCollide = false
    g.Material = Enum.Material.Glass
    g.Color = Color3.fromHSV(rng:NextNumber(), 0.8, 1)
    g.Position = Vector3.new(rng:NextInteger(-32, 32), 1, rng:NextInteger(-32, 32))
    g.Touched:Connect(function(hit)
        local p = playerOf(hit)
        if p and g.Parent then
            p.leaderstats.Gems.Value += p.leaderstats.Power.Value
            g:Destroy()
            task.delay(3, spawnGem)
        end
    end)
    g.Parent = gems
end
for i = 1, 60 do spawnGem() end
workspace.SellPad.Touched:Connect(function(hit)
    local p = playerOf(hit)
    if p then
        p.leaderstats.Coins.Value += p.leaderstats.Gems.Value * 2
        p.leaderstats.Gems.Value = 0
    end
end)
workspace.UpgradePad.Touched:Connect(function(hit)
    local p = playerOf(hit)
    local pad = workspace.UpgradePad
    if p and p.leaderstats.Coins.Value >= pad:GetAttribute("Price") then
        p.leaderstats.Coins.Value -= pad:GetAttribute("Price")
        p.leaderstats.Power.Value += 1
    end
end)
task.spawn(function()
    while true do
        task.wait(2)
        if #gems:GetChildren() < 40 then spawnGem() end
    end
end)
print("TEMPLATE READY simulator")
"""
    # Golden hour in the desert: long shadows and crystals that catch it.
    rest = [lighting(pl, 16.9, bloom(pl, 0.8, 1.0) + atmosphere(pl, 0.1, 0xffd9a0) + color_correction(pl, 0.1, 0.12)
                     + sun_rays(pl, 0.16), 2.8),
            service(pl, "ServerScriptService", script(pl, "Simulator", src))]
    return place(ws, rest)

def racing():
    pl = Place()
    ws = [part(pl, "Ground", (0, -1, 0), (300, 1, 300), 0x689f38, "Grass", tags=SCENERY),
          part(pl, "Grid", (0, 0, 60), (16, 1, 10), 0xeeeeee, "Asphalt", cls="SpawnLocation")]
    gates = [(0, 60), (60, 0), (0, -60), (-60, 0)]
    for i in range(16):
        import math
        a = i / 16.0 * math.pi * 2
        ws.append(part(pl, "Track%d" % i, (math.sin(a) * 60, -0.4, math.cos(a) * 60), (26, 0.2, 26), 0x424242,
                       "Asphalt"))
    for i, (x, z) in enumerate(gates):
        ws.append(part(pl, "Gate%d" % (i + 1), (x, 4, z), (16, 8, 2), 0xffffff, "ForceField", collide=False,
                       transparency=0.6, tags=("Gate",), attrs={"Index": (i + 1)}))
    # Infield, grandstand, a start arch, tyre stacks and hills.
    import math
    ws.append(part(pl, "Infield", (0, -0.45, 0), (70, 0.1, 70), 0x7cb342, "Grass"))
    # The bay the loop runs around: a pond in the infield.
    ws.append(part(pl, "Bay", (0, -0.5, 0), (44, 0.2, 44), 0x2a86b0, "Water", shape=2, rot=UPRIGHT))
    for i in range(8):
        a = math.radians(i * 45 + 20)
        tree(pl, ws, math.sin(a) * 22, math.cos(a) * 22, -0.5)
    for k in range(4):
        ws.append(part(pl, "Stand", (0, 1 + k * 1.5, 92 + k * 3), (40, 2 + k * 3, 3), 0x90a4ae, "Concrete"))
        ws.append(part(pl, "Seats", (0, 2.2 + k * 3, 92 + k * 3), (38, 0.4, 2), (0xe53935, 0xfdd835, 0x1e88e5,
                       0x43a047)[k], "SmoothPlastic"))
    ws.append(part(pl, "StandRoof", (0, 15, 97), (44, 0.6, 12), 0xeceff1, "Metal"))
    for x in (-9, 9):
        ws.append(part(pl, "ArchPost", (x, 6, 60), (1, 12, 1), 0x212121, "Metal"))
    ws.append(part(pl, "ArchTop", (0, 12.5, 60), (19, 2, 1), 0xffffff, "SmoothPlastic"))
    ws.append(part(pl, "ArchStripe", (0, 12.5, 60.6), (17, 0.8, 0.1), 0xff1744, "Neon"))
    for i in range(16):
        a = i / 16.0 * math.pi * 2 + 0.2
        for k in range(2):
            ws.append(part(pl, "Tyre", (math.sin(a) * 76, 0.5 + k, math.cos(a) * 76), (1, 3, 3), 0x212121,
                           "Plastic", shape=2, rot=UPRIGHT))
    hills(pl, ws, 220, -1)
    src = LEADER + """
local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
local GATES = 4
local state = {}
local function setup(p)
    stats(p, {"Laps", "Best"})
    state[p] = {next = 2, start = time()}
end
Players.PlayerAdded:Connect(setup)
for _, p in ipairs(Players:GetPlayers()) do setup(p) end
for _, g in ipairs(CollectionService:GetTagged("Gate")) do
    g.Touched:Connect(function(hit)
        local p = playerOf(hit)
        local s = p and state[p]
        if s == nil then return end
        local idx = g:GetAttribute("Index")
        if idx ~= s.next then return end
        if idx == 1 then
            local t = math.floor((time() - s.start) * 10)
            p.leaderstats.Laps.Value += 1
            if p.leaderstats.Best.Value == 0 or t < p.leaderstats.Best.Value then
                p.leaderstats.Best.Value = t
            end
            s.start = time()
        end
        s.next = idx % GATES + 1
    end)
end
print("TEMPLATE READY racing")
"""
    rest = [lighting(pl, 14.5, bloom(pl, 0.6, 1.1) + atmosphere(pl, 0.08, 0xcfe3ff) + color_correction(pl, 0.1, 0.1)
                     + sun_rays(pl, 0.1), 2.8),
            service(pl, "ServerScriptService", script(pl, "Racing", src))]
    return place(ws, rest)

def baseplate():
    """An empty world to build in from scratch: a baseplate, a spawn and a sky."""
    pl = Place()
    ws = [part(pl, "Baseplate", (0, -0.5, 0), (240, 1, 240), 0x5f9e4f, "Grass"),
          part(pl, "Spawn", (0, 0.1, 30), (8, 0.2, 8), 0x9e9e9e, "Concrete", cls="SpawnLocation")]
    # The horizon in one folder, so the Explorer starts with what you build.
    scenery = []
    hills(pl, scenery, 200, -1)
    ws.append(folder(pl, "Scenery", "\n".join(scenery)))
    rest = [lighting(pl, 14.8, bloom(pl, 0.5, 1.2) + atmosphere(pl, 0.08, 0xdbe9ff) + color_correction(pl, 0.1, 0.1),
                     2.8),
            service(pl, "ServerScriptService")]
    return place(ws, rest)

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "com", "codename1",
                   "rbx", "templates")
for name, fn in (("baseplate", baseplate), ("obby", obby), ("rainbow", rainbow), ("tycoon", tycoon), ("arena", arena), ("simulator", simulator),
                 ("racing", racing)):
    with open(os.path.join(OUT, name + ".rbxlx"), "w") as f:
        f.write(fn())
print("wrote", OUT)
