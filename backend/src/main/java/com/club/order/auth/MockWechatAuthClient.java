package com.club.order.auth;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(name = "app.wechat.enabled", havingValue = "false", matchIfMissing = true)
public class MockWechatAuthClient implements WechatAuthClient {
    @Override
    public String openidForCode(String code) {
        return code;
    }
}
