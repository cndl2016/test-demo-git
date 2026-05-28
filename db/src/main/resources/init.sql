-- 创建schema
CREATE SCHEMA IF NOT EXISTS school_schema;
SET search_path TO school_schema;

-- 创建学校表
CREATE TABLE school (
id SERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
address VARCHAR(255),
established_year INT,
principal VARCHAR(100),
contact_phone VARCHAR(20)
);
COMMENT ON TABLE school IS '学校基本信息表，存储所有学校的基础数据';
COMMENT ON COLUMN school.id IS '学校唯一标识，自增主键';
COMMENT ON COLUMN school.name IS '学校名称，不能为空';
COMMENT ON COLUMN school.address IS '学校地址';
COMMENT ON COLUMN school.established_year IS '学校创办年份';
COMMENT ON COLUMN school.principal IS '校长姓名';
COMMENT ON COLUMN school.contact_phone IS '学校联系电话';

-- 创建老师表
CREATE TABLE teacher (
id SERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
gender VARCHAR(10),
age INT,
subject VARCHAR(100),
hire_date DATE,
school_id INT REFERENCES school(id)
);
COMMENT ON TABLE teacher IS '教师信息表，存储所有教师的基本信息及所属学校';
COMMENT ON COLUMN teacher.id IS '教师唯一标识，自增主键';
COMMENT ON COLUMN teacher.name IS '教师姓名，不能为空';
COMMENT ON COLUMN teacher.gender IS '教师性别（男/女）';
COMMENT ON COLUMN teacher.age IS '教师年龄';
COMMENT ON COLUMN teacher.subject IS '教师教授科目';
COMMENT ON COLUMN teacher.hire_date IS '教师入职日期';
COMMENT ON COLUMN teacher.school_id IS '所属学校ID，关联school表的id字段';

-- 创建班级表
CREATE TABLE class (
id SERIAL PRIMARY KEY,
name VARCHAR(50) NOT NULL,
grade INT NOT NULL,
student_count INT DEFAULT 0,
head_teacher_id INT REFERENCES teacher(id),
school_id INT REFERENCES school(id)
);
COMMENT ON TABLE class IS '班级信息表，存储班级基本信息、班主任及所属学校';
COMMENT ON COLUMN class.id IS '班级唯一标识，自增主键';
COMMENT ON COLUMN class.name IS '班级名称（如：高一(1)班），不能为空';
COMMENT ON COLUMN class.grade IS '年级（如：1表示高一，2表示高二），不能为空';
COMMENT ON COLUMN class.student_count IS '班级学生数量，默认值为0';
COMMENT ON COLUMN class.head_teacher_id IS '班主任ID，关联teacher表的id字段';
COMMENT ON COLUMN class.school_id IS '所属学校ID，关联school表的id字段';

-- 创建学生表
CREATE TABLE student (
id SERIAL PRIMARY KEY,
name VARCHAR(100) NOT NULL,
gender VARCHAR(10),
age INT,
enrollment_date DATE,
address VARCHAR(255),
class_id INT REFERENCES class(id),
school_id INT REFERENCES school(id)
);
COMMENT ON TABLE student IS '学生信息表，存储学生基本信息、所属班级及学校';
COMMENT ON COLUMN student.id IS '学生唯一标识，自增主键';
COMMENT ON COLUMN student.name IS '学生姓名，不能为空';
COMMENT ON COLUMN student.gender IS '学生性别（男/女）';
COMMENT ON COLUMN student.age IS '学生年龄';
COMMENT ON COLUMN student.enrollment_date IS '入学日期';
COMMENT ON COLUMN student.address IS '学生住址';
COMMENT ON COLUMN student.class_id IS '所属班级ID，关联class表的id字段';
COMMENT ON COLUMN student.school_id IS '所属学校ID，关联school表的id字段';

-- 插入测试学校数据
INSERT INTO school (name, address, established_year, principal, contact_phone) VALUES
('阳光第一中学', '北京市海淀区阳光路88号', 1985, '张明', '010-12345678'),
('希望小学', '上海市浦东新区希望街123号', 1992, '李华', '021-87654321'),
('未来实验学校', '广州市天河区未来大道45号', 2005, '王静', '020-56781234');

