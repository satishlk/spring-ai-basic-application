package com.example.checkout.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Stage 5 — application-level metrics. Every checkout outcome and the
 * end-to-end latency of POST /api/checkout/pay show up in Prometheus.
 */
@Component
public class CheckoutMetrics {

    private final Counter paid;
    private final Counter declined;
    private final Counter failed;
    private final Counter timeout;
    private final Counter idempotentHit;
    private final Timer   payLatency;

    public CheckoutMetrics(MeterRegistry registry) {
        this.paid          = Counter.builder("checkout.outcome").tag("result", "paid").register(registry);
        this.declined      = Counter.builder("checkout.outcome").tag("result", "declined").register(registry);
        this.failed        = Counter.builder("checkout.outcome").tag("result", "failed").register(registry);
        this.timeout       = Counter.builder("checkout.outcome").tag("result", "timeout").register(registry);
        this.idempotentHit = Counter.builder("checkout.outcome").tag("result", "idempotent_hit").register(registry);
        this.payLatency    = Timer.builder("checkout.pay.latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public void recordPaid()           { paid.increment(); }
    public void recordDeclined()       { declined.increment(); }
    public void recordFailed()         { failed.increment(); }
    public void recordTimeout()        { timeout.increment(); }
    public void recordIdempotentHit()  { idempotentHit.increment(); }

    public Timer payLatency() { return payLatency; }
}
