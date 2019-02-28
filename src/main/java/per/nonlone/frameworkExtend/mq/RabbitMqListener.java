package per.nonlone.frameworkExtend.mq;

import org.springframework.amqp.core.AcknowledgeMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RabbitMqListener {
    /**
     * 队列名
     *
     * @return
     */
    String queue();

    /**
     * 队列持久化
     *
     * @return
     */
    boolean durable() default true;

    /**
     * 同时消费线程数
     *
     * @return
     */
    int concurrentConsumer() default 1;

    /**
     * 最大同时消费数
     *
     * @return
     */
    int maxConcurrentConsumer() default 1;

    /**
     * Ack策略
     *
     * @return
     */
    AcknowledgeMode acknowledgeMode() default AcknowledgeMode.AUTO;

    /**
     * RabbitAdmin Bean名
     *
     * @return
     */
    String rabbitAdminBeanName() default "";

    /**
     * ConnectionFactory Bean名
     *
     * @return
     */
    String conncetionFactoryBeanName() default "";

}
