package org.anest.hola.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MessageUtils {

    private static MessageSource messageSource;
    private static final MessageSource FALLBACK_SOURCE = createFallbackMessageSource();

    public MessageUtils(MessageSource messageSource) {
        MessageUtils.messageSource = messageSource;
    }

    public static String get(String key, Object... args) {
        MessageSource activeSource = (messageSource != null) ? messageSource : FALLBACK_SOURCE;
        return activeSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }

    private static MessageSource createFallbackMessageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setUseCodeAsDefaultMessage(true);
        return source;
    }
}

