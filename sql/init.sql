-- ========================================================
-- 电商秒杀系统数据库初始化脚本
-- 数据库: seckill_db
-- 字符集: utf8mb4
-- ========================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE seckill_db;

-- ========================================================
-- 1. 用户表 (t_user)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '用户名',
    password VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(32) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(11) NOT NULL COMMENT '手机号',
    avatar VARCHAR(256) DEFAULT NULL COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    email VARCHAR(64) DEFAULT NULL COMMENT '邮箱',
    birthday DATE DEFAULT NULL COMMENT '生日',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    login_fail_count INT DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time DATETIME DEFAULT NULL COMMENT '账号锁定时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ========================================================
-- 2. 收货地址表 (t_address)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '地址ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(32) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(11) NOT NULL COMMENT '收货人电话',
    province VARCHAR(32) NOT NULL COMMENT '省',
    city VARCHAR(32) NOT NULL COMMENT '市',
    district VARCHAR(32) NOT NULL COMMENT '区',
    detail_address VARCHAR(256) NOT NULL COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认：0-否 1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ========================================================
-- 3. 商品分类表 (t_category)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID（0为一级分类）',
    name VARCHAR(32) NOT NULL COMMENT '分类名称',
    sort INT DEFAULT 0 COMMENT '排序序号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ========================================================
