package per.nonlone.frameworkExtend.mybatis.genid;

import tk.mybatis.mapper.genid.GenId;

import java.util.UUID;

public class UUIDGenId implements GenId<String> {

    @Override
    public String genId(String table, String column) {
        return UUID.randomUUID().toString();
    }
}
