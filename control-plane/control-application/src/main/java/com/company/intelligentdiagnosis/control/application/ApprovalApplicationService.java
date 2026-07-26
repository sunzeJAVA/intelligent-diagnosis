package com.company.intelligentdiagnosis.control.application;

import com.company.intelligentdiagnosis.control.domain.workflow.WorkflowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApprovalApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalApplicationService.class);

    private final WorkflowService workflowService;

    public ApprovalApplicationService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public void approve(String workflowId, String approver, String comment) {
        log.info("Approving workflow {} by {}: {}", workflowId, approver, comment);
        workflowService.approveWorkflow(workflowId, approver, comment);
    }

    public void reject(String workflowId, String reason) {
        log.info("Rejecting workflow {}: {}", workflowId, reason);
        workflowService.rejectWorkflow(workflowId, reason);
    }
}
