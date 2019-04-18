package per.nonlone.frameworkExtend.servlet;

import org.springframework.web.filter.OncePerRequestFilter;
import per.nonlone.utilsExtend.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Form提交下划线转驼峰过滤器
 */
public class SnakeToCamelFilter extends OncePerRequestFilter {

    private static final char UNDERLINE = '_';

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

        final Map<String, String[]> convertedParamsMap = new ConcurrentHashMap<>();

        httpServletRequest.getParameterMap().forEach((key, value) -> {
            if (key.contains(String.valueOf(UNDERLINE))) {
                String formattedParam = StringUtils.lineToHump(key);
                convertedParamsMap.put(formattedParam, httpServletRequest.getParameterValues(key));
            }
            convertedParamsMap.put(key, value);
        });


        filterChain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
            @Override
            public String getParameter(String name) {
                return convertedParamsMap.containsKey(name) ? convertedParamsMap.get(name)[0] : null;
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return Collections.enumeration(convertedParamsMap.keySet());
            }

            @Override
            public String[] getParameterValues(String name) {
                return convertedParamsMap.get(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return convertedParamsMap;
            }
        }, httpServletResponse);
    }


}
