package com.MWS;

import com.MWS.dto.EmailRequest;
import com.MWS.model.UserEntity;
import com.MWS.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SheduledMail {
    private static final Logger log = LoggerFactory.getLogger(SheduledMail.class);
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    private final String EMAIL_SERVICE_URL = "http://localhost:6767/api/email/send";

    public SheduledMail(RestTemplate restTemplate, UserRepository userRepository) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void scheduleTask() {
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            Map<String, Object> context = new HashMap<>();
            context.put("name", user.getName());
            context.put("message", "С нашей подпиской память не закончится никогда!!!");

            EmailRequest emailRequest = new EmailRequest(
                    user.getEmail(),
                    "Подписка Cloud Storage",
                    "EmailAdvertismentTemplate1",
                    context
            );

            try {
                restTemplate.postForEntity(EMAIL_SERVICE_URL, emailRequest, Void.class);
                log.info("Запрос на рассылку отправлен пользователю: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Email_Service недоступен или вернул ошибку для {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }
    }
}