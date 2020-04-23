package per.nonlone.framework.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.MDC;
import per.nonlone.framework.mdc.MDCDecorator;
import per.nonlone.utils.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

@Slf4j
public class TraceFilter implements Filter {

    public static final String KEY_REFERED_ID = "REFERED_ID";

    public static final String KEY_TRACE_ID = "TRACE_ID";

    public static final String KEY_URL = "URL";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        String uri = null;
        if(request instanceof HttpServletRequest){
            uri = ((HttpServletRequest) request).getRequestURI();
        }
        StopWatch stopWatch = new StopWatch();
        String traceId = ((HttpServletRequest) request).getHeader(StringUtils.lineToHump(KEY_TRACE_ID));
        if (StringUtils.isBlank(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "").toLowerCase();
        } else {
            if (StringUtils.isNotBlank(uri)) {
                log.info(String.format("use traceId from request uri<%s> traceId<%s>", uri, traceId));
            } else {
                log.info(String.format("use traceId from request  traceId<%s>", traceId));
            }
        }
        if(response instanceof HttpServletResponse){
            ((HttpServletResponse)response).setHeader(KEY_TRACE_ID,traceId);
        }

        final String finalUri = uri;
        final String finalTraceId = traceId;

        MDCDecorator.run(new HashMap<String,String>(){{
            this.put(KEY_TRACE_ID,finalTraceId);
            this.put(KEY_URL,finalUri);
        }}, () -> {
            stopWatch.start();
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
            stopWatch.stop();
            // 日志输出
            if (StringUtils.isNotBlank(finalUri)) {
                log.info(String.format("use traceId from request uri<%s> traceId<%s>", finalUri, finalTraceId));
            } else {
                log.info(String.format("use traceId from request  traceId<%s>", finalTraceId));
            }
        });
        MDC.remove(KEY_TRACE_ID);
    }

    @Override
    public void destroy() {

    }
}
