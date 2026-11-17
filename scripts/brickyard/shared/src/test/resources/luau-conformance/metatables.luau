-- __index function and table chains, __newindex, __call, __len, __concat, __unm
local log = {}
local proxy = setmetatable({}, {
    __index = function(t, k) return k .. "!" end,
    __newindex = function(t, k, v) table.insert(log, k); rawset(t, k, v) end,
    __call = function(self, a, b) return a * b end,
    __len = function() return 42 end,
    __concat = function(a, b) return "cat" end,
    __unm = function() return "neg" end,
})
assert(proxy.foo == "foo!")
proxy.bar = 1
assert(log[1] == "bar" and rawget(proxy, "bar") == 1)
proxy.bar = 2
assert(#log == 1)
assert(proxy(6, 7) == 42 and #proxy == 42 and proxy .. "x" == "cat" and -proxy == "neg")
-- inheritance chain
local Base = {}
Base.__index = Base
function Base:hello() return "base " .. self.name end
local Derived = setmetatable({}, {__index = Base})
Derived.__index = Derived
function Derived:bye() return "bye " .. self.name end
local obj = setmetatable({name = "o"}, Derived)
assert(obj:hello() == "base o" and obj:bye() == "bye o")
-- __iter
local Range = {}
Range.__iter = function(r)
    local i = r.lo - 1
    return function() i += 1; if i <= r.hi then return i end end
end
local sum = 0
for i in setmetatable({lo = 1, hi = 4}, Range) do sum += i end
assert(sum == 10)
-- __le, __lt with mixed
local W = {}
W.__le = function(a, b) return a.v <= b.v end
W.__lt = function(a, b) return a.v < b.v end
local w1, w2 = setmetatable({v = 1}, W), setmetatable({v = 1}, W)
assert(w1 <= w2 and not (w1 < w2) and w1 >= w2)
-- string methods through the string metatable
local s = "abc"
assert(s:upper() == "ABC" and s:len() == 3 and ("%d"):format(5) == "5")
-- rawequal / rawlen
assert(rawequal(obj, obj) and not rawequal(w1, w2) and rawlen({1, 2}) == 2)
-- next ordering is stable while assigning existing keys to nil
local t = {a = 1, b = 2, c = 3, d = 4}
local seen = 0
for k in pairs(t) do t[k] = nil; seen += 1 end
assert(seen == 4 and next(t) == nil)
-- array/hash migration
local arr = {}
arr[3] = "c"; arr[2] = "b"; arr[1] = "a"
assert(#arr == 3 and table.concat(arr) == "abc")
print("metatables OK")
