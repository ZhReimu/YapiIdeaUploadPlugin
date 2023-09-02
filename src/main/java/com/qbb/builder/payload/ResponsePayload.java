package com.qbb.builder.payload;

import com.qbb.builder.KV;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResponsePayload extends BasePayload {

    private String type;
    private String title;
    private String description;
    private FieldPayload items;

    public static ResponsePayload copy(KV<?, ?> kv) {
        return copy(ResponsePayload.class, kv);
    }
}
