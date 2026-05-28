CREATE TABLE dtp_hospital (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
province VARCHAR(20) DEFAULT NULL,              -- 省份
city VARCHAR(20) DEFAULT NULL,                  -- 城市
reporting_team VARCHAR(50) DEFAULT NULL,        -- 提报团队
district VARCHAR(20) DEFAULT NULL,              -- 区
hospital_name VARCHAR(100) DEFAULT NULL,        -- 申请的DTP药房 主要对应的医院名称
hospital_code VARCHAR(50) DEFAULT NULL,         -- 申请DTP主要对应的医院code
hospital_address VARCHAR(255) DEFAULT NULL,     -- 医院具体地址
location VARCHAR(100) DEFAULT NULL,             -- 医院所在经纬度
del_flag TINYINT DEFAULT 0,                     -- 删除状态 0正常 1已删除
create_by VARCHAR(32) DEFAULT NULL,             -- 创建人
create_time DATETIME DEFAULT NULL,              -- 创建时间
update_by VARCHAR(32) DEFAULT NULL,             -- 更新人
update_time DATETIME DEFAULT NULL,              -- 更新时间
image VARCHAR(255) DEFAULT NULL                 -- 图片
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;        -- 指定存储引擎和字符集

ALTER TABLE dtp_hospital COMMENT '医院表';
ALTER TABLE dtp_hospital MODIFY COLUMN id BIGINT AUTO_INCREMENT COMMENT '主键';
ALTER TABLE dtp_hospital MODIFY COLUMN province VARCHAR(20) DEFAULT NULL COMMENT '省份';
ALTER TABLE dtp_hospital MODIFY COLUMN city VARCHAR(20) DEFAULT NULL COMMENT '城市';
ALTER TABLE dtp_hospital MODIFY COLUMN reporting_team VARCHAR(50) DEFAULT NULL COMMENT '提报团队';
ALTER TABLE dtp_hospital MODIFY COLUMN district VARCHAR(20) DEFAULT NULL COMMENT '区';
ALTER TABLE dtp_hospital MODIFY COLUMN hospital_name VARCHAR(100) DEFAULT NULL COMMENT '申请的DTP药房 主要对应的医院名称';
ALTER TABLE dtp_hospital MODIFY COLUMN hospital_code VARCHAR(50) DEFAULT NULL COMMENT '申请DTP主要对应的医院code';
ALTER TABLE dtp_hospital MODIFY COLUMN hospital_address VARCHAR(255) DEFAULT NULL COMMENT '医院具体地址';
ALTER TABLE dtp_hospital MODIFY COLUMN location VARCHAR(100) DEFAULT NULL COMMENT '医院所在经纬度';
ALTER TABLE dtp_hospital MODIFY COLUMN del_flag TINYINT DEFAULT 0 COMMENT '删除状态 0正常 1已删除';
ALTER TABLE dtp_hospital MODIFY COLUMN create_by VARCHAR(32) DEFAULT NULL COMMENT '创建人';
ALTER TABLE dtp_hospital MODIFY COLUMN create_time DATETIME DEFAULT NULL COMMENT '创建时间';
ALTER TABLE dtp_hospital MODIFY COLUMN update_by VARCHAR(32) DEFAULT NULL COMMENT '更新人';
ALTER TABLE dtp_hospital MODIFY COLUMN update_time DATETIME DEFAULT NULL COMMENT '更新时间';
ALTER TABLE dtp_hospital MODIFY COLUMN image VARCHAR(255) DEFAULT NULL COMMENT '图片';

INSERT INTO dtp_hospital (
    province, city, reporting_team, district, hospital_name,
    hospital_code, hospital_address, location, del_flag,
    create_by, create_time, update_by, update_time, image
) VALUES
      ('上海', '上海市', '团队A', '黄浦区', '上海交通大学医学院附属仁济医院西院',
       'RJXY001', '上海市黄浦区重庆南路139号', '121.4934,31.2394', 0,
       'admin', '2025-03-21 15:30:55', 'admin', '2025-03-21 15:30:55',
       'http://example.com/image1.jpg'),
      ('上海', '上海市', '团队B', '黄浦区', '上海交通大学医学院附属瑞金医院',
       'RJYY002', '上海市黄浦区瑞金二路197号', '121.4934,31.2394', 0,
       'admin', '2025-03-21 15:31:00', 'admin', '2025-03-21 15:31:00',
       'http://example.com/image2.jpg');
