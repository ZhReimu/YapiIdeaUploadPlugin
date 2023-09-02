package com.qbb.builder.payload;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Properties extends BasePayload {
    private String type;
    private String mock;
}
