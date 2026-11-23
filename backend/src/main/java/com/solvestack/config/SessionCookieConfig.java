
package com.solvestack.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
public class SessionCookieConfig {
    public static final Duration GUEST_LIFETIME = Duration.ofDays(365);
    public static final String PERSIST_GUEST_COOKIE =
            SessionCookieConfig.class.getName() + ".persistGuest";

    @Bean
    CookieSerializer sessionCookieSerializer(
            @Value("${solvestack.session.cookie-secure}") boolean secure,
            @Value("${solvestack.session.cookie-same-site:Lax}") String sameSite) {

        DefaultCookieSerializer browserSession = cookie(secure, sameSite);
        DefaultCookieSerializer guestSession = cookie(secure, sameSite);

        guestSession.setCookieMaxAge(Math.toIntExact(GUEST_LIFETIME.toSeconds()));

        return new CookieSerializer() {
            @Override
            public List<String> readCookieValues(HttpServletRequest request) {
                return browserSession.readCookieValues(request);
            }

            @Override
            public void writeCookieValue(CookieValue cookieValue) {
                boolean persistGuest = Boolean.TRUE.equals(
                        cookieValue.getRequest().getAttribute(PERSIST_GUEST_COOKIE));

                (persistGuest ? guestSession : browserSession)
                        .writeCookieValue(cookieValue);
            }
        };
    }

    private static DefaultCookieSerializer cookie(boolean secure, String sameSite) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SOLVESTACK_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(sameSite);
        return serializer;
    }
}
