package com.qbb.builder.payload;

import com.qbb.builder.KV;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class FieldPayload extends BasePayload {

    private String type;
    private Map<String, Object> properties;
    private List<String> required;

    public static FieldPayload copy(KV<?, ?> kv) {
        return copy(FieldPayload.class, kv);
    }

}