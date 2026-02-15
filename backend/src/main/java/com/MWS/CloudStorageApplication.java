package com.MWS;
import com.MWS.storage.Database;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class CloudStorageApplication {
    private static final Logger logger = LoggerFactory.getLogger(com.MWS.CloudStorageApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CloudStorageApplication.class, args);
    }

    /**
     * Проверка подключения к БД
     */
    @PostConstruct
    public void checkDatabase() {
        if (!Database.testConnection()) {
            logger.error("Не удалось подключиться к базе данных!");
            System.exit(1);
        }
    }

    /**
     * Включает CORS для всех API запросов
     * фронт сможет обратиться к бэку
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }


}
