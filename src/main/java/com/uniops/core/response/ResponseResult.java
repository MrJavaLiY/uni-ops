package com.uniops.core.response;

import lombok.Data;

/**
 * ResponseResult 类的简要描述
 *
 * @author liyang
 * @since 2026/1/20
 */
@Data
public class ResponseResult <T>{
    private int code;
    private String message;
    private T data;
    public static <T> ResponseResult<T> success(T data){
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    public static <T> ResponseResult<T> error(int  code, String message){
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    public static <T> ResponseResult<T> error(String message){
        return error(500, message);
    }
    public static <T> ResponseResult<T> error(){
        return error(500, "服务器异常");
    }
    public static <T> ResponseResult<T> error(Exception e){
        return error(e.getMessage());
    }
    public static <T> ResponseResult<T> error(Throwable e){
        return error(e.getMessage());
    }
    public static <T> ResponseResult<T> error(int code, String message, T data){
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

}
