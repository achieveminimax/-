-- ========================================================
-- 电商秒杀系统数据库初始化脚本
-- 数据库: seckill_db
-- 字符集: utf8mb4
-- 版本: 1.0.0
-- ========================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS seckill_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE seckill_db;

-- ========================================================
-- 1. 用户表 (t_user)
-- ========================================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username        VARCHAR(32)     NOT NULL COMMENT '用户名',
    password        VARCHAR(128)    NOT NULL COMMENT '密码（BCrypt加密）',
    nickname        VARCHAR(32)     NULL COMMENT '昵称',
    phone           VARCHAR(11)     NOT NULL COMMENT '手机号',
    avatar          VARCHAR(256)    NULL COMMENT '头像URL',
    gender          TINYINT         DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    email           VARCHAR(64)     NULL COMMENT '邮箱',
    birthday        DATE            NULL COMMENT '生日',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    login_fail_count INT            DEFAULT 0 COMMENT '连续登录失败次数',
    lock_time       DATETIME        NULL COMMENT '账号锁定时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ========================================================
-- 2. 收货地址表 (t_address)
-- ========================================================
DROP TABLE IF EXISTS t_address;
CREATE TABLE t_address (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    receiver_name   VARCHAR(32)     NOT NULL COMMENT '收货人姓名',
    receiver_phone  VARCHAR(11)     NOT NULL COMMENT '收货人电话',
    province        VARCHAR(32)     NOT NULL COMMENT '省',
    city            VARCHAR(32)     NOT NULL COMMENT '市',
    district        VARCHAR(32)     NOT NULL COMMENT '区',
    detail_address  VARCHAR(256)    NOT NULL COMMENT '详细地址',
    is_default      TINYINT         DEFAULT 0 COMMENT '是否默认：0-否 1-是',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- ========================================================
-- 3. 商品分类表 (t_category)
-- ========================================================
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    parent_id       BIGINT          DEFAULT 0 COMMENT '父分类ID（0为一级分类）',
    name            VARCHAR(32)     NOT NULL COMMENT '分类名称',
    icon            VARCHAR(256)    NULL COMMENT '分类图标URL',
    sort            INT             DEFAULT 0 COMMENT '排序序号',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id),
    KEY idx_status (status),
    KEY idx_sort (sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ========================================================
-- 4. 商品表 (t_goods)
-- ========================================================
DROP TABLE IF EXISTS t_goods;
CREATE TABLE t_goods (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name            VARCHAR(128)    NOT NULL COMMENT '商品名称',
    description     TEXT            NULL COMMENT '商品描述',
    price           DECIMAL(10,2)   NOT NULL COMMENT '商品原价',
    stock           INT             NOT NULL DEFAULT 0 COMMENT '总库存',
    category_id     BIGINT          NULL COMMENT '分类ID',
    cover_image     VARCHAR(256)    NULL COMMENT '封面图URL',
    images          JSON            NULL COMMENT '商品图片列表',
    detail          LONGTEXT        NULL COMMENT '商品详情（富文本）',
    sales           INT             DEFAULT 0 COMMENT '销量',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0-下架 1-上架',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_category_id (category_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ========================================================
-- 5. 秒杀活动表 (t_seckill_activity)
-- ========================================================
DROP TABLE IF EXISTS t_seckill_activity;
CREATE TABLE t_seckill_activity (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    activity_name   VARCHAR(128)    NOT NULL COMMENT '活动名称',
    description     VARCHAR(512)    NULL COMMENT '活动描述',
    activity_img    VARCHAR(256)    NULL COMMENT '活动封面图URL',
    start_time      DATETIME        NOT NULL COMMENT '开始时间',
    end_time        DATETIME        NOT NULL COMMENT '结束时间',
    status          TINYINT         DEFAULT 0 COMMENT '状态：0-未开始 1-进行中 2-已结束 3-已下架',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_status_time (status, start_time, end_time),
    KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀活动表';

-- ========================================================
-- 6. 秒杀商品关联表 (t_seckill_goods)
-- ========================================================
DROP TABLE IF EXISTS t_seckill_goods;
CREATE TABLE t_seckill_goods (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    activity_id     BIGINT          NOT NULL COMMENT '秒杀活动ID',
    goods_id        BIGINT          NOT NULL COMMENT '商品ID',
    seckill_price   DECIMAL(10,2)   NOT NULL COMMENT '秒杀价格',
    seckill_stock   INT             NOT NULL COMMENT '秒杀库存数量',
    limit_per_user  INT             DEFAULT 1 COMMENT '每人限购数量',
    sales_count     INT             DEFAULT 0 COMMENT '已售数量',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activity_goods (activity_id, goods_id),
    KEY idx_activity_id (activity_id),
    KEY idx_goods_id (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀商品关联表';

-- ========================================================
-- 7. 订单表 (t_order)
-- ========================================================
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    order_no        VARCHAR(32)     NOT NULL COMMENT '订单编号（雪花算法）',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    goods_id        BIGINT          NOT NULL COMMENT '商品ID',
    activity_id     BIGINT          NOT NULL COMMENT '秒杀活动ID',
    goods_name      VARCHAR(128)    NOT NULL COMMENT '商品名称（下单时快照）',
    goods_image     VARCHAR(256)    NULL COMMENT '商品封面图（下单时快照）',
    order_price     DECIMAL(10,2)   NOT NULL COMMENT '订单金额',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '购买数量',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0-初始化 1-待支付 2-已支付 3-已发货 4-已完成 5-已取消 6-已退款',
    receiver_name   VARCHAR(32)     NOT NULL COMMENT '收货人姓名（下单时快照）',
    receiver_phone  VARCHAR(11)     NOT NULL COMMENT '收货人电话（下单时快照）',
    receiver_address VARCHAR(512)   NOT NULL COMMENT '收货地址（下单时快照）',
    address_id      BIGINT          NULL COMMENT '收货地址ID',
    pay_type        TINYINT         NULL COMMENT '支付方式：1-余额 2-模拟支付宝 3-模拟微信',
    pay_time        DATETIME        NULL COMMENT '支付时间',
    express_company VARCHAR(50)     NULL COMMENT '快递公司',
    express_no      VARCHAR(50)     NULL COMMENT '快递单号',
    ship_time       DATETIME        NULL COMMENT '发货时间',
    receive_time    DATETIME        NULL COMMENT '收货时间',
    cancel_time     DATETIME        NULL COMMENT '取消时间',
    cancel_reason   VARCHAR(255)    NULL COMMENT '取消原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted         TINYINT         DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_user_id (user_id),
    KEY idx_user_status (user_id, status),
    KEY idx_create_time (create_time),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ========================================================
-- 8. 秒杀记录表 (t_seckill_record)
-- ========================================================
DROP TABLE IF EXISTS t_seckill_record;
CREATE TABLE t_seckill_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    activity_id     BIGINT          NOT NULL COMMENT '秒杀活动ID',
    goods_id        BIGINT          NOT NULL COMMENT '商品ID',
    order_id        BIGINT          NULL COMMENT '订单ID（秒杀成功后有值）',
    status          TINYINT         DEFAULT 0 COMMENT '状态：0-排队中 1-成功 2-失败',
    fail_reason     VARCHAR(255)    NULL COMMENT '失败原因',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finish_time     DATETIME        NULL COMMENT '完成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_activity_goods (user_id, activity_id, goods_id),
    KEY idx_user_id (user_id),
    KEY idx_activity_id (activity_id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='秒杀记录表';

-- ========================================================
-- 9. 支付记录表 (t_pay_record)
-- ========================================================
DROP TABLE IF EXISTS t_pay_record;
CREATE TABLE t_pay_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pay_no          VARCHAR(64)     NOT NULL COMMENT '支付流水号',
    order_no        VARCHAR(32)     NOT NULL COMMENT '关联订单号',
    user_id         BIGINT          NOT NULL COMMENT '用户ID',
    pay_method      TINYINT         NOT NULL COMMENT '支付方式：1-余额 2-模拟支付宝 3-模拟微信',
    pay_amount      DECIMAL(10,2)   NOT NULL COMMENT '支付金额',
    status          TINYINT         DEFAULT 0 COMMENT '状态：0-待支付 1-已支付 2-支付失败 3-已退款',
    trade_no        VARCHAR(64)     NULL COMMENT '第三方交易号',
    pay_time        DATETIME        NULL COMMENT '支付时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pay_no (pay_no),
    KEY idx_order_no (order_no),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ========================================================
-- 10. 管理员表 (t_admin)
-- ========================================================
DROP TABLE IF EXISTS t_admin;
CREATE TABLE t_admin (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    username        VARCHAR(32)     NOT NULL COMMENT '管理员账号',
    password        VARCHAR(128)    NOT NULL COMMENT '密码（BCrypt加密）',
    real_name       VARCHAR(32)     NULL COMMENT '真实姓名',
    role            VARCHAR(20)     NOT NULL COMMENT '角色：SUPER_ADMIN / ADMIN / OPERATOR',
    status          TINYINT         DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    last_login_time DATETIME        NULL COMMENT '最后登录时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员表';

-- ========================================================
-- 11. 管理员操作日志表 (t_admin_operation_log)
-- ========================================================
DROP TABLE IF EXISTS t_admin_operation_log;
CREATE TABLE t_admin_operation_log (
    id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    admin_id        BIGINT          NOT NULL COMMENT '管理员ID',
    operation       VARCHAR(64)     NOT NULL COMMENT '操作类型',
    target_type     VARCHAR(32)     NULL COMMENT '操作对象类型',
    target_id       BIGINT          NULL COMMENT '操作对象ID',
    detail          TEXT            NULL COMMENT '操作详情',
    ip              VARCHAR(45)     NULL COMMENT '操作IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_admin_id (admin_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作日志表';

-- ========================================================
-- 初始化数据
-- ========================================================

-- 初始化管理员账号（密码: admin123）
INSERT INTO t_admin (username, password, real_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'SUPER_ADMIN', 1);

-- 初始化商品分类
INSERT INTO t_category (id, parent_id, name, icon, sort, status) VALUES
(1, 0, '手机数码', 'https://cdn.seckill.com/category/phone.png', 1, 1),
(2, 0, '电脑办公', 'https://cdn.seckill.com/category/computer.png', 2, 1),
(3, 0, '家用电器', 'https://cdn.seckill.com/category/appliance.png', 3, 1),
(4, 0, '服装服饰', 'https://cdn.seckill.com/category/clothing.png', 4, 1),
(5, 0, '美妆护肤', 'https://cdn.seckill.com/category/beauty.png', 5, 1);

-- 初始化二级分类
INSERT INTO t_category (id, parent_id, name, icon, sort, status) VALUES
(11, 1, '手机', 'https://cdn.seckill.com/category/smartphone.png', 1, 1),
(12, 1, '平板电脑', 'https://cdn.seckill.com/category/tablet.png', 2, 1),
(13, 1, '耳机音箱', 'https://cdn.seckill.com/category/earphone.png', 3, 1),
(21, 2, '笔记本', 'https://cdn.seckill.com/category/laptop.png', 1, 1),
(22, 2, '台式机', 'https://cdn.seckill.com/category/desktop.png', 2, 1),
(23, 2, '电脑配件', 'https://cdn.seckill.com/category/accessories.png', 3, 1),
(31, 3, '电视', 'https://cdn.seckill.com/category/tv.png', 1, 1),
(32, 3, '空调', 'https://cdn.seckill.com/category/aircon.png', 2, 1),
(33, 3, '冰箱', 'https://cdn.seckill.com/category/fridge.png', 3, 1);

-- 初始化商品数据
INSERT INTO t_goods (id, name, description, price, stock, category_id, cover_image, images, status) VALUES
(1, 'Apple iPhone 16 Pro Max 256GB', '全新 iPhone 16 Pro Max，搭载 A18 Pro 芯片，钛金属设计，超长续航。', 9999.00, 500, 11, 'https://cdn.seckill.com/goods/1001_main.jpg', '["https://cdn.seckill.com/goods/1001_1.jpg","https://cdn.seckill.com/goods/1001_2.jpg"]', 1),
(2, '华为 Mate 70 Pro 512GB', '华为旗舰手机，麒麟芯片，超感光徕卡影像系统。', 6999.00, 300, 11, 'https://cdn.seckill.com/goods/1002_main.jpg', '["https://cdn.seckill.com/goods/1002_1.jpg","https://cdn.seckill.com/goods/1002_2.jpg"]', 1),
(3, '小米 15 Ultra 256GB', '小米顶级旗舰，骁龙8 Gen4处理器，徕卡影像。', 5999.00, 400, 11, 'https://cdn.seckill.com/goods/1003_main.jpg', '["https://cdn.seckill.com/goods/1003_1.jpg","https://cdn.seckill.com/goods/1003_2.jpg"]', 1),
(4, 'MacBook Pro 16英寸 M3 Pro', '苹果专业级笔记本，M3 Pro芯片，极致性能。', 19999.00, 200, 21, 'https://cdn.seckill.com/goods/1004_main.jpg', '["https://cdn.seckill.com/goods/1004_1.jpg"]', 1),
(5, 'iPad Pro 12.9英寸 M2', '苹果专业级平板，M2芯片，生产力工具。', 8999.00, 300, 12, 'https://cdn.seckill.com/goods/1005_main.jpg', '["https://cdn.seckill.com/goods/1005_1.jpg"]', 1);
