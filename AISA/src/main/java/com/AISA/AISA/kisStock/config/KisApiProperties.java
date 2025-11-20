package com.AISA.AISA.kisStock.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kis")
@Getter
@Setter
public class KisApiProperties {
    private String baseUrl;
    private String authUrl;
    private String priceUrl;
    private String appkey;
    private String appsecret;
}
