local function msg(f, ...)
    local ok, e = pcall(f, ...)
    assert(not ok, "expected an error")
    return e
end
assert(msg(function() local t = nil; t.x = 1 end):find("attempt to index nil with 'x'"))
assert(msg(function() return 1 + {} end):find("attempt to perform arithmetic"))
assert(msg(function() return "a" < 1 end):find("attempt to compare"))
assert(msg(function() return #5 end):find("attempt to get length of a number value"))
assert(msg(function() local f; f() end):find("attempt to call a nil value"))
assert(msg(function() return ("x"):nope() end):find("missing method 'nope'"))
assert(msg(function() error("lvl", 2) end))
local e = msg(function() error("here") end)
assert(e:match("^errors:%d+: here$"), e)
assert(msg(string.rep) :find("string expected"))
assert(msg(setmetatable, 1, {}):find("table expected"))
-- protected metatable
local p = setmetatable({}, {__metatable = "locked"})
assert(getmetatable(p) == "locked")
assert(msg(setmetatable, p, {}):find("protected metatable"))
-- error objects pass through untouched
local obj = {}
assert(msg(error, obj) == obj)
-- nested pcall unwinds only to the nearest
local ok, inner = pcall(function()
    local ok2, e2 = pcall(error, "deep", 0)
    assert(not ok2 and e2 == "deep")
    return "outer fine"
end)
assert(ok and inner == "outer fine")
-- assert with message and values
assert(select("#", assert(1, 2, 3)) == 3)
assert(msg(assert, false, "custom") == "custom")
assert(msg(assert, nil):find("assertion failed"))
-- syntax errors come back from load-time compile via pcall of a loader in Java tests
print("errors OK")
