package com.cloudstorage.security;

/**
 * кодировщик паролей
 * обёртка над HashPassword для удобства
 * убирает многослойность и добавляет читаемости
 */
public class PasswordEncoder {

    /**
     * хэширует пароль
     * @param rawPassword
     * @return hashedPassword
     */
    public String encode(String rawPassword){
        return HashPassword.createPasswordHash(rawPassword);
    }


    /**
     * проверка пароля на совпадение с хэшем
     * @param rawPassword - пароль для проверки
     * @param storedPassword - сохранённый хэш пароля, с которым должен совпадать
     * @return true/false
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null){
            return false;
        }

        return HashPassword.verifyPassword(rawPassword, storedPassword);
    }
}
