package per.nonlone.frameworkExtend.mybatis.genid;

import tk.mybatis.mapper.genid.GenId;

import java.util.UUID;

public class UUIDLeastGenId implements GenId<Long> {


    @Override
    public Long genId(String table, String column) {
        return UUID.randomUUID().getLeastSignificantBits();
    }
}
