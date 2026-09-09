package com.club.order.auth;

import com.club.order.common.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/** 真实微信登录：用 code 调 jscode2session 换 openid。配置 app.wechat.enabled=true 后生效。 */
@Component
@ConditionalOnProperty(name = "app.wechat.enabled", havingValue = "true")
public class RealWechatAuthClient implements WechatAuthClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String appid;
    private final String secret;

    public RealWechatAuthClient(
            @Value("${app.wechat.appid:}") String appid,
            @Value("${app.wechat.secret:}") String secret) {
        this.appid = appid;
        this.secret = secret;
    }

    @Override
    public String openidForCode(String code) {
        if (appid.isBlank() || secret.isBlank()) {
            throw new BizException(500, "微信 AppID/Secret 未配置");
        }
        String url = UriComponentsBuilder
                .fromHttpUrl("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", appid)
                .queryParam("secret", secret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .toUriString();
        Map<?, ?> resp = restTemplate.getForObject(url, Map.class);
        if (resp == null || resp.get("openid") == null) {
            throw new BizException(401, "微信登录失败：" + (resp == null ? "无响应" : resp.get("errmsg")));
        }
        return (String) resp.get("openid");
    }
}
