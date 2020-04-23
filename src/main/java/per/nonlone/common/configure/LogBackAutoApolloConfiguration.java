package per.nonlone.common.configure;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.spring.boot.ApolloAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;

@Configuration
@Slf4j
@ConditionalOnClass(ApolloAutoConfiguration.class)
@AutoConfigureAfter(ApolloAutoConfiguration.class)
public class LogBackAutoApolloConfiguration implements SmartInitializingSingleton {

    /**
     * 默认Namespace
     */
    private  String logbackNamespace = "logback";

    public LogBackAutoApolloConfiguration(){};

    protected LogBackAutoApolloConfiguration(String namespace){
        this.logbackNamespace = namespace;
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 手动刷新logback配置
        reconfigureLogback(logbackNamespace);
        // 注入监听
        Config config = ConfigService.getAppConfig();
        config.addChangeListener(t -> {
            if (logbackNamespace.equals(t.getNamespace())) {
                reconfigureLogback(logbackNamespace);
            }
        });
    }

    protected void reconfigureLogback(String namespace) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("reconfigureLogback"));
        }

        ConfigFile logbackConfigFile = ConfigService.getConfigFile(namespace, ConfigFileFormat.XML);
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();
        try {
            configurator.doConfigure(new ByteArrayInputStream(logbackConfigFile.getContent().getBytes()));
        } catch (JoranException je) {
            log.error(String.format(" reconfigureLogback error %s", je.getMessage()), je);
        }
    }
}
