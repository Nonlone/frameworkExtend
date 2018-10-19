package com.feitai.base.mybatis.genid;

import com.feitai.utils.SnowFlakeIdGenerator;
import tk.mybatis.mapper.genid.GenId;

public class SnowFlakeGenId implements GenId<Long> {

    @Override
    public Long genId(String table, String column) {
        return genId();
    }

    private static  Long genId(){
        return SnowFlakeIdGenerator.getDefaultNextId();
    }
}
