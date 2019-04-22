# 1.  IOC执行原理
1. 在配置文件中配置scanPackage
2. 扫描并加载配置文件
3. 扫描scanPackage所配置的包，将带有@Service、@Controller、@Repository、@Component注解的类的类信息保存到bean的信息容器中
4. 实例化。
   1. 遍历bean信息容器，通过反射的方式获取到bean的类对象
   2. 获取bean name。 bean name优先使用注解上所配置的name，若未配置则默认类名首字母小写
   3. 以bean name为key，bean的实例为value存放到容器中。

# 2.  DI执行原理

1. 遍历IOC容器中的bean
2. 获取bean的所有属性
3. 只有带有@Autowred注解的属性需要进行依赖注入
4. 获取需要被注入实例的bean name，默认为类名首字母小写，优先使用@Autowred中所配置的值
5. 以bean name为key到IOC容器中查找，将查找的结果强制赋值给属性

# 3.  MVC执行原理

## 配置阶段

1. 配置web.xml文件。

   ```
   <?xml version="1.0" encoding="UTF-8"?>
   <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="http://java.sun.com/xml/ns/j2ee"
            xsi:schemaLocation="http://java.sun.com/xml/ns/j2ee http://java.sun.com/xml/ns/j2ee/web-app_2_4.xsd"
            version="2.4">
       <display-name>My Web Application</display-name>
       <servlet>
           <!--第一步 配置DispatcherServlet-->
           <servlet-name>lakeMVC</servlet-name>
           <servlet-class>com.lake.mvcframework.v3.servlet.MyDispatcherServlet</servlet-class>
           <!--第二步 设置初始化参数 init-param-->
           <init-param>
               <param-name>contextConfigLocation</param-name>
               <param-value>application.properties</param-value>
           </init-param>
   
           <load-on-startup>1</load-on-startup>
       </servlet>
       <servlet-mapping>
           <!--第三步 配置url-pattern-->
           <servlet-name>lakeMVC</servlet-name>
           <url-pattern>/*</url-pattern>
       </servlet-mapping>
   </web-app>
   ```
## 初始化阶段

1. 调用`public void init(ServletConfig config)`方法开始初始化MVC。

2. IOC容器初始化。在init方法中调用IOC的初始化方法开始初始化IOC容器

3. 扫描IOC容器中带有@Controller注解的类

4. HandleMapping初始化。获取并遍历Controller中的所有public方法

5. 获取带有@RequestMapping注解的方法的Method对象

6. 获取@RequestMapping中配置的url信息，并将其解析成为Pattern

7. 获取Method的所有参数，将参数名称和参数的位置保存到map容器中

8. 将Controller实例、Method对象、url对应的Pattern和参数位置信息保存到Handle中，再将Handle保存到HandleMapping（Handle的List集合）中

## 调用阶段

9. doDispatch方法获取到请求
10. 获取请求url，根据此url到HandleMapping中查询，若结果为空则为404错误
11. 从查询结果的Handle中获取参数顺序映射关系，根据此关系获取请求参数的值，并将其转换成为指定类型的值后存入到方法的请求参数值得数组里面
12. doDispatch方法反射调用请求方法将返回值输出response中