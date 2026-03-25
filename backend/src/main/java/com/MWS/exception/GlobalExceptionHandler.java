package com.MWS.exception;

import com.MWS.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 401 - Unauthorized (не авторизован)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage();

        // Проверяем различные сообщения об ошибках
        if (message != null) {
            if (message.contains("No such user") || message.contains("not logged in")) {
                logger.warn("Неавторизованный доступ: {}", message);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse(message));
            }
            if (message.contains("User not found")) {
                logger.warn("Пользователь не найден: {}", message);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse(message));
            }
            if (message.contains("You cannot share file to yourself")) {
                logger.warn("Попытка поделиться файлом с собой");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse(message));
            }
        }

        // По умолчанию - 400 Bad Request
        logger.warn("Некорректный запрос: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    // 402
    @ExceptionHandler(PaymentRequiredException.class)
    @ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
    public ErrorResponse handlePaymentRequired(PaymentRequiredException ex) {
        logger.warn("Требуется оплата: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    // 403 - Forbidden (не хватает прав)
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleSecurity(SecurityException ex) {
        logger.warn("Доступ запрещен: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    // 404 - Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(EntityNotFoundException ex) {
        logger.warn("Сущность не найдена: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    // 500 - Internal Server Error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        logger.error("Внутренняя ошибка сервера: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Внутренняя ошибка сервера: " + e.getMessage()));
    }
}