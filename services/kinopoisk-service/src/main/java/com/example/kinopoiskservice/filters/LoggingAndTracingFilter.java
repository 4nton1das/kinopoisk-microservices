package com.example.kinopoiskservice.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

// @Component
@Order(1) // Высокий приоритет - выполняется первым
public class LoggingAndTracingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LoggingAndTracingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Управление Correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }

        // Помещаем ID в MDC для обогащения всех логов
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

        // Добавляем ID в заголовок ответа
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();

        try {
            // Логируем входящий запрос (только для API endpoints)
            if (request.getRequestURI().startsWith("/api/")) {
                log.info("Request started: {} {}", request.getMethod(), request.getRequestURI());
            }

            // Передаем управление дальше по цепочке
            filterChain.doFilter(request, response);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Логируем завершение запроса (только для API endpoints)
            if (request.getRequestURI().startsWith("/api/")) {
                log.info("Request finished: {} {} with status {} in {}ms",
                        request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            }

            // Критически важно! Очищаем MDC
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
