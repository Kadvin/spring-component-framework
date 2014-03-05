/**
 * @author XiongJie, Date: 13-12-27
 */
package spring.test.scan_p;

import org.springframework.stereotype.Component;

/** the provider impl */
@Component
public class ProviderImpl implements Provider {
    private static int times;

    private int sequence;

    public ProviderImpl() {
        System.out.println("The provider is created: " + ++times);
        sequence = times;
    }

    @Override
    public String doSomeFavor(Object caller) {
        return ("Work by provider: " + sequence + "/" + times);
    }
}
