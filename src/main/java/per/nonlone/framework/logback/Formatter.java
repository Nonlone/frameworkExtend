package per.nonlone.framework.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface Formatter {

    String format(ILoggingEvent event);

}
