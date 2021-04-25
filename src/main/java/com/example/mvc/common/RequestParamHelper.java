package com.example.mvc.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;

/**
 * @author panzhi
 * @since 2021-04-24
 */
public class RequestParamHelper {

    public static Object[] buildRequestParam(RequestHandler handle, HttpServletRequest request, HttpServletResponse response){
        //获取参数列表
        Class<?>[] parameters = handle.getMethod().getParameterTypes();
        //保存所有自动赋值的参数值
        Object[] paramValues = new Object[parameters.length];
        Map<String,String[]> paramMap = request.getParameterMap();
        for (Map.Entry<String,String[]> param : paramMap.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
            if(!handle.getParamIndexMapping().containsKey(param.getKey())){
                continue;
            }
            int index = handle.getParamIndexMapping().get(param.getKey());
            paramValues[index] = convert(parameters[index],value);
        }
        int requestIndex= handle.getParamIndexMapping().get(HttpServletRequest.class.getName());
        paramValues[requestIndex] = request;
        int responseIndex= handle.getParamIndexMapping().get(HttpServletResponse.class.getName());
        paramValues[responseIndex] = response;
        return paramValues;
    }

    /**
     * 数据类型转换
     * @param type
     * @param value
     * @return
     */
    private static Object convert(Class<?> type,String value){
        if(type.equals(Integer.class)){
            return Integer.valueOf(value);
        }
        return value;
    }
}
