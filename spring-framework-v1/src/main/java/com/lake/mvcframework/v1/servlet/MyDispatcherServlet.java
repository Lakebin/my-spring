package com.lake.mvcframework.v1.servlet;

import com.lake.mvcframework.annotation.MyAutowired;
import com.lake.mvcframework.annotation.MyController;
import com.lake.mvcframework.annotation.MyRequestMapping;
import com.lake.mvcframework.annotation.MyService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * v1版精简SpringMVC
 *
 * @author Lake Fang
 * @date 2019-04-16 20:44
 */
public class MyDispatcherServlet extends HttpServlet {
    private static final Logger log = LoggerFactory.getLogger(MyDispatcherServlet.class);
    /**
     * url/method映射
     */
    private Map<String, Object> mapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            resp.getWriter().println("500 服务器异常" + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 处理请求
     *
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws InvocationTargetException, IllegalAccessException, IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        log.debug("request url:{}", url);
        if(!this.mapping.containsKey(url)){resp.getWriter().write("404 Not Found!!");return;}
        Method method = (Method) this.mapping.get(url);
        Map<String,String[]> params = req.getParameterMap();
        method.invoke(this.mapping.get(method.getDeclaringClass().getSimpleName()), req,resp,params.get("name")[0]);
    }


    /**
     * SpringMVC初始化
     *
     * @param config
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));) {
            //读取配置文件
            Properties properties = new Properties();
            properties.load(is);
            //获取扫描路径
            String scanPackage = properties.getProperty("scanPackage");
            //扫描package
            doScanner(scanPackage);

            //实例化和依赖注入
            //todo ConcurrentModificationException error,因为在遍历map时又对map进行增减操作
            for (String className : mapping.keySet()) {
                if (!className.contains(".")) {continue;}
                Class clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    mapping.put(className,clazz.newInstance());
                    //获取类上MyRequestMapping注解的url
                    MyRequestMapping requestMapping = (MyRequestMapping) clazz.getAnnotation(MyRequestMapping.class);
                    StringBuilder baseUrl = new StringBuilder("/" + requestMapping.value());
                    //获取添加了@MyRequestMapping的方法
                    for (Method method : clazz.getMethods()) {
                        if (!method.isAnnotationPresent(MyRequestMapping.class)) {continue;}
                        requestMapping = method.getAnnotation(MyRequestMapping.class);
                        baseUrl.append("/").append(requestMapping.value());
                        String url = baseUrl.toString().replaceAll("/+", "/");
                        mapping.put(url, method);
                        log.debug("mapped url[{}], method[{}]", url, method.getName());
                    }
                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    //实例化Service
                    MyService service = (MyService) clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if (StringUtils.isEmpty(beanName)) {
                        beanName = clazz.getSimpleName();
                    }
                    Object bean = null;
                    try {
                        bean = clazz.newInstance();
                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    mapping.put(beanName, bean);
                    log.debug("beanName[{}]:bean[{}]", beanName, bean);
                    //一般依赖注入时配置的属性都是接口
                    for (Class anInterface : clazz.getInterfaces()) {
                        mapping.put(anInterface.getSimpleName(), bean);
                        log.debug("beanName[{}]:bean[{}]", anInterface.getSimpleName(), bean);
                    }
                }
            }

            //依赖注入
            for (Object value : mapping.values()) {
                if (null == value) {continue;}
                Class clazz = value.getClass();
                //只对Controller依赖注入
                if (clazz.isAnnotationPresent(MyController.class)) {
                    for (Field field : clazz.getDeclaredFields()) {
                        if (!field.isAnnotationPresent(MyAutowired.class)) {continue;}
                        MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                        String beanName = autowired.value();
                        if (StringUtils.isEmpty(beanName)) {
                            //获取属性类型
                            beanName = field.getType().getSimpleName();
                        }
                        //获取bean
                        Object bean = mapping.get(beanName);
                        field.setAccessible(true);
                        field.set(value, bean);
                        log.debug("依赖注入:field[{}],bean[{}]", field.getName(), bean);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫描配置的包
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        //获取扫描包的绝对路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        if (null == url) {
            log.error("扫描的路径不存在[{}]", scanPackage);
            throw new NullPointerException("扫描的路径不存在[" + scanPackage + "]");
        }
        //获取url对应的文件
        File classDir = new File(url.getFile());
        //遍历文件夹，获取里面class文件
        for (File file : classDir.listFiles()) {
            String fileName = file.getName();
            //若是文件夹则继续遍历子文件
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + fileName);
            } else {
                //识别class文件
                if (!fileName.endsWith(".class")) {continue;}
                String className = scanPackage + "." + fileName.replaceAll(".class$", "");
                log.debug("class name[{}]", className);
                mapping.put(className, null);
            }
        }
    }
}
