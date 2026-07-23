package com.company.intelligentdiagnosis.control.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/control/workflows")
public class WorkflowController {

    @GetMapping
    public ResponseEntity<List<WorkflowDto>> listWorkflows() {
        // TODO: 调用 application service
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDto> getWorkflow(@PathVariable String workflowId) {
        // TODO: 查询 Temporal 工作流状态
        return ResponseEntity.ok(null);
    }

    @PostMapping("/{workflowId}/pause")
    public ResponseEntity<Void> pause(@PathVariable String workflowId) {
        // TODO: 发送暂停信号
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/resume")
    public ResponseEntity<Void> resume(@PathVariable String workflowId) {
        // TODO: 发送恢复信号
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/rollback")
    public ResponseEntity<Void> rollback(@PathVariable String workflowId) {
        // TODO: 发送回滚信号
        return ResponseEntity.ok().build();
    }

    public record WorkflowDto(
        String workflowId,
        String workflowType,
        String status,
        String currentStep,
        String startedAt
    ) {}
}
