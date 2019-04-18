package per.nonlone.frameworkExtend.configuration;

import per.nonlone.frameworkExtend.mq.RabbitMqListener;
import per.nonlone.utilsExtend.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
public class AmqpConfiguration implements SmartInitializingSingleton, ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 所有的队列监听容器MAP
     */
    private static Map<String, SimpleMessageListenerContainer> allQueueMap = new ConcurrentHashMap<>();

    @Override
    public void afterSingletonsInstantiated() {
        Map<String, Object> rabbitMqListenerMap = applicationContext.getBeansWithAnnotation(RabbitMqListener.class);
        if (!CollectionUtils.isEmpty(rabbitMqListenerMap)) {
            List<String> queueNameList = new ArrayList<>();
            // 获取listener列表
            for (String key : rabbitMqListenerMap.keySet()) {
                Object listener = rabbitMqListenerMap.get(key);
                Class<?> clazz = listener.getClass();
                // 获取注解
                RabbitMqListener rabbitMqListener = clazz.getAnnotation(RabbitMqListener.class);
                SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer();
                simpleMessageListenerContainer.setMessageListener(listener);
                String queueName = rabbitMqListener.queue();
                if(queueName.startsWith("${")&&queueName.endsWith("}")){
                    queueName = queueName.replace("${","").replace("}","");
                    queueName = applicationContext.getEnvironment().getProperty(queueName);
                }
                //加入当次初始化监听集合
                queueNameList.add(queueName);

                simpleMessageListenerContainer.addQueueNames(queueName);
                simpleMessageListenerContainer.setAcknowledgeMode(rabbitMqListener.acknowledgeMode());
                simpleMessageListenerContainer.setAutoDeclare(true);
                simpleMessageListenerContainer.setConcurrentConsumers(rabbitMqListener.concurrentConsumer());
                simpleMessageListenerContainer.setMaxConcurrentConsumers(rabbitMqListener.maxConcurrentConsumer());
                //已经初始化过的不需要再次添加监听
                if(!allQueueMap.containsKey(queueName)){
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
                    allQueueMap.put(queueName,simpleMessageListenerContainer);
                }
            }

            //移除监听
            for(Map.Entry<String, SimpleMessageListenerContainer> entry:allQueueMap.entrySet()){
                String queueName = entry.getKey();
                //如果历史的不在此次初始化的集合中,需要停止监听,移除历史集合
                if(!queueNameList.contains(queueName)){
                    try {
                        SimpleMessageListenerContainer simpleMessageListenerContainer = allQueueMap.get(queueName);
                        simpleMessageListenerContainer.stop();
                        log.info("[amqp] remove old queue {} success",queueName);
                        allQueueMap.remove(queueName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
