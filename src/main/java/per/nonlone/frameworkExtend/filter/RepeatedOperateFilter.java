package per.nonlone.frameworkExtend.filter;

import com.google.common.collect.ImmutableMultiset;
import org.springframework.web.filter.OncePerRequestFilter;
import per.nonlone.utilsExtend.CollectionUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

public class RepeatedOperateFilter extends OncePerRequestFilter {

    private String contextPath;

    private Set<String> excludeUrlPrefix;

    public RepeatedOperateFilter() {
    }

    public RepeatedOperateFilter(String contextPath, String... excludeUrlPrefix) {
        this.contextPath = contextPath;
        this.excludeUrlPrefix = ImmutableMultiset.<String>copyOf(excludeUrlPrefix).elementSet();
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (isAsyncDispatch(request) || checkURIExclude(request.getRequestURI())) {
            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(new RepeatedReadRequestWrapper(request), new RepeatedReadResponseWrapper(response));
        }
    }

    private boolean checkURIExclude(String uri) {
        if (CollectionUtils.isNotEmpty(excludeUrlPrefix)) {
            String realURL = uri.replace(contextPath, "");
            for (String urlPrefix : excludeUrlPrefix) {
                if (realURL.startsWith(urlPrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

}


