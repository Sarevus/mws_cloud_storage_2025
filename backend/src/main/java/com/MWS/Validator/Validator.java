package com.MWS.Validator;


import com.MWS.Validator.annotations.Email;
import com.MWS.Validator.annotations.NotNull;
import com.MWS.Validator.annotations.PhoneNumber;
import com.MWS.Validator.annotations.Size;

import java.lang.reflect.Field;
import java.util.regex.Pattern;


public class Validator {
    private static final String EMAIL_REGEX = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

//    private static final String PHONE_NUMBER_REGEX = "^\\+([78])\\s\\([0-9]{3}\\)\\s[0-9]{3}-[0-9]{2}-[0-9]{2}$";
    private static final String PHONE_NUMBER_REGEX = "^\\+7\\d{10}$";
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(PHONE_NUMBER_REGEX);

    public static ValidationResult validate(Object object) {
        ValidationResult errors = new ValidationResult();

        if (object == null) {
            errors.addError("Object can't be null");
            return errors;
        }

        for (Field field : object.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);

                Object fieldValue = field.get(object);

                validateNotNull(field, fieldValue, errors);
                validateSize(field, fieldValue, errors);
                validatePhoneNumber(field, fieldValue, errors);
                validateEmail(field, fieldValue, errors);
            } catch (IllegalAccessException e) {
                errors.addError("No access to the field");
            }
        }
        return errors;
    }

    private static void validateNotNull(Field field, Object fieldValue, ValidationResult errors) {
        if (field.isAnnotationPresent(NotNull.class)) {
            NotNull annotation = field.getAnnotation(NotNull.class);
            if (fieldValue == null) {
                errors.addError(annotation.message());
                return;
            }
        }
    }

    private static void validateSize(Field field, Object fieldValue, ValidationResult errors) {
        if (field.isAnnotationPresent(Size.class)) {
            Size annotation = field.getAnnotation(Size.class);
            if (fieldValue == null) {
                errors.addError(annotation.message());
                return;
            }

            if (!(fieldValue instanceof String stringValue)) {
                return;
            }

            int len = stringValue.length();
            if (len < annotation.min() || len > annotation.max()) {
                errors.addError(annotation.message());
            }
        }
    }

    private static void validatePhoneNumber(Field field, Object fieldValue, ValidationResult errors) {
        if (field.isAnnotationPresent(PhoneNumber.class)) {
            PhoneNumber annotation = field.getAnnotation(PhoneNumber.class);
            if (fieldValue == null) {
                errors.addError(annotation.message());
                return;
            }

            if (!(fieldValue instanceof String stringValue)) {
                return;
            }

            if (!PHONE_NUMBER_PATTERN.matcher(stringValue).matches()) {
                errors.addError(annotation.message());
            }
        }

    }

    private static void validateEmail(Field field, Object fieldValue, ValidationResult errors) {
        if (field.isAnnotationPresent(Email.class)) {
            Email annotation = field.getAnnotation(Email.class);
            if (fieldValue == null) {
                errors.addError(annotation.message());
                return;
            }

            if (!(fieldValue instanceof String stringValue)) {
                return;
            }

            if (!EMAIL_PATTERN.matcher(stringValue).matches()) {
                errors.addError(annotation.message());
            }
        }
    }
}