-- 插入测试老师数据
INSERT INTO teacher (name, gender, age, subject, hire_date, school_id) VALUES
('赵老师', '女', 35, '语文', '2010-09-01', 1),
('钱老师', '男', 42, '数学', '2005-09-01', 1),
('孙老师', '女', 28, '英语', '2018-09-01', 1),
('李老师', '男', 38, '语文', '2012-09-01', 2),
('周老师', '女', 31, '数学', '2015-09-01', 2),
('吴老师', '男', 45, '科学', '2000-09-01', 3),
('郑老师', '女', 33, '英语', '2014-09-01', 3);

-- 插入测试班级数据
INSERT INTO class (name, grade, student_count, head_teacher_id, school_id) VALUES
('高一(1)班', 10, 45, 1, 1),
('高一(2)班', 10, 42, 2, 1),
('初一(1)班', 7, 38, 4, 2),
('初一(2)班', 7, 40, 5, 2),
('实验一班', 9, 35, 6, 3),
('实验二班', 9, 37, 7, 3);

-- 插入测试学生数据
INSERT INTO student (name, gender, age, enrollment_date, address, class_id, school_id) VALUES
-- 阳光第一中学 高一(1)班学生
('张三', '男', 16, '2023-09-01', '海淀区幸福路1号', 1, 1),
('李四', '女', 15, '2023-09-01', '海淀区幸福路2号', 1, 1),
('王五', '男', 16, '2023-09-01', '海淀区幸福路3号', 1, 1),
('赵六', '女', 16, '2023-09-01', '海淀区幸福路4号', 1, 1),

-- 阳光第一中学 高一(2)班学生
('孙七', '男', 16, '2023-09-01', '海淀区快乐路1号', 2, 1),
('周八', '女', 15, '2023-09-01', '海淀区快乐路2号', 2, 1),
('吴九', '男', 16, '2023-09-01', '海淀区快乐路3号', 2, 1),

-- 希望小学 初一(1)班学生
('郑十', '女', 13, '2023-09-01', '浦东新区平安路1号', 3, 2),
('王十一', '男', 13, '2023-09-01', '浦东新区平安路2号', 3, 2),
('冯十二', '女', 12, '2023-09-01', '浦东新区平安路3号', 3, 2),

-- 希望小学 初一(2)班学生
('陈十三', '男', 13, '2023-09-01', '浦东新区健康路1号', 4, 2),
('褚十四', '女', 13, '2023-09-01', '浦东新区健康路2号', 4, 2),

-- 未来实验学校 实验一班学生
('卫十五', '男', 15, '2023-09-01', '天河区创新路1号', 5, 3),
('蒋十六', '女', 15, '2023-09-01', '天河区创新路2号', 5, 3),

-- 未来实验学校 实验二班学生
('沈十七', '男', 15, '2023-09-01', '天河区科技路1号', 6, 3),
('韩十八', '女', 14, '2023-09-01', '天河区科技路2号', 6, 3);

-- 创建视图：学校-班级-老师关联视图
-- 包含学校、班级的基本信息以及对应的班主任信息
CREATE OR REPLACE VIEW teacher_view AS
SELECT
    s.id AS school_id,
    s.name AS school_name,
    s.address AS school_address,
    s.established_year,
    c.id AS class_id,
    c.name AS class_name,
    c.grade,
    c.student_count,
    t.id AS teacher_id,
    t.name AS teacher_name,
    t.gender AS teacher_gender,
    t.age AS teacher_age,
    t.subject AS teacher_subject,
    t.hire_date AS teacher_hire_date
FROM
    school s
        JOIN
    class c ON s.id = c.school_id
        JOIN
    teacher t ON c.head_teacher_id = t.id;

-- 创建视图：学校-班级-老师-学生关联视图
-- 包含完整的学校、班级、班主任和学生信息
CREATE OR REPLACE VIEW student_view AS
SELECT
    s.id AS school_id,
    s.name AS school_name,
    c.id AS class_id,
    c.name AS class_name,
    c.grade,
    t.id AS teacher_id,
    t.name AS teacher_name,
    t.subject AS teacher_subject,
    st.id AS student_id,
    st.name AS student_name,
    st.gender AS student_gender,
    st.age AS student_age,
    st.enrollment_date,
    st.address AS student_address
FROM
    school s
        JOIN
    class c ON s.id = c.school_id
        JOIN
    teacher t ON c.head_teacher_id = t.id
        JOIN
    student st ON c.id = st.class_id;
