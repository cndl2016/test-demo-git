SELECT
pgc.conname AS 外键约束名,
cl.relname AS 主表名,
-- 主表字段按顺序拼接：a1|a2|a3
string_agg(col.attname, '|' ORDER BY array_position(pgc.conkey, col.attnum)) AS attname_link,
fcl.relname AS 引用表名,
-- 引用表字段按顺序拼接：b1|b2|b3
string_agg(fcol.attname, '|' ORDER BY array_position(pgc.confkey, fcol.attnum)) AS fattname_link,
CASE WHEN count(*) > 1 THEN '是' ELSE '否' END AS 是否联合外键
FROM pg_constraint pgc
JOIN pg_class cl     ON pgc.conrelid = cl.oid
JOIN pg_class fcl    ON pgc.confrelid = fcl.oid
JOIN pg_namespace ns ON cl.relnamespace = ns.oid
JOIN pg_attribute col ON col.attrelid = cl.oid AND col.attnum = ANY(pgc.conkey)
JOIN pg_attribute fcol ON fcol.attrelid = fcl.oid AND fcol.attnum = ANY(pgc.confkey)
WHERE
pgc.contype = 'f'  -- 外键
AND ns.nspname = 'your_schema'  -- 修改 schema
AND array_position(pgc.conkey, col.attnum) = array_position(pgc.confkey, fcol.attnum)
GROUP BY
pgc.conname, cl.relname, fcl.relname
ORDER BY
pgc.conname;