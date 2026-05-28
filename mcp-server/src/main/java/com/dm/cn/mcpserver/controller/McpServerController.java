package com.dm.cn.mcpserver.controller;

import com.dm.cn.mcpserver.service.StockService;
import com.dm.cn.mcpserver.service.WeatherService;
import com.dm.cn.mcpserver.utils.MathTools;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/mcp/server")
public class McpServerController {

    @Autowired
    McpSyncServer mcpSyncServer;

    /**
     * 展示可用工具列表
     */
    @GetMapping("/showAvailableTools")
    public String showAvailableTools() {
        StringBuffer sb = new StringBuffer();
        sb.append("Available Tools:\n");

        sb.append("mathTool:\n");
        ToolCallback[] tools = ToolCallbacks.from(new MathTools());
        for (ToolCallback tool : tools) {
            ToolDefinition definition = tool.getToolDefinition();
            sb.append(MessageFormat.format("{0} -> {1}\n", definition.name(), definition.description()));
        }
        sb.append("\n");

        sb.append("stockTool:\n");
        tools = ToolCallbacks.from(new StockService());
        for (ToolCallback tool : tools) {
            ToolDefinition definition = tool.getToolDefinition();
            sb.append(MessageFormat.format("{0} -> {1}\n", definition.name(), definition.description()));
        }
        sb.append("\n");

        sb.append("weatherTool:\n");
        tools = ToolCallbacks.from(new WeatherService());
        for (ToolCallback tool : tools) {
            ToolDefinition definition = tool.getToolDefinition();
            sb.append(MessageFormat.format("{0} -> {1}\n", definition.name(), definition.description()));
        }

        return sb.toString();
    }

    /**
     * 添加工具
     */
    @GetMapping("/addToolByClassSync")
    public String addToolByClassSync(@RequestParam(value = "className") String className) {
        List<McpServerFeatures.SyncToolSpecification> newTools = new ArrayList<>();
        if("mathTool".equals(className)) {
            newTools = McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(new MathTools()));
        } else if("stockTool".equals(className)) {
            newTools = McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(new StockService()));
        } else if("weatherTool".equals(className)) {
            newTools = McpToolUtils.toSyncToolSpecifications(ToolCallbacks.from(new WeatherService()));
        }
        for (McpServerFeatures.SyncToolSpecification newTool : newTools) {
            mcpSyncServer.addTool(newTool);
        }
        return className + "添加成功";
    }

    /**
     * 移除工具
     */
    @GetMapping("/removeToolSync")
    public String removeToolSync(@RequestParam(value = "toolName") String toolName) {
        mcpSyncServer.removeTool(toolName);
        return toolName + "移除成功";
    }
}
