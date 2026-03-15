package com.example.saas.dm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.dm")
public class DmSenderProperties {

    private Channel instagram = new Channel();
    private Channel kakao = new Channel();

    @Getter
    @Setter
    public static class Channel {
        private boolean enabled;
        private String baseUrl;
        private String accessToken;
        private boolean dryRun = true;
        private String oauthAuthorizeUrl = "https://www.facebook.com/v23.0/dialog/oauth";
        private String oauthTokenUrl = "https://graph.facebook.com/v23.0/oauth/access_token";
        private String clientId;
        private String clientSecret;
        private String redirectUri = "http://localhost:8081/api/channels/instagram/callback";
        private String scopes = "pages_show_list,pages_read_engagement,pages_manage_metadata,business_management,instagram_basic,instagram_manage_comments,instagram_manage_messages";
        private String webhookVerifyToken;
    }
}
