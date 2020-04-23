package per.nonlone.framework.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

@Data
@Slf4j
public class LogstashFormatter implements Formatter {

    private Encoder<DeferredProcessingAware> encoder;

    @Override
    public String format(ILoggingEvent iLoggingEvent) {
        try {
            return new String(encoder.encode(iLoggingEvent), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            if (log.isDebugEnabled()) {
                log.error(String.format("String charset not support utf-8 %", e.getMessage()), e);
            }
        }
        return new String(encoder.encode(iLoggingEvent));
    }
}
