package com.qbb.builder.payload;

import com.google.gson.JsonObject;
import com.qbb.builder.KV;
import com.qbb.builder.NormalTypes;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class FieldPayload extends BasePayload {

    private String type;
    private String title;
    private JsonObject mock;
    private String description;
    private Map<String, Object> properties;
    private List<String> required;

    public static FieldPayload newObjectField() {
        FieldPayload fieldPayload = new FieldPayload();
        fieldPayload.setType(NormalTypes.TYPE_OBJECT);
        return fieldPayload;
    }

    public static FieldPayload copy(KV<?, ?> kv) {
        return copy(FieldPayload.class, kv);
    }

}