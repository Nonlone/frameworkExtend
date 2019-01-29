package com.feitai.base.json.filter;

import com.alibaba.fastjson.serializer.ValueFilter;
import com.feitai.base.annotion.NoKeyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fastjson Key 过滤器
 */
@Slf4j
public class KeyFilter implements ValueFilter {

    private static final Map<String, Pattern> regPatternMap = new HashMap<>();

    private final Map<String, KeyValueHandler> regKeyHandlerMap;

    public KeyFilter(Map<String, KeyValueHandler> regKeyHandlerMap) {
        this.regKeyHandlerMap = regKeyHandlerMap;
    }

    /**
     * 脱敏处理器
     */
    public interface KeyValueHandler {

        Object doProcess(Object value);
    }

    @Override
    public Object process(Object object, String name, Object value) {
        Class<?> objectClass = object.getClass();
        // 非处理器处理
        if (objectClass.isAnnotationPresent(NoKeyFilter.class)) {
            return value;
        }
        // 检查成员变量
        boolean checkField = false;
        if (!Map.class.isAssignableFrom(object.getClass())) {
            while (objectClass != Object.class) {
                try {
                    if (object.getClass().getDeclaredField(name).isAnnotationPresent(NoKeyFilter.class)) {
                        checkField = true;
                    }
                } catch (NoSuchFieldException nsfe) {
                    // 异常无需处理，遍历到父级寻找字段
                }
                // 跳到父级
                objectClass = objectClass.getSuperclass();
            }
            // 非Map映射的尝试判断是否在存在成员变量
            if (checkField) {
                return value;
            }
        }
        if (!CollectionUtils.isEmpty(regKeyHandlerMap)) {
            for (Map.Entry<String, KeyValueHandler> entry : regKeyHandlerMap.entrySet()) {
                String reg = entry.getKey();
                KeyValueHandler keyValueHandler = entry.getValue();
                Pattern pattern;
                if (regPatternMap.containsKey(reg)) {
                    pattern = regPatternMap.get(reg);
                } else {
                    pattern = Pattern.compile(reg);
                    regPatternMap.put(reg, pattern);
                }
                Matcher matcher = pattern.matcher(name);
                if (matcher.find()) {
                    // KeyFilter 处理
                    return keyValueHandler.doProcess(value);
                }
            }
        }
        return value;
    }

}