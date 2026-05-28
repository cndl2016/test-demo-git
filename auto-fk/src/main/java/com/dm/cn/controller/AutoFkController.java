package com.dm.cn.controller;

import com.dm.cn.model.FkMatchResult;
import com.dm.cn.service.AutoFkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/auto-fk")
@RequiredArgsConstructor
public class AutoFkController {

    private final AutoFkService autoFkService;

    /**
     * 执行Auto-FK识别隐性外键
     */
    @GetMapping("/execute")
    public List<FkMatchResult> execute() {
        return autoFkService.executeAutoFk();
    }
}