package per.nonlone.framework.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import per.nonlone.utils.CollectionUtils;
import per.nonlone.utils.StringUtils;
import per.nonlone.utils.jackson.JacksonUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * 日志记录Filter
 */
@Slf4j
public class LogFilter extends OncePerRequestFilter {

    public static final String ORIGINAL_PAYLOAD = "originalPayload";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            if ((request instanceof RepeatedReadRequestWrapper)) {
                doFilterWrapped(request, response, filterChain);
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

    protected void doFilterWrapped(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            log.info(getRequestLog(request));
            filterChain.doFilter(request, response);
        } finally {
            log.info(getResponseLog(request, response));
        }
    }

    protected String getRequestLog(HttpServletRequest request) {
        String payLoad = getRequestPayload(request);
        request.setAttribute(ORIGINAL_PAYLOAD,payLoad);
        String logMessage = String.format("|> request url<%s> method<%s> header<%s> parameters<%s>",
                request.getRequestURI(),
                request.getMethod(),
                JacksonUtils.toJSONString(getRequestHeader(request)),
                payLoad);
        return logMessage;
    }

    protected String getResponseLog(HttpServletRequest request, HttpServletResponse response) {
        String body = null;
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            if (outputStream instanceof RepeatedReadResponseWrapper.ServletOutputStreamWrapper) {
                RepeatedReadResponseWrapper.ServletOutputStreamWrapper servletOutputStreamWrapper = (RepeatedReadResponseWrapper.ServletOutputStreamWrapper) outputStream;
                body = new String(servletOutputStreamWrapper.getByteArrayOutputStream().toByteArray());
            }
        } catch (IOException e) {
            log.error(String.format("getResponseLog error %s", e.getMessage()), e);
        }

        return String.format("<| response url<%s> status<%s> header<%s> body<%s>",
                request.getRequestURI(),
                HttpStatus.valueOf(response.getStatus()),
                JacksonUtils.toJSONString(getResponseHeader(response)),
                body);
    }

    private String getRequestPayload(HttpServletRequest request) {
        if(StringUtils.isBlank(request.getContentType())){
            return "";
        }
        MediaType mediaType = MediaType.valueOf(request.getContentType());
        if (MediaType.APPLICATION_JSON.equals(mediaType)
                || MediaType.APPLICATION_JSON_UTF8.equals(mediaType)) {
            // json 结构
            StringBuilder sbPayLoad = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                char[] buff = new char[1024];
                int len;
                while ((len = reader.read(buff)) != -1) {
                    sbPayLoad.append(buff, 0, len);
                }
            } catch (IOException e) {
                log.error("getRequestPayload error", e);
            }
            return sbPayLoad.toString();
        } else if (MediaType.MULTIPART_FORM_DATA.equals(mediaType)
                || MediaType.APPLICATION_FORM_URLENCODED.equals(mediaType)) {
            Map<String, Object> parameters = new HashMap<>();
            Set<String> set = request.getParameterMap().keySet();
            if (CollectionUtils.isNotEmpty(set)) {
                for (String s : set) {
                    String[] values = request.getParameterValues(s);
                    if (values.length == 1) {
                        parameters.put(s, values[0]);
                    } else {
                        parameters.put(s, values);
                    }
                }
            }
            CommonsMultipartResolver commonsMultipartResolver = new CommonsMultipartResolver(request.getServletContext());
            boolean isUploadFile = commonsMultipartResolver.isMultipart(request);
            if (isUploadFile) {
                // 文件上传
                List<Map<String, Object>> mapList = new ArrayList<>();
                MultipartHttpServletRequest multiRequest = (MultipartHttpServletRequest) request;
                Iterator<String> fileNames = multiRequest.getFileNames();
                while (fileNames.hasNext()) {
                    Map<String, Object> map = new HashMap<>();
                    String fileName = fileNames.next();
                    Long fileLength = multiRequest.getFile(fileName).getSize();
                    map.put("fileName", fileName);
                    map.put("fileLength", fileLength);
                    mapList.add(map);
                }
                parameters.put("file", mapList);
            }
            if(CollectionUtils.isNotEmpty(parameters)){
                return JacksonUtils.toJSONString(parameters);
            }
        }
        return null;
    }

    /**
     * 获取请求头部
     *
     * @param request
     * @return
     */
    private Map<String, String> getRequestHeader(HttpServletRequest request) {
        Map<String, String> header = new HashMap<>();
        Enumeration<String> headerKeys = request.getHeaderNames();
        while (headerKeys.hasMoreElements()) {
            String key = headerKeys.nextElement();
            header.put(key, request.getHeader(key));
        }
        return header;
    }

    /**
     * 获取返回头部
     *
     * @param response
     * @return
     */
    private Map<String, String> getResponseHeader(HttpServletResponse response) {
        Map<String, String> header = new HashMap<>();
        Collection<String> headerKeys = response.getHeaderNames();
        for (String headerKey : headerKeys) {
            header.put(headerKey, response.getHeader(headerKey));
        }
        return header;
    }
}
