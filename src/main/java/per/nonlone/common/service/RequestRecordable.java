package per.nonlone.common.service;


import per.nonlone.utils.jackson.JacksonUtils;

public interface RequestRecordable {

    default <T> String parseRequest(T request){
        return JacksonUtils.toJSONString(request);
    }

    void setRequest(String requestBody);

}
