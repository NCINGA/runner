package com.ncinga.runner.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResponseMessage {
    protected String code;
    protected String message;
    protected Object data;
    protected Object error;

    public static ResponseMessage getInstance(ResponseCode responseCode) {
        ResponseMessage responseMessage = new ResponseMessage(responseCode.getCode(), responseCode.getMessage(), null, null);
        return responseMessage;
    }

    public static ResponseMessage getInstance(ResponseCode responseCode, Object data) {
        ResponseMessage responseMessage = new ResponseMessage(responseCode.getCode(), responseCode.getMessage(), data, null);
        return responseMessage;
    }

    public static ResponseMessage getInstance(ResponseCode responseCode, Object data, Object error) {
        ResponseMessage responseMessage = new ResponseMessage(responseCode.getCode(), responseCode.getMessage(), data, error);
        return responseMessage;
    }
}
