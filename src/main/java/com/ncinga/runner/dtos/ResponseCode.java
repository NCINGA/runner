package com.ncinga.runner.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ResponseCode {
    EXECUTE_REQUEST_SUCCESS("CODE-001", "Execute request process success"),
    EXECUTE_REQUEST_FAILED("CODE-00", "Execute request process failed");



    private String code;
    private String message;

    public void setCode(String code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
