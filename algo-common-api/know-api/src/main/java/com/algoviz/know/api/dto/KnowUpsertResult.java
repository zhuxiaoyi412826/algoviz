package com.algoviz.know.api.dto;

import java.io.Serializable;

/** 单条入库结果 */
public class KnowUpsertResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private String id;

    public KnowUpsertResult() {
    }

    public KnowUpsertResult(boolean success, String message, String id) {
        this.success = success;
        this.message = message;
        this.id = id;
    }

    public static KnowUpsertResult ok(String id) {
        return new KnowUpsertResult(true, "ok", id);
    }

    public static KnowUpsertResult fail(String message) {
        return new KnowUpsertResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
