package com.lake.mvcframework.v3.servlet;

import com.lake.mvcframework.annotation.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.Beans;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static jdk.nashorn.api.scripting.ScriptUtils.convert;

/**
 * v2版精简SpringMVC
 *
 * @author Lake Fang
 * @date 2019-03-30 10:48
 */
public class MyDispatcherServlet extends HttpServlet {

    /**使用log.debug*/
    private final static Logger log = LoggerFactory.getLogger(MyDispatcherServlet.class);

    /** 保存application.properties配置文件中的内容 */
    private Properties contextConfig = new Properties();

    /**
     * 传说中的IOC容器，我们来揭开它的神秘面纱
     * 为了简化程序，暂时不考虑ConcurrentHashMap
     * 主要还是关注设计思想和原理
     */
    private Map<String,Object> ioc = new HashMap<String,Object>();

    /** 保存url和Method的对应关系 */
    private List<Handler> handlerMapping = new ArrayList<>();

    /** 保存扫描的所有的类名 */
    private List<String> classNames = new ArrayList<String>();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //6、调用，运行阶段
        try {
            //用了委派模式，委派模式的具体逻辑在doDispatch()方法中：
            doDispatch(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().write("500 Exection,Detail : " + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     *
     * @param req
     * @param resp
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        log.debug("doDispatch...");
        //获取HandlerMapping
        Handler handler = getHandler(req);

        if(null == handler){
            log.debug("404 not found");
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        //获取调用方法
        Method method = handler.getMethod();
        log.debug("请求method[{}]", method.getName());
        //获取请求参数
        Object[] params = paramAdapter(handler, req, resp);

        log.debug("method[{}] invoke with parameter[{}]", method.getName(), params);
        //从IOC中根据bean name获取method所属的类对象
        Object value = method.invoke(handler.getController(), params);
        if (method.getReturnType() != void.class) {
            log.debug("请求结果：" + value);
            //解决中文乱码
            String v = "";
            if (null != value) {
                v = new String(value.toString().getBytes("gbk"), StandardCharsets.ISO_8859_1);
            }

            resp.getWriter().println(v);
        }
    }

    /**
     * 从handle mapping映射集合中获取HandleMapping
     * @param req
     * @return
     */
    private Handler getHandler(HttpServletRequest req) {
        if (handlerMapping.isEmpty()) {return null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        log.debug("请求url['" + url +"']");
        //循环匹配url
        for (Handler handler : handlerMapping) {
            if (handler.getUrl().matcher(url).matches()) {
                return handler;
            }
        }
        return null;
    }

    /**
     * 处理请求参数
     * @param handler
     * @param req
     * @return
     */
    private Object[] paramAdapter(Handler handler, HttpServletRequest req, HttpServletResponse resp) {

        Map<String, String[]> parameterMap = req.getParameterMap();
        log.debug("URL请求参数[{}]", parameterMap);

        Object[] paramValues = new Object[handler.getMethod().getParameterTypes().length];
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (!handler.getParameterIndexMapping().containsKey(entry.getKey())) {continue;}
            String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]","")
                    .replaceAll("\\s",",");
            int index = handler.getParameterIndexMapping().get(entry.getKey());
            paramValues[index] = convert(handler.getParameterTypes()[index], value);
        }

        if(handler.getParameterIndexMapping().containsKey(HttpServletRequest.class.getName())) {
            int reqIndex = handler.getParameterIndexMapping().get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }

        if(handler.getParameterIndexMapping().containsKey(HttpServletResponse.class.getName())) {
            int respIndex = handler.getParameterIndexMapping().get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }

        return paramValues;
    }

    /**
     * 初始化
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        log.debug("servlet v3 init...");
        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));
        //2、扫描相关类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3、初始化扫描到的类，并且将它们放入到ICO容器之中
        doInstance();
        //4、完成依赖注入
        doAutoWired();
        //5、初始化HandlerMapping
        initHandlerMapping();
        log.debug("My mvc framework init .");
    }

    //url传过来的参数都是String类型的，HTTP是基于字符串协议
    //只需要把String转换为任意类型就好
    private Object convert(Class<?> type,String value){
        //如果是int
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        else if(Double.class == type){
            return Double.valueOf(value);
        }
        //如果还有double或者其他类型，继续加if
        //这时候，我们应该想到策略模式了
        //在这里暂时不实现，希望小伙伴自己来实现
        return value;
    }

    /**
     * 初始化url和Method的一对一对应关系
     */
    private void initHandlerMapping() {
        log.debug("initHandlerMapping...");
        if(ioc.isEmpty()){ return; }
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(MyController.class)) { continue; }
            //保存写在类上面的@RequestMapping("/user)
            String baseUrl = "/";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl += requestMapping.value();
            }
            //遍历public方法，将注解了@RequestMapping的方法保存在handlerMapping中
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) { continue;}
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                //TODO MyRequestMapping中可能配置多个url
                String url = baseUrl + "/" + requestMapping.value();
                url = url.replaceAll("/+", "/");
                Pattern urlPattern = Pattern.compile(url);
                Handler handlerMapping = new Handler(entry.getValue(), method, urlPattern);
                this.handlerMapping.add(handlerMapping);
                log.debug("mapping:url[{}], method[{}]", url, method);
            }
        }
        log.debug("initHandlerMapping over.");
    }

    private void doAutoWired() {
        log.debug("开始依赖注入...");
        //扫描IOC容器中的类
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            //获取带注入的类
            Class clzz = entry.getValue().getClass();
            log.debug("开始依赖注入[" + clzz +"]");
            //这里参数上和方法上都有可能注解Autowired，为了简化操作只使用了参数上的autowired注解
            //获取参数 Declared 所有的，特定的 字段，包括private/protected/default
            Field[] fields = clzz.getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) { continue; }
                MyAutowired autowired = field.getAnnotation(MyAutowired.class);
                String beanName = autowired.value().trim();
                //若用户自定义了beanName，则直接从IOC中取，没有则取默认
                if (StringUtils.isBlank(beanName)) {
                    beanName = field.getType().getName();
                }
                //暴力访问
                field.setAccessible(true);
                try {
                    //TODO 属性类型如果是接口，则应该查询他的实现类
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                log.debug("正在注入参数[" + clzz.getName() + "." + field.getName() + "]" + "注入对象[" + ioc.get(beanName) +"]");
            }
            log.debug("注入完成[" + clzz + "].");
        }
    }

    /**
     * 初始化bean
     */
    private void doInstance() {
        log.debug("IOC容器开始初始化...");
        //遍历classNames中的类名，实例化其中带有@MyCOntroller或者@MyService的类，为了简化这里只列举了这两个类
         if (classNames.isEmpty()) { log.debug("没有要实例化的类"); return;}
        try {
            for (String className : classNames) {
                Class clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    log.debug("正在实例化 [" + clazz.getName() + "]");
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                }else if (clazz.isAnnotationPresent(MyService.class)) {
                    log.debug("正在实例化 [" + clazz.getName() + "]");
                    MyService service = (MyService) clazz.getAnnotation(MyService.class);
                    //1、自定义的beanName
                    String beanName = service.value();
                    //2、默认类名首字母小写
                    if (StringUtils.isBlank(beanName)) {
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName, instance);
                    //3、根据类型自动赋值,投机取巧的方式
                    for (Class<?> i : clazz.getInterfaces()) {
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The “" + i.getName() + "” is exists!!");
                        }
                        //把接口的类型直接当成key 了
                        ioc.put(i.getName(), instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        log.debug("IOC容器初始化完成. " + ioc);
    }

    /**
     * 类名首字母小写
     * @param beanName
     * @return
     */
    private String toLowerFirstCase(String beanName) {
        //转为ASCII码
        char[] chars = beanName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描指定包下的类
     *
     * @param scanPackage 包名 com.lake.demo
     */
    private void doScanner(String scanPackage) {
        log.debug("开始扫描包：" + scanPackage);
        URL url = this.getClass().getClassLoader().getResource(scanPackage.replaceAll("\\.", "/"));
        log.debug("包路径" + url);
        if (null == url) { throw new NullPointerException("扫描的包为空"); }
        File classPath = new File(url.getFile());
        for (File file : classPath.listFiles()) {
            //如果是文件夹，则递归子目录
            if (file.isDirectory()) {
                doScanner(scanPackage + "." + file.getName());
            } else if (file.isFile()){
                //若是java文件则获取文件名
                if (!file.getName().endsWith(".class")) { continue; }
                String className = scanPackage + "." + file.getName().replace(".class", "");
                log.debug("向classNames中添加类名： " + className);
                classNames.add(className);
            }
        }
        log.debug("包扫描完成：" + classNames);
    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void doLoadConfig(String contextConfigLocation) {
        InputStream is;
        is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
            if (is != null) {
                is.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
