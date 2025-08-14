package works.weave.socks.shipping.middleware;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.TimeUnit;


public class HTTPMonitoringInterceptor implements HandlerInterceptor {

    private final MeterRegistry meterRegistry;
    private static final String START_TIME = "startTime";
    private final Counter requestCounter;
    private final Counter errorCounter;

    public HTTPMonitoringInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialisation des compteurs
        this.requestCounter = Counter.builder("http_requests_total")
                .description("Total HTTP requests")
                .tag("component", "http_interceptor")
                .register(meterRegistry);
                
        this.errorCounter = Counter.builder("http_errors_total")
                .description("Total HTTP error responses")
                .tag("component", "http_interceptor")
                .register(meterRegistry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.nanoTime());
        requestCounter.increment(); // Incrémente le compteur de requêtes
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        Long start = (Long) request.getAttribute(START_TIME);
        if (start != null) {
            long elapsed = System.nanoTime() - start;

            // Enregistrement du temps de réponse
            Timer.builder("http_server_requests_custom")
                    .description("Custom HTTP request latency")
                    .tags("method", request.getMethod(),
                          "uri", request.getRequestURI(),
                          "status", String.valueOf(response.getStatus()),
                          "host", request.getServerName())
                    .register(meterRegistry)
                    .record(elapsed, TimeUnit.NANOSECONDS);

            // Comptage des erreurs (4xx et 5xx)
            if (response.getStatus() >= 400) {
                errorCounter.increment();
            }
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Vous pourriez ajouter un traitement supplémentaire ici si nécessaire
        // Par exemple, suivre les exceptions non attrapées
        if (ex != null) {
            meterRegistry.counter("http_exceptions_total",
                    "exception", ex.getClass().getSimpleName(),
                    "uri", request.getRequestURI())
                .increment();
        }
    }
}