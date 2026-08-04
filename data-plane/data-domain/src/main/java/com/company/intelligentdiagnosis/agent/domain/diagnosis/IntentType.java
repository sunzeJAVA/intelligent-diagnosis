package com.company.intelligentdiagnosis.agent.domain.diagnosis;

/**
 * 诊断意图类型
 * <p>
 * 对用户输入的错误信息/问题描述进行分类，用于优化检索策略和 LLM prompt。
 * 覆盖 Java/后端服务常见故障类别。
 */
public enum IntentType {

    /** 空指针异常 */
    NULL_POINTER("空指针异常", "NullPointerException 或对象为 null 导致的调用失败"),

    /** 数据库相关错误：连接失败、SQL 语法、约束冲突、死锁等 */
    DATABASE_ERROR("数据库错误", "SQL 异常、连接失败、约束冲突、死锁等"),

    /** 网络相关错误：超时、连接拒绝、SSL 等 */
    NETWORK_ERROR("网络错误", "连接超时、连接拒绝、SSL/TLS 异常等"),

    /** 并发问题：死锁、竞态条件、线程安全 */
    CONCURRENCY("并发问题", "死锁、竞态条件、线程安全问题"),

    /** 内存问题：OOM、内存泄漏、GC 相关 */
    MEMORY_ERROR("内存错误", "OutOfMemoryError、内存泄漏、GC 开销过大"),

    /** 配置错误：缺失配置、配置项无效、Bean 注入失败 */
    CONFIG_ERROR("配置错误", "配置缺失、Bean 注入失败、属性绑定异常"),

    /** 类加载/依赖问题：ClassNotFoundException、NoClassDefFoundError、版本冲突 */
    CLASSLOADING("类加载/依赖问题", "ClassNotFoundException、NoClassDefFoundError、版本冲突"),

    /** API 调用/参数错误：IllegalArgumentException、参数校验失败 */
    API_ERROR("API 调用错误", "参数校验失败、非法参数、类型转换异常"),

    /** 性能问题：响应慢、吞吐量低、资源耗尽 */
    PERFORMANCE("性能问题", "响应缓慢、吞吐量下降、资源耗尽"),

    /** 安全相关：认证失败、授权拒绝、SQL 注入等 */
    SECURITY("安全问题", "认证失败、授权拒绝、安全漏洞"),

    /** 通用/无法分类 */
    UNKNOWN("通用问题", "无法归类的通用问题");

    private final String displayName;
    private final String description;

    IntentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
