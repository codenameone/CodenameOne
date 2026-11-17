-- arithmetic, strings, tables, closures, varargs
local x: number = 1 + 2 * 3
assert(x == 7)
assert(7 // 2 == 3 and -7 // 2 == -4 and 7 % -3 == -2 and 2 ^ 10 == 1024)
assert(10 / 4 == 2.5)
assert(tostring(1/0) == "inf" and tostring(10) == "10" and tostring(0.1) == "0.1")
assert(tostring(1e20) == "1e+20", tostring(1e20))
assert("10" + 1 == 11)
assert(#"hello" == 5 and "a" .. "b" .. 1 == "ab1")
local t = {1, 2, 3, n = "x", [10] = 5}
assert(#t == 3 and t.n == "x" and t[10] == 5)
local function sum(...)
    local s = 0
    for _, v in ipairs({...}) do s += v end
    return s, select("#", ...)
end
local a, b = sum(1, 2, 3)
assert(a == 6 and b == 3)
local function counter()
    local c = 0
    return function() c += 1; return c end
end
local c1, c2 = counter(), counter()
assert(c1() == 1 and c1() == 2 and c2() == 1)
-- closures per iteration
local fs = {}
for i = 1, 3 do fs[i] = function() return i end end
assert(fs[1]() == 1 and fs[3]() == 3)
-- while / repeat / continue / break
local n = 0
while true do n += 1; if n < 5 then continue end; break end
assert(n == 5)
local r = 0
repeat local q = r; r += 1 until q >= 3
assert(r == 4)
-- generalized iteration
local keys = 0
for k, v in {a = 1, b = 2, 3} do keys += 1 end
assert(keys == 3)
-- if expression, interpolation
local name = "world"
local s = `hello {name}, {1 + 1} {if x > 5 then "big" else "small"}`
assert(s == "hello world, 2 big", s)
-- multiple assignment and swaps
local p, q = 1, 2
p, q = q, p
assert(p == 2 and q == 1)
-- methods and metatables
local Account = {}
Account.__index = Account
function Account.new(b) return setmetatable({balance = b}, Account) end
function Account:deposit(v) self.balance += v; return self end
local acc = Account.new(10):deposit(5)
assert(acc.balance == 15)
local V = {}
V.__index = V
V.__add = function(a, b) return setmetatable({x = a.x + b.x}, V) end
V.__eq = function(a, b) return a.x == b.x end
V.__tostring = function(a) return "V(" .. a.x .. ")" end
V.__lt = function(a, b) return a.x < b.x end
local v1, v2 = setmetatable({x = 1}, V), setmetatable({x = 2}, V)
assert((v1 + v2).x == 3 and tostring(v1) == "V(1)" and v1 < v2 and v1 + v1 == v2)
-- pcall / error
local ok, err = pcall(function() error("boom") end)
assert(not ok and err:find("boom"), err)
local ok2, err2 = pcall(function() local z = nil; return z.y end)
assert(not ok2 and err2:find("attempt to index nil"), err2)
local ok3, e3 = pcall(error, {code = 7})
assert(not ok3 and e3.code == 7)
assert(select(2, xpcall(function() error("x", 0) end, function(m) return "handled " .. m end)) == "handled x")
-- type annotations and aliases are ignored
type Point = { x: number, y: number }
export type Maybe<T> = T?
local function dist(p: Point): number
    return math.sqrt(p.x * p.x + p.y * p.y)
end
assert(dist({x = 3, y = 4}) == 5)
local casted = (5 :: any) :: number
assert(casted == 5)
print("basics OK")
