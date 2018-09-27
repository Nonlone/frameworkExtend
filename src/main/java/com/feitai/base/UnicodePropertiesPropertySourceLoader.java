package com.feitai.base;


import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Properties UTF-8 载入器，需要配置 resource/META-INF/spring.factories
 * <p>
 * org.springframework.boot.env.PropertySourceLoader=\
 * com.rongtai.backend.base.UnicodePropertiesPropertySourceLoader,\
 * org.springframework.boot.env.PropertiesPropertySourceLoader,\
 * org.springframework.boot.env.YamlPropertySourceLoader
 * </p>
 */
public class UnicodePropertiesPropertySourceLoader implements PropertySourceLoader {

    @Override
    public String[] getFileExtensions() {
        return new String[]{"properties"};
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        Properties properties = new Properties();
        properties.load(new InputStreamReader(resource.getInputStream(), "UTF-8"));
        if (!properties.isEmpty()) {
            return Collections.singletonList(new PropertiesPropertySource(name, properties));
        }
        return Collections.EMPTY_LIST;
    }

}