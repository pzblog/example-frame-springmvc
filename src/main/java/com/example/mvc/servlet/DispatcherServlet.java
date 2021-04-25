package com.example.mvc.servlet;

import com.example.mvc.annotation.Autowired;
import com.example.mvc.annotation.Controller;
import com.example.mvc.annotation.RequestMapping;
import com.example.mvc.annotation.Service;
import com.example.mvc.common.PackageHelper;
import com.example.mvc.common.RequestHandler;
import com.example.mvc.common.RequestParamHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * servlet跳转层
 * @author panzhi
 * @version v1.0, 2018.8.7
 */
@WebServlet(name = "DispatcherServlet",urlPatterns = "/*", loadOnStartup = 1, initParams = {@WebInitParam(name="scanPackage", value="com.example.mvc")})
public class DispatcherServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(DispatcherServlet.class);

    /**请求方法映射容器*/
    private static List<RequestHandler> handlerMapping = new ArrayList<>();

    /**
     * 服务启动的时候，进行初始化，流程如下：
     * 1、扫描指定包下所有的类
     * 2、通过反射将类实例，放入ioc容器
     * 3、通过Autowired注解，实现自动依赖注入，也就是set类中的属性
     * 4、通过RequestMapping注解，获取需要映射的所有方法，然后将类信息存放到容器中
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            //1、扫描指定包下所有的类
            String scanPackage = config.getInitParameter("scanPackage");
            //1、扫描指定包下所有的类
            List<String> classNames = doScan(scanPackage);
            //2、初始化所有类实例，放入ioc容器，也就是map对象中
            Map<String, Object> iocMap = doInstance(classNames);
            //3、实现自动依赖注入
            doAutowired(iocMap);
            //5、初始化方法mapping
            initHandleMapping(iocMap);
        } catch (Exception e) {
            logger.error("dispatcher-servlet类初始化失败!",e);
            throw new ServletException(e.getMessage());
        }
    }


    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //跳转
        doDispatch(request, response);
    }

    /**
     * 扫描指定包下的类文件
     * @param packageName
     * @return
     */
    private List<String> doScan(String packageName){
        if(StringUtils.isBlank(packageName)){
            throw new RuntimeException("mvc配置文件中指定扫描包名为空!");
        }
        return PackageHelper.getClassName(packageName);
    }

    private Map<String, Object> doInstance(List<String> classNames) {
        Map<String, Object> iocMap = new HashMap<>();
        if(!CollectionUtils.isNotEmpty(classNames)){
            throw new RuntimeException("获取的类为空!");
        }
        for (String className : classNames) {
            try {
                //通过反射机制构造对象
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(Controller.class)){
                    //将类名第一个字母小写
                    String baneName = firstLowerCase(clazz.getSimpleName());
                    iocMap.put(baneName, clazz.newInstance());
                }else if(clazz.isAnnotationPresent(Service.class)){
                    //服务层注解判断
                    Service service = clazz.getAnnotation(Service.class);
                    String beanName = service.value();
                    //如果该注解上没有自定义类名，则默认首字母小写
                    if(StringUtils.isBlank(beanName)){
                        beanName = clazz.getName();
                    }
                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    //如果注入的是接口，可以巧妙的用接口的类型作为key
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> clazzInterface : interfaces) {
                        iocMap.put(clazzInterface.getName(), instance);
                    }
                }
            } catch (Exception e) {
                logger.error("初始化mvc-ioc容器失败!",e);
                throw new RuntimeException("初始化mvc-ioc容器失败!");
            }
        }
        return iocMap;
    }

    /**
     * 实现自动依赖注入
     * @throws Exception
     */
    private void doAutowired(Map<String, Object> iocMap) {
        if(!MapUtils.isNotEmpty(iocMap)){
            throw new RuntimeException("初始化实现自动依赖失败，ioc为空!");
        }
        for(Map.Entry<String, Object> entry : iocMap.entrySet()){
            //获取对象下所有的属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                //判断字段上有没有@Autowried注解，有的话才注入
                if(field.isAnnotationPresent(Autowired.class)){
                    try {
                        Autowired autowired = field.getAnnotation(Autowired.class);
                        //获取注解上有没有自定义值
                        String beanName = autowired.value().trim();
                        if(StringUtils.isBlank(beanName)){
                            beanName = field.getType().getName();
                        }
                        //如果想要访问到私有的属性，我们要强制授权
                        field.setAccessible(true);
                        field.set(entry.getValue(), iocMap.get(beanName));
                    } catch (Exception e) {
                        logger.error("初始化实现自动依赖注入失败!",e);
                        throw new RuntimeException("初始化实现自动依赖注入失败");
                    }
                }
            }
        }
    }

    /**
     * 初始化方法mapping
     */
    private void initHandleMapping(Map<String, Object> iocMap){
        if(!MapUtils.isNotEmpty(iocMap)){
            throw new RuntimeException("初始化实现自动依赖失败，ioc为空");
        }
        for(Map.Entry<String, Object> entry:iocMap.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            //判断是否是controller层
            if(!clazz.isAnnotationPresent(Controller.class)){
                continue;
            }
            String baseUrl = null;
            //判断类有没有requestMapping注解
            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl= requestMapping.value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                //判断方法上有没有requestMapping
                if(!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }
                RequestMapping requestMethodMapping = method.getAnnotation(RequestMapping.class);
                //"/+",表示将多个"/"转换成"/"
                String regex = (baseUrl + requestMethodMapping.value()).replaceAll("/+", "/");
                Pattern pattern = Pattern.compile(regex);
                handlerMapping.add(new RequestHandler(pattern, entry.getValue(), method));
            }
        }
    }

    /**
     * servlet请求跳转
     * @param request
     * @param response
     * @throws IOException
     */
    private void doDispatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            request.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", -1);
            response.setContentType("text/html");
            response.setHeader("content-type", "text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            RequestHandler handle = getHandleMapping(request);
            if(Objects.isNull(handle)){
                //异常请求地址
                logger.warn("异常请求地址!地址:" + request.getRequestURI());
                response.getWriter().append("error request url");
                return;
            }
            //获取参数列表
            Object[] paramValues = RequestParamHelper.buildRequestParam(handle, request, response);
            Object result = handle.getMethod().invoke(handle.getController(), paramValues);
            if(result != null){
                PrintWriter out = response.getWriter();
                out.println(result);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            logger.error("接口请求失败!",e);
            PrintWriter out = response.getWriter();
            out.println("请求异常,请稍后再试");
            out.flush();
            out.close();
        }
    }

    /**
     * 将类名第一个字母小写
     * @param clazzName
     * @return
     */
    private String firstLowerCase(String clazzName){
        char[] chars = clazzName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }


    /**
     * 获取用户请求方法名
     * 与handlerMapping中的路径名进行匹配
     * @param request
     * @return
     */
    private RequestHandler getHandleMapping(HttpServletRequest request){
        if(CollectionUtils.isNotEmpty(handlerMapping)){
            //获取用户请求路径
            String url = request.getRequestURI();
            String contextPath = request.getContextPath();
            String serviceUrl = url.replace(contextPath, "").replaceAll("/+", "/");
            for (RequestHandler handle : handlerMapping) {
                //正则匹配请求方法名
                Matcher matcher = handle.getPattern().matcher(serviceUrl);
                if(matcher.matches()){
                    return handle;
                }
            }
        }
        return null;
    }

}

