package com.feitai.base.json.serializer;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.feitai.utils.datetime.DateTimeStyle;
import com.feitai.utils.datetime.DateUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

public class BooleanSerializer implements ObjectSerializer {

    public final static BooleanSerializer instance = new BooleanSerializer();


    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object.getClass() == Boolean.class || object.getClass() == Boolean.TYPE) {
            if((boolean)object){
                out.writeString("是" );
            }else{

            }
        }
        return;
    }
}
