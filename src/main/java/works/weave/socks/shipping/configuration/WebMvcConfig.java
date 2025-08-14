package works.weave.socks.shipping.configuration;
import io.micrometer.core.instrument.MeterRegistry;
import works.weave.socks.shipping.middleware.HTTPMonitoringInterceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.MappedInterceptor;

@Configuration
public class WebMvcConfig {

    @Bean
    public HTTPMonitoringInterceptor httpMonitoringInterceptor(MeterRegistry meterRegistry) {
        return new HTTPMonitoringInterceptor(meterRegistry);
    }

    @Bean
    public MappedInterceptor myMappedInterceptor(HTTPMonitoringInterceptor interceptor) {
        return new MappedInterceptor(new String[]{"/**"}, interceptor);
    }
}