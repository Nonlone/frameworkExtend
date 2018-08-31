package com.feitai.backend.base.model;

import lombok.Data;
import lombok.ToString;

import java.util.Date;

@Data
@ToString
public class BaseModel {

    protected Date createdTime;

    protected Date updateTime;
}
