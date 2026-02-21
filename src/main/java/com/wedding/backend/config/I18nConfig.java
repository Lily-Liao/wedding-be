package com.wedding.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

@Configuration
public class I18nConfig {

    /**
     * Resolves locale from the Accept-Language request header.
     * Defaults to zh_TW when header is absent or unsupported.
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.TAIWAN);
        resolver.setSupportedLocales(List.of(Locale.TAIWAN, Locale.ENGLISH));
        return resolver;
    }
}
