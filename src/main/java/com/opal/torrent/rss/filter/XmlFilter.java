package com.opal.torrent.rss.filter;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

@Component
public class XmlFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest) || !(((HttpServletRequest) request).getRequestURI().endsWith("feed"))) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletResponse r = (HttpServletResponse) response;
        HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(r) {
            @Override
            public ServletOutputStream getOutputStream() throws java.io.IOException {
                ServletResponse response = this.getResponse();
                response.setContentType("text/xml; charset=utf-8");
                return super.getOutputStream();
            }
        };
        chain.doFilter(request, wrappedResponse);
    }
}
