package com.MWS;

import com.MWS.model.UserEntity;
import com.MWS.repository.UserRepository;
import com.MWS.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

import java.util.List;

@Component
public class SheduledMail {
    private static final Logger log = LoggerFactory.getLogger(SheduledMail.class);
    private final EmailService emailService;
    private final UserRepository userRepository;

    public SheduledMail(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void scheduleTask() {
        List<UserEntity> users = userRepository.findAll();
        for (UserEntity user : users) {
            String currentEmail = user.getEmail();
            System.out.println(currentEmail);
            String currentName = user.getName();
            Context context = new Context();
            context.setVariable("name", currentName);
            context.setVariable("message", "Предлагаем вам нашу подписку Ultra! в нее входит: 10гб хранилища данных.");
            context.setVariable("subject", "Подписка Cloud Storage");
            try {
                emailService.sendHtmlEmail(currentEmail, "Подписка на Cloud Storage", "EmailAdvertismentTemplate1", context);
            } catch (Exception e) {
                log.error("Ошибка в отправке сообщения: " + e.getMessage() + currentEmail);
            }
            //            emailService.sendTextEmail(currentEmail, "Подписка на Cloud Storage",
//                    "С нашей подпиской память не кончится никогда!!!");
        }

    }
}