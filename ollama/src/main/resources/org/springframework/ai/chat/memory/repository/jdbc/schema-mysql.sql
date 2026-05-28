
-- 需要自己执行初始化SQL脚本，字段名称和类型不可修改
CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    id INT PRIMARY KEY AUTO_INCREMENT,
    conversation_id VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);