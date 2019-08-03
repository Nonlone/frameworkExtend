package per.nonlone.frameworkExtend.spring;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

public class FixkSpringfoxBeanNameGenerator extends AnnotationBeanNameGenerator {

    @Override
    public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
        if (definition.getBeanClassName().startsWith("springfox")) {
            return super.generateBeanName(definition, registry);
        }
        return BeanDefinitionReaderUtils.generateBeanName(definition, registry);
    }

}
