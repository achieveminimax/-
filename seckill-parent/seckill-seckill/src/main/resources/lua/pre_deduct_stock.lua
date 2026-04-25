local quantity = tonumber(ARGV[1])
local userId = ARGV[2]
local doneTtl = tonumber(ARGV[3])
local stockValue = redis.call('GET', KEYS[1])

if stockValue == false then
    return -3
end

local stock = tonumber(stockValue)
if stock < quantity then
    return -1
end

if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
    return -2
end

redis.call('DECRBY', KEYS[1], quantity)
redis.call('SADD', KEYS[2], userId)

if doneTtl > 0 then
    redis.call('EXPIRE', KEYS[2], doneTtl)
end

return 1
