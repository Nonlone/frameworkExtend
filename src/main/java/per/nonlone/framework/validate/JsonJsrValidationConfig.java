package per.nonlone.framework.validate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import per.nonlone.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Objects;

/**
 * @Describe :
 * @Author: humin
 * @Date: 2018/11/15 11:02
 */
@Slf4j
@Component
public class JsonJsrValidationConfig implements ConstraintValidator<JsonJsrValidation, JSONObject> {

    private String[] jsonPath;

    @Override
    public void initialize(JsonJsrValidation constraintAnnotation) {
        jsonPath = constraintAnnotation.value();
        log.info("[JSON JSR] init json jsr validation:"+ JSON.toJSONString(jsonPath));
    }

    @Override
    public boolean isValid(JSONObject value, ConstraintValidatorContext context) {
        String defaultMsg = context.getDefaultConstraintMessageTemplate();
        if(jsonPath!=null&&jsonPath.length>0){
            for(String root:jsonPath){
                Object jsonObj = null;
                try {
                    jsonObj = JSONPath.read(JSON.toJSONString(value),root);
                } catch (Exception e) {
                    log.error("[JSON JSR] read json from param:{} has error",value,e);
                }
                if(checkJsonEmpty(jsonObj)){
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(root+":"+defaultMsg).addConstraintViolation();
                    return false;
                }
                //数组结构的需要校验数组下的个元素是否为空
                if(jsonObj instanceof JSONArray){
                    JSONArray array = (JSONArray)jsonObj;
                    for(int i=0;i<array.size();i++){
                        Object one = array.get(i);
                        if(checkJsonEmpty(one)){
                            context.disableDefaultConstraintViolation();
                            context.buildConstraintViolationWithTemplate(root+"["+i+"]:"+defaultMsg).addConstraintViolation();
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean checkJsonEmpty(Object jsonObj){
        if(jsonObj==null){
            return true;
        }
        if(jsonObj instanceof JSONArray){
            JSONArray array = (JSONArray)jsonObj;
            if(array==null||array.size()==0){
                return true;
            }
        }else if(jsonObj instanceof JSONObject){
            JSONObject obj = (JSONObject)jsonObj;
            if(obj==null||obj.size()==0){
                return true;
            }
        }else {
            String obj = jsonObj.toString();
            //如果是传的null的也认为是空
            if(StringUtils.isEmpty(obj)||Objects.equals("null",obj)){
                return true;
            }
        }
        return false;
    }
}