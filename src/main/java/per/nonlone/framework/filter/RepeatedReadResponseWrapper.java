package per.nonlone.framework.filter;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * 重新包裹下请求，解决在拦截器下失败的问题
 */
public class RepeatedReadResponseWrapper extends HttpServletResponseWrapper {

    private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private HttpServletResponse response;

    public RepeatedReadResponseWrapper(HttpServletResponse response) {
        super(response);
        this.response = response;
    }

    public byte[] getBody() {
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public ServletOutputStream getOutputStream() {
        return new ServletOutputStreamWrapper(this.byteArrayOutputStream, this.response);
    }


    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(new OutputStreamWriter(getOutputStream(), this.response.getCharacterEncoding()));
    }

    @Data
    @AllArgsConstructor
    public static class ServletOutputStreamWrapper extends ServletOutputStream {

        private ByteArrayOutputStream byteArrayOutputStream;
        private HttpServletResponse response;

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener listener) {

        }

        @Override
        public void write(int b) throws IOException {
            this.byteArrayOutputStream.write(b);
            this.response.getOutputStream().write(b);
        }

        @Override
        public void flush() throws IOException {
            this.response.getOutputStream().flush();
        }
    }
}