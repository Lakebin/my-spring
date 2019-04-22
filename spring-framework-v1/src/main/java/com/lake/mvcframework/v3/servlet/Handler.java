package com.lake.mvcframework.v3.servlet;

import com.lake.mvcframework.annotation.MyRequestParam;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 保存url-method映射关系
 *
 * @author Lake Fang
 * @date 2019-04-19 12:31
 */
public class Handler {
    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    /** method 所属实例 */
    private Object controller;

    /** 请求方法 */
    private Method method;

    /** 请求路径正则 */
    private Pattern url;

    /** 参数位置序号映射 */
    private Map<String,Integer> parameterIndexMapping;

    private Class<?>[] parameterTypes;

    public Handler(Object controller, Method method, Pattern url) {
        this.controller = controller;
        this.method = method;
        this.url = url;
        parameterTypes = method.getParameterTypes();
        parameterIndexMapping = new HashMap<>();
        setParameterIndexMapping(method);
    }

    public Pattern getUrl() {
        return url;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public Map<String, Integer> getParameterIndexMapping() {
        return parameterIndexMapping;
    }

    /**
     * 从method中获取请求参数，并将请求参数和参数的位置保存到map中，这样使得后面请求调用方法时能正确的匹配参数的位置
     * @param method
     */
    private void setParameterIndexMapping(Method method) {
        /*Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType =  parameterTypes[i];
            //保存HttpServletRequest或者HttpServletResponse
            if ( parameterType == HttpServletRequest.class || parameterType == HttpServletResponse.class) {
                parameterIndexMapping.put(parameterType.getName(), i);
            } else if (parameterType.isAnnotationPresent(MyRequestParam.class)){
                //保存MyRequestParam注解参数的位置
                MyRequestParam requestParam = parameterType.getAnnotation(MyRequestParam.class);
                String parameterName = requestParam.value();
                if (StringUtils.isNotEmpty(parameterName)) {
                    parameterIndexMapping.put(parameterName, i);
                }
            }
        }*/

        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter =  parameters[i];
            //保存HttpServletRequest或者HttpServletResponse
            if ( parameter.getType() == HttpServletRequest.class || parameter.getType() == HttpServletResponse.class) {
                parameterIndexMapping.put(parameter.getType().getName(), i);
            } else if (parameter.isAnnotationPresent(MyRequestParam.class)){
                //保存MyRequestParam注解参数的位置
                MyRequestParam requestParam = parameter.getAnnotation(MyRequestParam.class);
                String parameterName = requestParam.value();
                if (StringUtils.isNotEmpty(parameterName)) {
                    parameterIndexMapping.put(parameterName, i);
                }
            }
        }
    }
}
