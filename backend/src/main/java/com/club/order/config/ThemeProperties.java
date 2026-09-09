package com.club.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 可复用主题配置：换名称、分类、图标、颜色即可套用其他行业。 */
@ConfigurationProperties(prefix = "app.theme")
public record ThemeProperties(
        String appName,
        String slogan,
        String primaryColor,
        List<Category> categories) {

    public record Category(String name, String icon, String color) {
    }

    public boolean isValidCategory(String name) {
        if (name == null || name.isBlank()) return true;
        return categories != null && categories.stream().anyMatch(c -> c.name().equals(name));
    }
}
