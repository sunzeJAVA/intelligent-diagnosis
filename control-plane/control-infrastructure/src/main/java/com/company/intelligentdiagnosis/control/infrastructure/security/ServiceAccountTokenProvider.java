package com.company.intelligentdiagnosis.control.infrastructure.security;

import com.company.intelligentdiagnosis.security.JwtTokenProvider;
import com.company.intelligentdiagnosis.security.SecurityUserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务账号 JWT Token 提供者
 * 用于控制平面调用数据平面内部接口时的认证
 */
@Component
public class ServiceAccountTokenProvider {

    private static final String SERVICE_ACCOUNT = "control-plane-service";

    private final JwtTokenProvider jwtTokenProvider;

    public ServiceAccountTokenProvider(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 生成服务账号 JWT Token
     * 每次调用生成新 token，避免过期问题
     *
     * @return Bearer Token
     */
    public String getToken() {
        SecurityUserDetails serviceUser = new SecurityUserDetails(
            SERVICE_ACCOUNT,
            "",
            List.of("ADMIN"),
            true
        );
        return jwtTokenProvider.generateToken(serviceUser);
    }
}
