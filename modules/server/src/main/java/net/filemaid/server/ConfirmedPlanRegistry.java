package net.filemaid.server;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.filemaid.core.model.PostProcessPlan;
import net.filemaid.core.model.RenameOperation;
import org.springframework.stereotype.Component;

@Component
public final class ConfirmedPlanRegistry {
    private static final Duration TTL = Duration.ofMinutes(15);
    private final ConcurrentHashMap<UUID, ConfirmedPlan> plans = new ConcurrentHashMap<>();

    public UUID store(String rootId, List<RenameOperation> operations, PostProcessPlan postProcess) {
        plans.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(Instant.now()));
        UUID token = UUID.randomUUID();
        plans.put(token, new ConfirmedPlan(rootId, List.copyOf(operations), postProcess, Instant.now().plus(TTL)));
        return token;
    }

    public ConfirmedPlan consume(UUID token) {
        ConfirmedPlan plan = plans.remove(token);
        if (plan == null || plan.expiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("确认令牌无效或已过期，请重新校验");
        return plan;
    }

    public record ConfirmedPlan(String rootId, List<RenameOperation> operations, PostProcessPlan postProcess, Instant expiresAt) { }
}
