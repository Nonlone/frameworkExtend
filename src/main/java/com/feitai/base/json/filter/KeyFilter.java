package com.feitai.base.json.filter;

import com.alibaba.fastjson.serializer.ValueFilter;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fastjson Key 过滤器
 */
public class KeyFilter implements ValueFilter {

    private static final Map<String, Pattern> regPatternMap = new HashMap<>();

    private final Map<String, KeyValueHandler> regKeyHandlerMap;

    public KeyFilter(Map<String, KeyValueHandler> regKeyHandlerMap){
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
        if(!CollectionUtils.isEmpty(regKeyHandlerMap)){
            for(Map.Entry<String, KeyValueHandler> entry:regKeyHandlerMap.entrySet()){
                String reg = entry.getKey();
                KeyValueHandler keyValueHandler = entry.getValue();
                Pattern pattern;
                if(regPatternMap.containsKey(reg)){
                    pattern = regPatternMap.get(reg);
                }else{
                    pattern = Pattern.compile(reg);
                    regPatternMap.put(reg,pattern);
                }
                Matcher matcher = pattern.matcher(name);
                if(matcher.find()){
                    // 脱敏处理
                    return keyValueHandler.doProcess(value);
                }
            }
        }
        return value;
    }

}