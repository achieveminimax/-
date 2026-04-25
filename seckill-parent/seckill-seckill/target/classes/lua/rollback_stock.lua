local stockKey = KEYS[1]
local doneKey = KEYS[2]
local quantity = tonumber(ARGV[1])
local userId = ARGV[2]
local doneTtl = tonumber(ARGV[3])

redis.call('INCRBY', stockKey, quantity)
redis.call('SREM', doneKey, userId)
if doneTtl > 0 then
    redis.call('EXPIRE', doneKey, doneTtl)
end
return 1
