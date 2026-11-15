#!/usr/bin/env python3
# Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
# Licensed under the GNU General Public License version 2 with the Classpath exception.
"""Writes the starter templates the studio offers (obby, tycoon, arena,
simulator, racing) as .rbxlx place files. They are original: layouts and
scripts written for Brickyard. Re-run after editing; the output is checked in.

    maven/rbx-compat/tools/gen-templates.py
"""
import os
from xml.sax.saxutils import escape

MAT = {"Plastic": 256, "SmoothPlastic": 272, "Neon": 288, "Wood": 512, "Concrete": 816, "Brick": 848,
       "Metal": 1088, "Grass": 1280, "Sand": 1296, "Ice": 1536, "Glass": 1568, "ForceField": 1584,
       "CrackedLava": 804, "Slate": 800, "DiamondPlate": 1056, "Asphalt": 1376, "Water": 2048}

class Place:
    def __init__(self):
        self.n = 0
        self.out = []

    def ref(self):
        self.n += 1
        return "RBX%d" % self.n

def cf(x, y, z):
    return ("<X>%g</X><Y>%g</Y><Z>%g</Z><R00>1</R00><R01>0</R01><R02>0</R02><R10>0</R10><R11>1</R11>"
            "<R12>0</R12><R20>0</R20><R21>0</R21><R22>1</R22>") % (x, y, z)

def rgb(c):
    return 0xff000000 | c

def part(pl, name, pos, size, color=0xa3a2a5, mat="Plastic", cls="Part", anchored=True, collide=True,
         transparency=0, tags=(), attrs=None, shape=0, extra=""):
    p = ['<Item class="%s" referent="%s"><Properties>' % (cls, pl.ref()),
         '<string name="Name">%s</string>' % escape(name),
         '<bool name="Anchored">%s</bool>' % ("true" if anchored else "false"),
         '<bool name="CanCollide">%s</bool>' % ("true" if collide else "false"),
         '<CoordinateFrame name="CFrame">%s</CoordinateFrame>' % cf(*pos),
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
    p.append('</Properties></Item>')
    return "".join(p)

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

def lighting(pl, clock, effects=""):
    return service(pl, "Lighting", effects, '<float name="ClockTime">%g</float>' % clock
                   + '<float name="Brightness">2.5</float>')

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

def obby():
    pl = Place()
    ws = [part(pl, "Start", (0, 0, 0), (16, 1, 16), 0x4caf50, "Grass", cls="SpawnLocation")]
    z = 0
    colors = [0x2196f3, 0xffc107, 0x9c27b0, 0xff5722]
    stage = 1
    for i in range(12):
        z -= 12
        y = 1 + i * 1.5
        ws.append(part(pl, "Step%d" % (i + 1), (((i % 3) - 1) * 6, y, z), (6, 1, 6), colors[i % 4], "SmoothPlastic"))
        if i % 4 == 3:
            ws.append(part(pl, "Checkpoint%d" % stage, (((i % 3) - 1) * 6, y + 0.6, z), (4, 0.2, 4), 0x00e676,
                           "Neon", collide=False, tags=("Checkpoint",),
                           attrs={"Stage": stage}))
            stage += 1
        if i % 3 == 1:
            ws.append(part(pl, "Lava%d" % i, (((i % 3) - 1) * 6, y - 3, z + 6), (14, 1, 6), 0xff3d00,
                           "CrackedLava", tags=("KillBrick",)))
    ws.append(part(pl, "Finish", (0, 20, z - 12), (12, 1, 12), 0xffd700, "Neon", tags=("Finish",)))
    src = LEADER + """
local Players = game:GetService("Players")
local CollectionService = game:GetService("CollectionService")
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
print("TEMPLATE READY obby")
"""
    rest = [lighting(pl, 14, bloom(pl, 0.6, 1.2)), service(pl, "ServerScriptService", script(pl, "Obby", src))]
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
    rest = [lighting(pl, 15), service(pl, "ServerScriptService", script(pl, "Tycoon", src))]
    return place(ws, rest)

def arena():
    pl = Place()
    ws = [part(pl, "Ring", (0, 0, 0), (60, 2, 60), 0x37474f, "Slate"),
          part(pl, "SpawnA", (-20, 1.5, 0), (6, 1, 6), 0xe53935, "Neon", cls="SpawnLocation"),
          part(pl, "SpawnB", (20, 1.5, 0), (6, 1, 6), 0x1e88e5, "Neon", cls="SpawnLocation"),
          part(pl, "Lava", (0, -6, 0), (200, 1, 200), 0xff3d00, "CrackedLava", tags=("KillBrick",)),
          part(pl, "Spinner", (0, 2.5, 0), (40, 1, 2), 0xffeb3b, "Neon", tags=("Spinner",)),
          part(pl, "Pad", (0, 1.1, 18), (5, 0.2, 5), 0x76ff03, "ForceField", collide=False, tags=("JumpPad",))]
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
    rest = [lighting(pl, 20.5, bloom(pl, 1.2, 0.8) + atmosphere(pl, 0.35, 0x40304a)),
            service(pl, "ServerScriptService", script(pl, "Arena", src))]
    return place(ws, rest)

def simulator():
    pl = Place()
    ws = [part(pl, "Field", (0, -1, 0), (120, 1, 120), 0xdce775, "Sand"),
          part(pl, "Spawn", (0, 0, 0), (8, 1, 8), 0x8d6e63, "Wood", cls="SpawnLocation"),
          part(pl, "SellPad", (0, 0.1, -20), (8, 0.2, 8), 0x00e5ff, "Neon", collide=False),
          part(pl, "UpgradePad", (12, 0.1, -20), (8, 0.2, 8), 0xff4081, "Neon", collide=False,
               attrs={"Price": 20})]
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
    g.Position = Vector3.new(rng:NextInteger(-50, 50), 1, rng:NextInteger(-50, 50))
    g.Touched:Connect(function(hit)
        local p = playerOf(hit)
        if p and g.Parent then
            p.leaderstats.Gems.Value += p.leaderstats.Power.Value
            g:Destroy()
        end
    end)
    g.Parent = gems
end
for i = 1, 40 do spawnGem() end
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
    rest = [lighting(pl, 11), service(pl, "ServerScriptService", script(pl, "Simulator", src))]
    return place(ws, rest)

def racing():
    pl = Place()
    ws = [part(pl, "Ground", (0, -1, 0), (300, 1, 300), 0x689f38, "Grass"),
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
    rest = [lighting(pl, 17.6, bloom(pl, 0.8, 1.0)), service(pl, "ServerScriptService", script(pl, "Racing", src))]
    return place(ws, rest)

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "com", "codename1",
                   "rbx", "templates")
for name, fn in (("obby", obby), ("tycoon", tycoon), ("arena", arena), ("simulator", simulator),
                 ("racing", racing)):
    with open(os.path.join(OUT, name + ".rbxlx"), "w") as f:
        f.write(fn())
print("wrote", OUT)
