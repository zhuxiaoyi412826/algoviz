-- 商品表 product.material_url（资料下载链接）字段补列脚本
-- 幂等：列已存在时仅执行 SELECT 1，不会重复建列/破坏数据；每次启动由 DatabaseInitializer 执行
SET @algoviz_col = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'material_url'),
    'SELECT 1',
    'ALTER TABLE `product` ADD COLUMN `material_url` VARCHAR(500) NULL DEFAULT NULL COMMENT ''购买后资料下载链接（随订单邮件发放）''');
PREPARE algoviz_material_stmt FROM @algoviz_col;
EXECUTE algoviz_material_stmt;
DEALLOCATE PREPARE algoviz_material_stmt;
