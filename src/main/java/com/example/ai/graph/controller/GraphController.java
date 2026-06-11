package com.example.ai.graph.controller;

import com.example.ai.graph.service.GraphWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/graph")
public class GraphController {

    private final GraphWorkflowService graphService;

    public GraphController(GraphWorkflowService graphService) {
        this.graphService = graphService;
    }

    /**
     * 执行 Graph 工作流 — POST /api/graph/execute
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute(@RequestBody Map<String, String> request) {
        String input = request.get("input");
        if (input == null || input.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "input 不能为空"));
        }

        long startTime = System.currentTimeMillis();
        Map<String, Object> result = graphService.execute(input);
        long totalDuration = System.currentTimeMillis() - startTime;

        String output = (String) result.get("output");
        String category = (String) result.get("category");

        return ResponseEntity.ok(Map.of(
                "output", output,
                "branch", category,
                "durationMs", totalDuration
        ));
    }
}
