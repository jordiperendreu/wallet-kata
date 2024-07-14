package com.playtomic.tests.wallet.infrastructure;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
public class TrailingSlashFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String requestURI = req.getRequestURI();
        if (requestURI.endsWith("/")) {
            requestURI = requestURI.substring(0, requestURI.length() - 1);
        }
        String finalRequestURI = requestURI;
        chain.doFilter(new HttpServletRequestWrapper(req) {
            @Override
            public String getRequestURI() {
                return finalRequestURI;
            }
            @Override
            public StringBuffer getRequestURL() {
                return new StringBuffer(super.getRequestURL()).deleteCharAt(super.getRequestURL().length() - 1);
            }
        }, response);
    }
}
