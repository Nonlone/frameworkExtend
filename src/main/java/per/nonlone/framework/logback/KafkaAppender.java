package per.nonlone.framework.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import per.nonlone.utils.CollectionUtils;
import per.nonlone.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class KafkaAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

    private static final String KAFKA_LOGGER_PREFIX = KafkaProducer.class.getPackage().getName().replaceFirst("\\.producer$", "");

    private final ConcurrentLinkedQueue<ILoggingEvent> queue = new ConcurrentLinkedQueue<ILoggingEvent>();

    private final AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<ILoggingEvent>();

    @Setter
    @Getter
    private String topic;

    @Setter
    @Getter
    private String key = null;

    @Setter
    @Getter
    private Integer partition = null;

    @Setter
    @Getter
    private Map<String, String> producerConfig = new HashMap<>();

    @Setter
    @Getter
    private Map<String, String> header = new HashMap<>();

    @Setter
    @Getter
    private Formatter formatter;
    private List<Header> headerList = null;
    private Producer<byte[], byte[]> producer;

    private static final Pattern pattern = Pattern.compile("[\\t\\r\\n]");

    public void addHeader(String keyValue) {
        String[] split = keyValue.split("=", 2);
        if (split.length == 2) {
            addHeader(split[0], split[1]);
        }
    }

    public void addHeader(String key, String value) {
        this.header.put(key, value);
    }


    public void addProducerConfig(String keyValue) {
        String[] split = keyValue.split("=", 2);
        if (split.length == 2) {
            addProducerConfigValue(split[0], split[1]);
        }
    }

    public void addProducerConfigValue(String key, String value) {
        this.producerConfig.put(key, value);
    }

    @Override
    public void start() {
        if (Objects.isNull(formatter)) {
            this.formatter = t -> t.getFormattedMessage();
        }
        if (partition != null && partition < 0) {
            partition = null;
        }
        super.start();
        if (CollectionUtils.isNotEmpty(header)) {
            header.forEach((k, v) -> {
                headerList.add(new RecordHeader(k, v.getBytes()));
            });
        }
        this.producer = getKafkaProdcucer();
    }


    private Producer<byte[], byte[]> getKafkaProdcucer() {
        if (Objects.isNull(this.producer)) {
            synchronized (this) {
                if (Objects.isNull(this.producer)) {
                    Properties props = new Properties();
                    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
                    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
                    props.putAll(producerConfig);
                    try {
                        this.producer = new KafkaProducer<byte[], byte[]>(props);
                    } catch (Throwable e) {
                        addError(String.format("init kafka client error %s", e.getMessage()), e);
                    }
                }
            }
        }
        return this.producer;
    }

    @Override
    public void stop() {
        super.stop();
        if (Objects.nonNull(this.producer)) {
            try {
                this.producer.close();
            } catch (KafkaException e) {
                this.addWarn("Failed to shut down kafka producer: " + e.getMessage(), e);
            } finally {
                this.producer = null;
            }
        }


    }

    /**
     * process deferred Appenders
     */
    private void processDeferredAppends() {
        ILoggingEvent event;
        while ((event = queue.poll()) != null) {
            super.doAppend(event);
        }
    }

    private void deferAppend(ILoggingEvent event) {
        queue.add(event);
    }

    @Override
    public void doAppend(ILoggingEvent event) {
        processDeferredAppends();
        if (event.getLoggerName().startsWith(KAFKA_LOGGER_PREFIX)) {
            deferAppend(event);
        } else {
            super.doAppend(event);
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        byte[] valueByte = null;
        String value = pattern.matcher(this.formatter.format(event)).replaceAll("");
        if(StringUtils.isNotBlank(value)){
            valueByte = value.getBytes();
        }
        byte[] keyByte = null;
        if(StringUtils.isNotBlank(this.key)){
            keyByte = this.key.getBytes();
        }
        ProducerRecord<byte[], byte[]> data = new ProducerRecord<>(this.topic, this.partition, System.currentTimeMillis(), keyByte, valueByte, headerList);
        if (Objects.isNull(this.producer)) {
            this.producer = getKafkaProdcucer();
        }
        // producer 非空发送
        if (Objects.nonNull(this.producer)) {
            this.producer.send(data, ((metadata, exception) -> {
                if (Objects.nonNull(exception)) {
                    addError(String.format("send log to kafka error topic<%s> ", metadata.topic()), exception);
                }
            }));
        } else if (aai.iteratorForAppenders().hasNext()) {
            addWarn("kafka error fallback log appender");
            this.aai.appendLoopOnAppenders(event);
        }
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        aai.addAppender(newAppender);
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return aai.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return aai.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return aai.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        aai.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return aai.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return aai.detachAppender(name);
    }
}
