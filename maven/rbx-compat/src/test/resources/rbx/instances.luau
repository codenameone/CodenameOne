local folder = Instance.new("Folder")
folder.Name = "Stuff"
local added = 0
folder.ChildAdded:Connect(function() added += 1 end)
local part = Instance.new("Part")
part.Name = "Brick"
part.Size = Vector3.new(4, 1, 4)
part.Position = Vector3.new(0, 10, 0)
part.Parent = folder
folder.Parent = workspace
assert(added == 1, "ChildAdded")
assert(workspace.Stuff.Brick == part, "index by name")
assert(part:IsDescendantOf(workspace), "descendant")
assert(part:GetFullName() == "Workspace.Stuff.Brick", part:GetFullName())
local changes = {}
part:GetPropertyChangedSignal("Transparency"):Connect(function() table.insert(changes, part.Transparency) end)
part.Transparency = 0.5
assert(changes[1] == 0.5, "property signal")
assert(part.Position.Y == 10, "position")
part.BrickColor = BrickColor.new("Really red")
assert(part.Color == BrickColor.new("Really red").Color, "brickcolor sets color")
local ok, err = pcall(function() part.NotAProp = 1 end)
assert(not ok and string.find(err, "not a valid member"), "unknown member error")
part:SetAttribute("Coins", 5)
assert(part:GetAttribute("Coins") == 5, "attributes")
local cs = game:GetService("CollectionService")
cs:AddTag(part, "Lava")
assert(#cs:GetTagged("Lava") == 1 and part:HasTag("Lava"), "tags")
local clone = folder:Clone()
assert(clone.Brick ~= part and clone.Brick.Size == part.Size, "clone")
local model = Instance.new("Model")
local a = Instance.new("Part"); a.Position = Vector3.new(0, 0, 0); a.Parent = model
local b = Instance.new("Part"); b.Position = Vector3.new(10, 0, 0); b.Parent = model
model.Parent = workspace
model:PivotTo(CFrame.new(100, 0, 0))
assert(math.abs(a.Position.X - 95) < 1e-6 and math.abs(b.Position.X - 105) < 1e-6, "pivot " .. tostring(a.Position))
local ev = Instance.new("BindableEvent")
local got
ev.Event:Connect(function(x) got = x end)
ev:Fire(7)
assert(got == 7, "bindable")
local bf = Instance.new("BindableFunction")
bf.OnInvoke = function(x) return x * 2 end
assert(bf:Invoke(21) == 42, "bindable function")
part:Destroy()
assert(part.Parent == nil, "destroy")
local ok2 = pcall(function() part.Parent = workspace end)
assert(not ok2, "destroyed parent locked")
print("DONE")
