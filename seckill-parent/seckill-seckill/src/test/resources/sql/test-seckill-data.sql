-- 测试数据准备脚本
-- 用于秒杀模块集成测试

-- 清理旧数据（以防万一）
DELETE FROM t_seckill_record WHERE activity_id = 1;
DELETE FROM t_order WHERE activity_id = 1;
DELETE FROM t_seckill_goods WHERE activity_id = 1;
DELETE FROM t_seckill_activity WHERE id = 1;
DELETE FROM t_goods WHERE id = 2001;
DELETE FROM t_category WHERE id = 100;
DELETE FROM t_address WHERE id = 3001;
DELETE FROM t_user WHERE id = 1001;

-- 插入测试用户
INSERT INTO t_user (id, username, password, phone, nickname, status, create_time, update_time, deleted)
VALUES (1001, 'testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '13800138000', '测试用户', 1, NOW(), NOW(), 0);

-- 插入测试分类
INSERT INTO t_category (id, parent_id, name, sort, status, create_time, update_time)
VALUES (100, 0, '测试分类', 1, 1, NOW(), NOW());

-- 插入测试商品（库存 100）
INSERT INTO t_goods (id, name, description, price, stock, sales, category_id, cover_image, status, create_time, update_time, deleted)
VALUES (2001, '测试秒杀商品', '用于集成测试的秒杀商品', 999.00, 100, 0, 100, 'https://example.com/test.jpg', 1, NOW(), NOW(), 0);

-- 插入测试地址
INSERT INTO t_address (id, user_id, receiver_name, receiver_phone, province, city, district, detail_address, is_default, create_time, update_time)
VALUES (3001, 1001, '张三', '13800138000', '广东省', '深圳市', '南山区', '科技园 1 号', 1, NOW(), NOW());

-- 插入秒杀活动（进行中）
INSERT INTO t_seckill_activity (id, activity_name, description, start_time, end_time, status, create_time, update_time)
VALUES (1, '测试秒杀活动', '用于集成测试的秒杀活动', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 1, NOW(), NOW());

-- 插入秒杀商品关联（库存 100）
INSERT INTO t_seckill_goods (id, activity_id, goods_id, seckill_price, seckill_stock, limit_per_user, sales_count, create_time, update_time)
VALUES (1, 1, 2001, 99.00, 100, 1, 0, NOW(), NOW());
