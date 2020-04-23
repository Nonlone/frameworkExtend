package per.nonlone.framework.logback;

import ch.qos.logback.core.PropertyDefinerBase;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class HostNamePropertyDefiner extends PropertyDefinerBase {

    @Override
    public String getPropertyValue() {
        InetAddress ia;
        //获取计算机主机名
        try {
            ia = InetAddress.getLocalHost();
            String host = ia.getHostName();
            return host;
        } catch (UnknownHostException e) {
            if (log.isDebugEnabled()) {
                log.error(String.format("getHostName error %s", e.getMessage()  ), e);
            }
        }
        return null;
    }

}
