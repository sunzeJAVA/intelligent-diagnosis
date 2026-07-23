package com.company.intelligentdiagnosis.control.application;

import org.springframework.stereotype.Service;

@Service
public class ApprovalApplicationService {

    // TODO: 注入 Temporal Client、Audit Service、Policy Engine

    public void approve(String workflowId, String approver, String comment) {
        // TODO: 发送 approve signal 到 Temporal workflow
    }

    public void reject(String workflowId, String reason) {
        // TODO: 发送 reject signal 到 Temporal workflow
    }
}
