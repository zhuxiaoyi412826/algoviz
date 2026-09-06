package com.algoviz.config.security;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 允许为空的 ClientRegistrationRepository，支持 OAuth 优雅降级：
 * 未配置任何平台时仓库为空但 Bean 始终存在，避免 InMemory 实现因非空断言导致启动失败。
 */
public class ConfigurableClientRegistrationRepository
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final List<ClientRegistration> registrations;

    public ConfigurableClientRegistrationRepository(List<ClientRegistration> registrations) {
        this.registrations = registrations == null
                ? new ArrayList<>()
                : new ArrayList<>(registrations);
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (registrationId == null) {
            return null;
        }
        for (ClientRegistration registration : registrations) {
            if (registrationId.equals(registration.getRegistrationId())) {
                return registration;
            }
        }
        return null;
    }

    /** 是否未配置任何平台 */
    public boolean isEmpty() {
        return registrations.isEmpty();
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return Collections.unmodifiableList(registrations).iterator();
    }
}
