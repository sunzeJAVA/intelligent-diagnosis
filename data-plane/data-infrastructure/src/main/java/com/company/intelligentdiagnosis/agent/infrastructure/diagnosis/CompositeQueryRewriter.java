package com.company.intelligentdiagnosis.agent.infrastructure.diagnosis;

import com.company.intelligentdiagnosis.agent.domain.diagnosis.DiagnosisIntent;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.QueryRewriter;
import com.company.intelligentdiagnosis.agent.domain.diagnosis.RewrittenQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 组合式 Query 重写器
 * <p>
 * 优先使用 {@link LlmQueryRewriter} 做语义增强；当 LLM 调用失败或解析异常时，
 * 自动降级到 {@link RuleBasedQueryRewriter}，保证检索链路不中断。
 * <p>
 * 该实现作为 {@link QueryRewriter} 的主 Bean 注入到应用服务中。
 */
@Primary
@Component
public class CompositeQueryRewriter implements QueryRewriter {

    private static final Logger log = LoggerFactory.getLogger(CompositeQueryRewriter.class);

    private final LlmQueryRewriter llmQueryRewriter;
    private final RuleBasedQueryRewriter ruleBasedQueryRewriter;

    public CompositeQueryRewriter(LlmQueryRewriter llmQueryRewriter,
                                  RuleBasedQueryRewriter ruleBasedQueryRewriter) {
        this.llmQueryRewriter = llmQueryRewriter;
        this.ruleBasedQueryRewriter = ruleBasedQueryRewriter;
    }

    @Override
    public RewrittenQuery rewrite(String originalQuery, DiagnosisIntent intent) {
        if (originalQuery == null || originalQuery.isBlank()) {
            return RewrittenQuery.identity("");
        }

        try {
            return llmQueryRewriter.rewrite(originalQuery, intent);
        } catch (Exception e) {
            log.warn("LLM query rewrite failed, falling back to rule-based rewrite: {}", e.getMessage());
            return ruleBasedQueryRewriter.rewrite(originalQuery, intent);
        }
    }
}
