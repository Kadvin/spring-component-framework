/**
 * Developer: Kadvin Date: 14-5-29 下午2:27
 */
package net.happyonroad.spring.support;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.DelegatingMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * 将多个依赖的组件的message source合并一个
 */
public class CombinedMessageSource extends DelegatingMessageSource {

    private final List<ResourceBundleMessageSource> sources;

    public CombinedMessageSource() {
        this.sources = new LinkedList<ResourceBundleMessageSource>();
    }

    public void combine(ResourceBundleMessageSource source){
        //后来优先级反而高
        sources.add(0, source);
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
