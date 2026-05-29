package com.lvatong.lft.security.encryption;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.PostLoad;
import java.lang.reflect.Field;

@Slf4j
@Component
public class EncryptionListener {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        EncryptionListener.encryptionService = encryptionService;
    }

    @PrePersist
    @PreUpdate
    public void encrypt(Object entity) {
        processFields(entity, true);
    }

    @PostLoad
    public void decrypt(Object entity) {
        processFields(entity, false);
    }

    private void processFields(Object entity, boolean encrypt) {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Encrypted.class)) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(entity);
                        if (value instanceof String) {
                            String stringValue = (String) value;
                            if (encrypt) {
                                if (!encryptionService.isEncrypted(stringValue)) {
                                    field.set(entity, encryptionService.encrypt(stringValue));
                                }
                            } else {
                                if (encryptionService.isEncrypted(stringValue)) {
                                    field.set(entity, encryptionService.decrypt(stringValue));
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        log.error("处理加密字段失败: {}.{}", clazz.getName(), field.getName(), e);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }
}