package per.nonlone.frameworkExtend;


import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Properties UTF-8 载入器，需要配置 resource/META-INF/spring.factories
 * <p>
 * org.springframework.boot.env.PropertySourceLoader=\
 * UnicodePropertiesPropertySourceLoader,\
 * org.springframework.boot.env.PropertiesPropertySourceLoader,\
 * org.springframework.boot.env.YamlPropertySourceLoader
 */
public class UnicodePropertiesPropertySourceLoader implements PropertySourceLoader {

    @Override
    public String[] getFileExtensions() {
        return new String[]{"properties"};
    }

    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        EncodedResource encodedResource = new EncodedResource(resource, StandardCharsets.UTF_8.name());
        ResourcePropertySource resourcePropertySource = new ResourcePropertySource(encodedResource);
        return new ArrayList<PropertySource<?>>(1) {{
            this.add(resourcePropertySource);
        }};
    }


}