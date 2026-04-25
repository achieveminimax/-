-- ========================================================
-- M7 性能优化索引补充脚本
-- 基于查询模式分析，补充缺失索引
-- ========================================================

USE seckill_db;

-- t_goods: 商品名称前缀匹配查询（LIKE 'xxx%'），补充 name 索引
ALTER TABLE t_goods ADD INDEX idx_name (name);

-- t_seckill_activity: 按结束时间查询即将结束/未结束活动
ALTER TABLE t_seckill_activity ADD INDEX idx_end_time (end_time);

-- t_seckill_goods: 按 goods_id 排序查询活动列表，避免 filesort
ALTER TABLE t_seckill_goods ADD INDEX idx_goods_id_activity_id (goods_id, activity_id);

-- t_order: 按活动维度查询订单
ALTER TABLE t_order ADD INDEX idx_activity_id (activity_id);

-- t_pay_record: 按支付状态查询待处理记录
ALTER TABLE t_pay_record ADD INDEX idx_status (status);

-- t_category: 分类列表按 status+parent_id+sort 联合查询
ALTER TABLE t_category ADD INDEX idx_status_parent_sort (status, parent_id, sort);

-- t_admin_operation_log: 按操作类型筛选日志
ALTER TABLE t_admin_operation_log ADD INDEX idx_operation (operation);
