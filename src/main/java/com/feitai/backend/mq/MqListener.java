package com.feitai.backend.mq;

import com.alibaba.fastjson.JSON;
import com.feitai.backend.base.mq.BaseMqListenter;
import com.feitai.backend.base.mq.RabbitMqListener;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.stereotype.Component;

@Component
@RabbitMqListener(queue = "test.mq", acknowledgeMode = AcknowledgeMode.NONE)
public class MqListener extends BaseMqListenter<MqBean> {

    @Override
    protected void onHandleMessage(MqBean mqBean) throws Exception {
        System.out.println("onHandleMessage >> " + JSON.toJSONString(mqBean));
    }
}

