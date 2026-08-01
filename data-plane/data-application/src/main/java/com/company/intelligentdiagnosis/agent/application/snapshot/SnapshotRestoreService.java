package com.company.intelligentdiagnosis.agent.application.snapshot;

import com.company.intelligentdiagnosis.agent.domain.snapshot.IndexSnapshot;
import com.company.intelligentdiagnosis.agent.infrastructure.snapshot.SnapshotApplicationService;
import org.springframework.stereotype.Service;

/**
 * 快照回滚应用服务
 * 为 API 层提供物理快照恢复能力，屏蔽基础设施细节
 */
@Service
public class SnapshotRestoreService {

    private final SnapshotApplicationService snapshotApplicationService;

    public SnapshotRestoreService(SnapshotApplicationService snapshotApplicationService) {
        this.snapshotApplicationService = snapshotApplicationService;
    }

    /**
     * 将指定仓库的索引数据物理恢复到快照状态
     */
    public IndexSnapshot restoreSnapshot(String snapshotId) {
        return snapshotApplicationService.restoreSnapshot(snapshotId);
    }
}
