package io.choerodon.devops.infra.enums;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Created by zzy on 2018/1/10.
 */
public enum PipelineStatus {
    CREATED,
    RUNNING,
    PENDING,
    SUCCESS,
    FAILED,
    STOP,
    CANCELED,
    NOT_AUDIT,
    SKIPPED;

    private static final HashMap<String, PipelineStatus> valuesMap = new HashMap<>(8);

    static {
        PipelineStatus[] var0 = values();

        for (PipelineStatus status : var0) {
            valuesMap.put(status.toValue(), status);
        }

    }

    PipelineStatus() {
    }

    @JsonCreator
    public static PipelineStatus forValue(String value) {
        return valuesMap.get(value);
    }

    @JsonValue
    public String toValue() {
        return this.name().toLowerCase();
    }

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}

