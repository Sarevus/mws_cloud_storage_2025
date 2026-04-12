package com.MWS;

import com.MWS.dto.EmailRequest;
import com.MWS.model.SubscriptionPlan;
import com.MWS.model.User;
import com.MWS.model.UserSubscription;
import com.MWS.repository.SubscriptionPlanRepository;
import com.MWS.repository.UserRepository;
import com.MWS.repository.UserSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class SheduledMail {

    private static final Logger log = LoggerFactory.getLogger(SheduledMail.class);
    private static final String EMAIL_SERVICE_URL = "http://localhost:6767/api/email/send";

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SheduledMail(
            RestTemplate restTemplate,
            UserRepository userRepository,
            UserSubscriptionRepository userSubscriptionRepository,
            SubscriptionPlanRepository subscriptionPlanRepository
    ) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void scheduleTask() {
        List<User> users = userRepository.findAll();

        for (User user : users) {
            Map<String, Object> context = new HashMap<>();
            context.put("name", user.getName());
            context.put("message", "С нашей подпиской память не закончится никогда!!!");

            EmailRequest emailRequest = null;

            if (userSubscriptionRepository.findByUserIdAndIsActiveTrue(user.getId()).isPresent()) {
                UserSubscription userSubscription =
                        userSubscriptionRepository.findByUserIdAndIsActiveTrue(user.getId()).get();

                String planName = userSubscription.getPlan().getName();

                String templateName = "Null";

                if ("BASIC".equalsIgnoreCase(planName)) {
                    templateName = "BasicAdd.html";
                } else if ("PREMIUM".equalsIgnoreCase(planName)) {
                    templateName = "PremiumAdd.html";
                }

                if (!templateName.equals("Null")) {
                    emailRequest = new EmailRequest(
                            user.getEmail(),
                            "Подписка Cloud Storage",
                            templateName,
                            context
                    );
                }


            }

            if (emailRequest != null) {
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
}