/**
 * Developer: Kadvin Date: 14-5-29 下午2:27
 */
package net.happyonroad.spring.support;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;

/**
 * Description here
 */
public class CombinedMessageSource extends DelegatingMessageSource {

    private final List<ResourceBundleMessageSource> sources;

    public CombinedMessageSource(List<ResourceBundleMessageSource> sources) {
        this.sources = sources;
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String message = null;
        for (ResourceBundleMessageSource source : sources) {
            try {
                message = source.getMessage(code, args, defaultMessage, locale);
                break;
            } catch (NoSuchMessageException e) {
                //continue
            }
        }
        if( message == null )
            return super.getMessage(code, args, defaultMessage, locale);
        return message;
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        String message = null;
        for (ResourceBundleMessageSource source : sources) {
            try {
                message = source.getMessage(code, args, locale);
                break;
            } catch (NoSuchMessageException e) {
                //continue
            }
        }
        if( message == null )
            return super.getMessage(code, args, locale);
        return message;
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        String message = null;
        for (ResourceBundleMessageSource source : sources) {
            try {
                message = source.getMessage(resolvable, locale);
                break;
            } catch (NoSuchMessageException e) {
                //continue
            }
        }
        if( message == null )
            return super.getMessage(resolvable, locale);
        return message;
    }
}
