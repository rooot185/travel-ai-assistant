package com.wayfarer.travel.api;

public class ApiFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final boolean retryable;

    public ApiFailure(int status, String code, String message, boolean retryable) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryable = retryable;
    }

    public static ApiFailure invalid(String message) {
        return new ApiFailure(400, "INVALID_REQUEST", message, false);
    }

    public static ApiFailure conflict(String message) {
        return new ApiFailure(409, "REVISION_CONFLICT", message, false);
    }

    public static ApiFailure missing() {
        return new ApiFailure(404, "NOT_FOUND", "找不到这条记录。", false);
    }

    public int status() { return status; }
    public String code() { return code; }
    public boolean retryable() { return retryable; }
}
