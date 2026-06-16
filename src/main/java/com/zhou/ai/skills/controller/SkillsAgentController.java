package com.zhou.ai.skills.controller;

import com.zhou.ai.skills.service.SkillsAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Skills Agent REST 端点 — 演示 Spring AI Alibaba Skills 渐进式披露功能。
 */
@Tag(name = "Skills Agent", description = "Spring AI Alibaba Skills 演示")
@RestController
@RequestMapping("/api/skills")
public class SkillsAgentController {

    private final SkillsAgentService skillsAgentService;

    public SkillsAgentController(SkillsAgentService skillsAgentService) {
        this.skillsAgentService = skillsAgentService;
    }

    @Operation(summary = "Skills Agent 对话", description = "与 Skills Agent 对话，支持多轮会话")
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "message 不能为空"));
        }
        String threadId = request.get("threadId");
        if (threadId == null || threadId.isBlank()) {
            threadId = UUID.randomUUID().toString();
        }
        long startTime = System.currentTimeMillis();
        String reply = skillsAgentService.chat(message, threadId);
        long duration = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "threadId", threadId,
                "durationMs", duration
        ));
    }

    @Operation(summary = "列出可用技能", description = "查看所有已注册的 Skills")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listSkills() {
        return ResponseEntity.ok(skillsAgentService.getDiagnostics());
    }
}
