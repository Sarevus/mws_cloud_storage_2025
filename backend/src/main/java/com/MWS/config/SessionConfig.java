package com.MWS.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 10000)   // включает поддержку сессий через Redit
public class SessionConfig {

    @Bean
    // RedisConnectionFactory - интерфейс для создания подключения к Redis
    // LettuceConnectionFactory - конкретная реализация
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(
                Config.getRedisHost(),
                Config.getRedisPort()
        );
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(10000); // 7 дней
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }
}
