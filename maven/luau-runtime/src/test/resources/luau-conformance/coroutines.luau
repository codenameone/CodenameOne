local co = coroutine.create(function(a, b)
    local c = coroutine.yield(a + b)
    local d, e = coroutine.yield(c * 2)
    return d + e
end)
local ok, v = coroutine.resume(co, 1, 2)
assert(ok and v == 3)
ok, v = coroutine.resume(co, 10)
assert(ok and v == 20)
ok, v = coroutine.resume(co, 3, 4)
assert(ok and v == 7 and coroutine.status(co) == "dead")
assert(not coroutine.resume(co))
-- generators via wrap
local function range(n)
    return coroutine.wrap(function()
        for i = 1, n do coroutine.yield(i) end
    end)
end
local total = 0
for i in range(10) do total += i end
assert(total == 55)
-- yield across pcall
local co2 = coroutine.create(function()
    local ok, err = pcall(function()
        coroutine.yield("inside")
        error("after")
    end)
    coroutine.yield(ok, err)
    return "done"
end)
local _, y1 = coroutine.resume(co2)
assert(y1 == "inside")
local _, ok2, err2 = coroutine.resume(co2)
assert(ok2 == false and err2:find("after"))
assert(select(2, coroutine.resume(co2)) == "done")
-- errors inside coroutines
local co3 = coroutine.create(function() error("bad", 0) end)
local ok3, e3 = coroutine.resume(co3)
assert(not ok3 and e3 == "bad")
assert(coroutine.isyieldable() == false)
assert(coroutine.wrap(function() return coroutine.isyieldable() end)())
-- deep recursion is fine (frames are not Java stack)
local function depth(n) if n == 0 then return 0 end return 1 + depth(n - 1) end
assert(depth(5000) == 5000)
local ok4, e4 = pcall(function() local function inf(n) return 1 + inf(n) end return inf(1) end)
assert(not ok4 and e4:find("stack overflow"), e4)
print("coroutines OK")
