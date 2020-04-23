package per.nonlone.framework.filter;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 重新包裹下请求，解决在拦截器下失败的问题
 */
public class RepeatedReadRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    private boolean isWrapped;

    public RepeatedReadRequestWrapper(HttpServletRequest request) throws IOException {
        this(request, StandardCharsets.UTF_8);
    }

    public RepeatedReadRequestWrapper(HttpServletRequest request, Charset charset) throws IOException {
        this(request, charset.name());
    }


    public RepeatedReadRequestWrapper(HttpServletRequest request, String charSet) throws IOException {
        super(request);
        MediaType mediaType = MediaType.valueOf(request.getContentType());
        if (MediaType.APPLICATION_JSON.equals(mediaType)
                || MediaType.APPLICATION_JSON_UTF8.equals(mediaType)) {
            body = readBytes(request.getReader(), charSet);
            isWrapped = true;
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (isWrapped) {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        } else {
            return super.getReader();
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (isWrapped && body != null) {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {

                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener listener) {

                }

                @Override
                public int read() throws IOException {
                    return bais.read();
                }
            };
        } else {
            return super.getInputStream();
        }
    }

    /**
     * 通过BufferedReader和字符编码集转换成byte数组
     *
     * @param br
     * @param encoding
     * @return
     * @throws IOException
     */
    private byte[] readBytes(BufferedReader br, String encoding) throws IOException {
        String str = null, retStr = "";
        while ((str = br.readLine()) != null) {
            retStr += str;
        }
        if (StringUtils.isNotBlank(retStr)) {
            return retStr.getBytes(Charset.forName(encoding));
        }
        return new byte[0];
    }
}