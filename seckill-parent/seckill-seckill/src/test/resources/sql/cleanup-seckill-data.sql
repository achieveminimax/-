-- 测试数据清理脚本
-- 用于秒杀模块集成测试后清理

DELETE FROM t_seckill_record WHERE activity_id = 1;
DELETE FROM t_order WHERE activity_id = 1;
DELETE FROM t_seckill_goods WHERE activity_id = 1;
DELETE FROM t_seckill_activity WHERE id = 1;
DELETE FROM t_goods WHERE id = 2001;
DELETE FROM t_category WHERE id = 100;
DELETE FROM t_address WHERE id = 3001;
DELETE FROM t_user WHERE id = 1001;
