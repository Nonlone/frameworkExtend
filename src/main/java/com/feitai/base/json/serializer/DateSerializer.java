package com.feitai.base.json.serializer;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.feitai.utils.datetime.DateTimeStyle;
import com.feitai.utils.datetime.DateUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

public class DateSerializer implements ObjectSerializer {

    public final static DateSerializer instance = new DateSerializer(DateTimeStyle.DEFAULT_YYYY_MM_DD_HH_MM_SS);

    private String parttern;

    public DateSerializer(String parttern) {
        this.parttern = parttern;
    }

    @Override
    public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        SerializeWriter out = serializer.out;
        if (object instanceof Date) {
            out.writeString(DateUtils.format((Date) object, parttern));
        }
        return;
    }
}
