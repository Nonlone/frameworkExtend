package per.nonlone.framework.logback;

import ch.qos.logback.core.PropertyDefinerBase;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class IPPropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error(String.format(" getIp error %s", e.getMessage()), e);
        }
        return null;
    }

}
