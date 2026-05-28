package com.dm.cn.service;

import com.dm.cn.model.SqlQuery;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spider测试集评估服务（实现官方评估逻辑）
 */
@Service
public class SpiderTestService {
    // 固定根路径
    private static final String BASE_PATH = "D:\\project\\python\\spider_data\\";
    // 核心缓存
    private final HashMap<String, String> schemaCache = new HashMap<>();

    public List<SqlQuery> getSqlResultByClient(ChatClient chatClient) {
        var promptTemplateStr = """
                你是一个专业自然语言转SQL生成助手。
                
                输出要求：仅返回可直接执行的标准SQL，每个SQL结果占一行，不要任务换行、回车，不要任何说明、注释、引号包裹。
                
                数据库schema信息：
                %s
                """;
        var sqlQueryList = parseJsonToSqlQueryList().subList(0, 10);

        try (PrintWriter writer = new PrintWriter(new FileWriter("predicted.txt", false))) {
            sqlQueryList.forEach(sqlQuery -> {
                var question = sqlQuery.getQuestion();
                var schemaInfo = readSchemaByDbId(sqlQuery.getDb_id());
                var prompt = String.format(promptTemplateStr, schemaInfo);
                System.out.println(prompt);

                var answer = chatClient.prompt()
                        .system(prompt)
                        .user(question)
                        .call()
                        .content();
                writer.println(answer);
                sqlQuery.setAnswer(answer);
                System.out.println(answer + "  |  " + sqlQuery.getQuery());
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return sqlQueryList;
    }

    public List<SqlQuery> getSqlResultByModel(ChatModel chatModel) {
        var promptTemplateStr = """
                你是一个专业自然语言转SQL生成助手。
                
                输出要求：仅返回可直接执行的标准SQL，不要任何说明、注释、引号包裹。
                
                数据库schema信息：
                %s
                """;
        var sqlQueryList = parseJsonToSqlQueryList().subList(0, 1);

        sqlQueryList.forEach(sqlQuery -> {
            var schemaInfo = readSchemaByDbId(sqlQuery.getDb_id());
            Message systemMessage = new SystemMessage(String.format(promptTemplateStr, schemaInfo));
            Message userMessage = new UserMessage(sqlQuery.getQuestion());
            System.out.println(systemMessage);
            System.out.println(userMessage);

            ChatResponse response = chatModel.call(
                    new Prompt(
                            List.of(systemMessage, userMessage),
//                            OllamaChatOptions.builder()
//                                    .disableThinking()
                            OpenAiChatOptions.builder()
                                    .build()
                    ));
            String answer = response.getResult().getOutput().getText();
            sqlQuery.setAnswer(answer);
            System.out.println(answer + "  |  " + sqlQuery.getQuery());
        });
        return sqlQueryList;
    }

    public List<SqlQuery> parseJsonToSqlQueryList() {
        try {
            String filePath = BASE_PATH + "dev.json";
            File jsonFile = new File(filePath);

            ObjectMapper objectMapper = new ObjectMapper();
            List<SqlQuery> sqlQueryList = objectMapper.readValue(jsonFile, new TypeReference<List<SqlQuery>>() {
            });

            return sqlQueryList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 根据 db_id 读取 schema.sql（带缓存，优先读内存）
     *
     * @param dbId 数据库ID
     * @return 表结构内容
     */
    public String readSchemaByDbId(String dbId) {
        // 1. 优先从缓存获取（存在直接返回）
        if (schemaCache.containsKey(dbId)) {
            return schemaCache.get(dbId);
        }
        // 2. 缓存不存在 读取文件
        try {
            String schemaContent = "";
            String schemaPath = BASE_PATH + "database\\" + dbId + "\\schema.sql";
            if (Files.exists(Paths.get(schemaPath))) {
                schemaContent = Files.readString(Paths.get(schemaPath));
            } else {
                schemaPath = BASE_PATH + "database\\" + dbId + "\\" + dbId + ".sql";
                if (Files.exists(Paths.get(schemaPath))) {
                    schemaContent = Files.readString(Paths.get(schemaPath));
                }
            }
            // 3. 核心：只保留建表语句，剔除 INSERT 语句
            schemaContent = filterCreateTableOnly(schemaContent);
            // 4. 读取后存入缓存（下次直接用）
            schemaCache.put(dbId, schemaContent);
            return schemaContent;
        } catch (Exception e) {
            System.err.println("读取schema失败，db_id：" + dbId + "，错误：" + e.getMessage());
            return "";
        }
    }

    /**
     * 过滤SQL文本，只保留 CREATE TABLE 建表语句，删除 INSERT 等其他语句
     */
    private String filterCreateTableOnly(String sqlContent) {
        if (sqlContent == null || sqlContent.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        // 正则匹配 CREATE TABLE 语句（跨多行、匹配到 ; 结束）
        Pattern pattern = Pattern.compile("CREATE TABLE[^;]+;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(sqlContent);

        // 把所有建表语句追加到结果
        while (matcher.find()) {
            result.append(matcher.group()).append("\n\n");
        }

        return result.toString().trim();
    }
}