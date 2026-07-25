package com.company.intelligentdiagnosis.agent.api;

/**
 * API 统一响应封装
 *
 * @param <T> 响应数据类型
 */
public record ApiResponse<T>(
    /**
     * 是否成功
     */
    boolean success,

    /**
     * 响应数据
     */
    T data,

    /**
     * 错误信息
     */
    String error
) {

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 创建错误响应
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 错误响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
