package com.club.order.auth;

/** 微信登录抽象。当前 mock 把 code 当 openid，后续接 code2session。 */
public interface WechatAuthClient {
    String openidForCode(String code);
}
