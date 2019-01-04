package com.feitai.base.configuration;

import com.feitai.base.mq.RabbitMqListener;
import com.feitai.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.Properties;

@Slf4j
@Configuration
public class AmqpConfiguration implements SmartInitializingSingleton, ApplicationContextAware {

    private static final String MESSAGE_COUNT = "QUEUE_MESSAGE_COUNT";

    private ApplicationContext applicationContext;

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> rabbitMqListenerMap = applicationContext.getBeansWithAnnotation(RabbitMqListener.class);
        if (!CollectionUtils.isEmpty(rabbitMqListenerMap)) {
            // 获取listener列表
            for (Map.Entry<String, Object> entry : rabbitMqListenerMap.entrySet()) {
                Object listener = entry.getValue();
                Class<?> clazz = listener.getClass();
                // 获取注解
                RabbitMqListener rabbitMqListener = clazz.getAnnotation(RabbitMqListener.class);
                SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
                simpleMessageListenerContainer.setMessageListener(entry.getValue());
                String queueName = rabbitMqListener.queue();
                if(queueName.startsWith("${")&&queueName.endsWith("}")){
                    queueName = queueName.replace("${","").replace("}","");
                    queueName = applicationContext.getEnvironment().getProperty(queueName);
                }
                simpleMessageListenerContainer.addQueueNames(queueName);
                simpleMessageListenerContainer.setAcknowledgeMode(rabbitMqListener.acknowledgeMode());
                simpleMessageListenerContainer.setAutoDeclare(true);
                simpleMessageListenerContainer.setConcurrentConsumers(rabbitMqListener.concurrentConsumer());
                simpleMessageListenerContainer.setMaxConcurrentConsumers(rabbitMqListener.maxConcurrentConsumer());
                if (StringUtils.isNotBlank(rabbitMqListener.conncetionFactoryBeanName())) {
                    simpleMessageListenerContainer.setConnectionFactory(applicationContext.getBean(rabbitMqListener.conncetionFactoryBeanName(), ConnectionFactory.class));
                } else {
                    // 封入默认连接工厂
                    simpleMessageListenerContainer.setConnectionFactory(applicationContext.getBean(ConnectionFactory.class));
                }
                RabbitAdmin rabbitAdmin = null;
                if (StringUtils.isNotBlank(rabbitMqListener.rabbitAdminBeanName())) {
                    rabbitAdmin = applicationContext.getBean(rabbitMqListener.rabbitAdminBeanName(), RabbitAdmin.class);
                } else {
                    // 封入默认RabbitAdmin
                    rabbitAdmin = applicationContext.getBean(RabbitAdmin.class);
                }

                simpleMessageListenerContainer.setRabbitAdmin(rabbitAdmin);
                try {
                    //初始化之前,先清空已有的队列
                    String[] queueNames = simpleMessageListenerContainer.getQueueNames();
                    if(queueNames!=null&&queueNames.length>0){
                        simpleMessageListenerContainer.removeQueueNames(queueNames);
                    }
                } catch (Exception e) {
                    log.error("clear history queue[{}] has error!",e);
                }

                // 自动创建队列
                try {
                    rabbitAdmin.declareQueue(new Queue(queueName, rabbitMqListener.durable()));
                    log.info("[amqp] init new queue {} success",queueName);
                } catch (RuntimeException re) {
                    log.error(String.format("create queue error queue<%s>", rabbitMqListener.queue()), re);
                    Properties properties = rabbitAdmin.getQueueProperties(rabbitMqListener.queue());
                    int messageCount = Integer.parseInt(properties.get("MESSAGE_COUNT").toString());
                    if (messageCount == 0) {
                        // 删除后重新创建
                        rabbitAdmin.deleteQueue(queueName);
                        rabbitAdmin.declareQueue(new Queue(queueName, rabbitMqListener.durable()));
                    }
                }
                // 启动监听器
                simpleMessageListenerContainer.start();
            }
        }
    }

    @Bean
    public AmqpTemplate amqpTemplate() {
        ConnectionFactory connectionFactory = applicationContext.getBean(ConnectionFactory.class);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setEncoding("UTF-8");
        // 消息发送失败返回到队列中，yml需要配置 publisher-returns: true
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String correlationId = message.getMessageProperties().getCorrelationIdString();
            if(log.isDebugEnabled()) {
                log.debug("amqp send fail message<{}> replyCode<{}> reason<{}> exchange<{}>  routeKey<{}>", correlationId, replyCode, replyText, exchange, routingKey);
            }
        });
        // 消息确认，yml需要配置 publisher-confirms: true
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                if(log.isDebugEnabled()) {
                    log.debug("amqp send success id<{}>", correlationData.getId());
                }
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("amqp send fail reason<{}>", cause);
                }
            }
        });
        return rabbitTemplate;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
