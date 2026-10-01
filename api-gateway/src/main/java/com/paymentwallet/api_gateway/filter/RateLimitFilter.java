package com.paymentwallet.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS = 100;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, ClientRequestInfo>
            clients = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {

        String clientIp = getClientIp(exchange);

        ClientRequestInfo requestInfo =
                clients.compute(
                        clientIp,
                        (key, existing) -> {

                            long now =
                                    Instant.now().getEpochSecond();

                            if (existing == null ||
                                    now - existing.windowStart
                                            >= WINDOW_SECONDS) {

                                return new ClientRequestInfo(
                                        now,
                                        new AtomicInteger(1)
                                );
                            }

                            existing.requestCount
                                    .incrementAndGet();

                            return existing;
                        }
                );

        if (requestInfo.requestCount.get()
                > MAX_REQUESTS) {

            exchange.getResponse()
                    .setStatusCode(
                            HttpStatus.TOO_MANY_REQUESTS
                    );

            return exchange.getResponse()
                    .setComplete();
        }

        return chain.filter(exchange);
    }

    private String getClientIp(
            ServerWebExchange exchange
    ) {

        if (exchange.getRequest()
                .getRemoteAddress() == null) {
            return "unknown";
        }

        return exchange.getRequest()
                .getRemoteAddress()
                .getAddress()
                .getHostAddress();
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private static class ClientRequestInfo {

        private final long windowStart;
        private final AtomicInteger requestCount;

        private ClientRequestInfo(
                long windowStart,
                AtomicInteger requestCount
        ) {
            this.windowStart = windowStart;
            this.requestCount = requestCount;
        }
    }
}