-- 4. 商品表 (t_goods)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '商品ID',
    name VARCHAR(128) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '商品原价',
    stock INT NOT NULL DEFAULT 0 COMMENT '总库存',
    category_id BIGINT COMMENT '分类ID',
    cover_image VARCHAR(256) COMMENT '封面图URL',
    images JSON COMMENT '商品图片列表',
    status TINYINT DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    KEY idx_category_id (category_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- ========================================================
-- 5. 秒杀活动表 (t_seckill_activity)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_seckill_activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '活动ID',
    activity_name VARCHAR(128) NOT NULL COMMENT '活动名称',
    description VARCHAR(512) DEFAULT NULL COMMENT '活动描述',
    activity_img VARCHAR(256) DEFAULT NULL COMMENT '活动封面图URL',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束 3-已下架',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀活动表';

-- ========================================================
-- 6. 秒杀商品关联表 (t_seckill_goods)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_seckill_goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    activity_id BIGINT NOT NULL COMMENT '秒杀活动ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    seckill_price DECIMAL(10,2) NOT NULL COMMENT '秒杀价格',
    seckill_stock INT NOT NULL COMMENT '秒杀库存数量',
    limit_per_user INT DEFAULT 1 COMMENT '每人限购数量',
    sales_count INT DEFAULT 0 COMMENT '已售数量',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_activity_goods (activity_id, goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品关联表';

-- ========================================================
-- 7. 订单表 (t_order)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单编号（雪花算法）',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    activity_id BIGINT NOT NULL COMMENT '秒杀活动ID',
    goods_name VARCHAR(128) NOT NULL COMMENT '商品名称（下单时快照）',
    goods_image VARCHAR(256) COMMENT '商品封面图（下单时快照）',
    order_price DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    status TINYINT DEFAULT 1 COMMENT '状态：0-初始化 1-待支付 2-已支付 3-已发货 4-已完成 5-已取消 6-已退款',
    receiver_name VARCHAR(32) NOT NULL COMMENT '收货人姓名（下单时快照）',
    receiver_phone VARCHAR(11) NOT NULL COMMENT '收货人电话（下单时快照）',
    receiver_address VARCHAR(512) NOT NULL COMMENT '收货地址（下单时快照）',
    address_id BIGINT COMMENT '收货地址ID',
    pay_type TINYINT COMMENT '支付方式：1-余额 2-模拟支付宝 3-模拟微信',
    pay_time DATETIME COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ========================================================
-- 8. 秒杀记录表 (t_seckill_record)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_seckill_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    activity_id BIGINT NOT NULL COMMENT '秒杀活动ID',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    order_id BIGINT COMMENT '订单ID（秒杀成功后有值）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-排队中 1-成功 2-失败',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_activity_goods (user_id, activity_id, goods_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀记录表';

-- ========================================================
-- 9. 支付记录表 (t_pay_record)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_pay_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    pay_no VARCHAR(64) NOT NULL COMMENT '支付流水号',
    order_no VARCHAR(32) NOT NULL COMMENT '关联订单号',
    pay_method TINYINT NOT NULL COMMENT '支付方式：1-余额 2-模拟支付宝 3-模拟微信',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-支付失败 3-已退款',
    trade_no VARCHAR(64) COMMENT '第三方交易号',
    pay_time DATETIME COMMENT '支付时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_pay_no (pay_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';

-- ========================================================
-- 10. 管理员表 (t_admin)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '管理员ID',
    username VARCHAR(32) NOT NULL COMMENT '管理员账号',
    password VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(32) DEFAULT NULL COMMENT '真实姓名',
    role VARCHAR(20) NOT NULL COMMENT '角色：SUPER_ADMIN / ADMIN / OPERATOR',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ========================================================
-- 11. 管理员操作日志表 (t_admin_operation_log)
-- ========================================================
CREATE TABLE IF NOT EXISTS t_admin_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    admin_id BIGINT NOT NULL COMMENT '管理员ID',
    operation VARCHAR(64) NOT NULL COMMENT '操作类型',
    target_type VARCHAR(32) COMMENT '操作对象类型',
    target_id BIGINT COMMENT '操作对象ID',
    detail TEXT COMMENT '操作详情',
    ip VARCHAR(45) COMMENT '操作IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作日志表';

-- ========================================================
-- 初始化数据
-- ========================================================

-- 插入默认管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO t_admin (username, password, real_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'SUPER_ADMIN', 1);

-- 插入商品分类数据
INSERT INTO t_category (id, parent_id, name, sort, status) VALUES
(1, 0, '手机数码', 1, 1),
(2, 0, '电脑办公', 2, 1),
(3, 0, '家用电器', 3, 1),
(4, 0, '服装服饰', 4, 1),
(5, 0, '美妆护肤', 5, 1);

-- 插入二级分类
INSERT INTO t_category (id, parent_id, name, sort, status) VALUES
(11, 1, '手机', 1, 1),
(12, 1, '平板电脑', 2, 1),
(13, 1, '耳机音箱', 3, 1),
(21, 2, '笔记本', 1, 1),
(22, 2, '台式机', 2, 1),
(23, 2, '电脑配件', 3, 1),
(31, 3, '电视', 1, 1),
(32, 3, '空调', 2, 1),
(33, 3, '冰箱', 3, 1);

-- 插入测试商品数据
INSERT INTO t_goods (id, name, description, price, stock, category_id, cover_image, images, status) VALUES
(1, 'Apple iPhone 16 Pro Max 256GB', '全新 iPhone 16 Pro Max，搭载 A18 Pro 芯片，钛金属设计，超长续航。', 9999.00, 500, 11, 'https://cdn.seckill.com/goods/1001_main.jpg', '["https://cdn.seckill.com/goods/1001_1.jpg","https://cdn.seckill.com/goods/1001_2.jpg"]', 1),
(2, '华为 Mate 70 Pro 512GB', '华为旗舰手机，麒麟芯片，超感光徕卡影像系统。', 6999.00, 300, 11, 'https://cdn.seckill.com/goods/1002_main.jpg', '["https://cdn.seckill.com/goods/1002_1.jpg"]', 1),
(3, '小米 15 Ultra 256GB', '小米旗舰，骁龙8 Gen4，徕卡影像，120W快充。', 5999.00, 400, 11, 'https://cdn.seckill.com/goods/1003_main.jpg', '["https://cdn.seckill.com/goods/1003_1.jpg"]', 1),
(4, 'MacBook Pro 16英寸 M3 Pro', '苹果专业笔记本，M3 Pro芯片，18GB内存，512GB SSD。', 19999.00, 200, 21, 'https://cdn.seckill.com/goods/1004_main.jpg', '["https://cdn.seckill.com/goods/1004_1.jpg"]', 1),
(5, '联想拯救者 Y9000P 2024', '游戏本旗舰，i9-14900HX，RTX 4090，32GB内存。', 14999.00, 150, 21, 'https://cdn.seckill.com/goods/1005_main.jpg', '["https://cdn.seckill.com/goods/1005_1.jpg"]', 1);

-- 插入秒杀活动数据
INSERT INTO t_seckill_activity (id, activity_name, description, activity_img, start_time, end_time, status) VALUES
(1, 'iPhone 16 限时秒杀', 'iPhone 16 系列限时特惠，数量有限，先到先得！', 'https://cdn.seckill.com/seckill/2001_banner.jpg', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY INTERVAL 2 HOUR), 0),
(2, '华为手机专场', '华为 Mate 系列限时抢购，超值优惠！', 'https://cdn.seckill.com/seckill/2002_banner.jpg', DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY INTERVAL 2 HOUR), 0);

-- 插入秒杀商品关联数据
INSERT INTO t_seckill_goods (activity_id, goods_id, seckill_price, seckill_stock, limit_per_user, sales_count) VALUES
(1, 1, 7999.00, 100, 1, 0),
(2, 2, 5499.00, 200, 1, 0);
