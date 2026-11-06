package io.swagger.v3.parser.processors;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

import io.swagger.v3.parser.ResolverCache;
import org.slf4j.Logger;

/**
 * Allocates unique names for resolved OpenAPI components.
 *
 * <p>The caller supplies the policies which are specific to a component type:
 * key lookup, reference equivalence, and reuse of an already resolved value.</p>
 */
final class ComponentNameAllocator {

    private final Logger logger;
    private final ResolverCache cache;

    ComponentNameAllocator(Logger logger, ResolverCache cache) {
        this.logger = logger;
        this.cache = cache;
    }

    <T> String allocate(Map<String, T> components, String baseName,
            T incoming, String incomingRef, Function<T, String> refOf) {
        return allocate(components, baseName, incoming, incomingRef, refOf,
                name -> components.containsKey(name) ? name : null,
                (existing, ignored) -> false);
    }

    <T> String allocate(Map<String, T> components, String baseName,
            T incoming, String incomingRef, Function<T, String> refOf,
            Function<String, String> keyOf, BiPredicate<T, T> reusePolicy) {
        for (int suffix = 0; ; suffix++) {
            String candidate = suffix == 0 ? baseName : baseName + "_" + suffix;
            String existingKey = keyOf.apply(candidate);
            if (existingKey == null) {
                return candidate;
            }
            if (canReuse(components.get(existingKey), incoming, incomingRef, refOf, reusePolicy)) {
                return existingKey;
            }
            logger.debug("A different component already claims the name {}", existingKey);
        }
    }

    private <T> boolean canReuse(T existing, T incoming, String incomingRef,
            Function<T, String> refOf, BiPredicate<T, T> reusePolicy) {
        String existingRef = existing == null ? null : refOf.apply(existing);
        if (existingRef != null) {
            return cache.refsAreEquivalent(existingRef, incomingRef);
        }
        return Objects.equals(incoming, existing) || reusePolicy.test(existing, incoming);
    }

    static <T> Function<String, String> caseInsensitiveKey(Map<String, T> components) {
        return candidate -> {
            if (components.containsKey(candidate)) {
                return candidate;
            }
            return components.keySet().stream()
                    .filter(name -> name.equalsIgnoreCase(candidate))
                    .findFirst()
                    .orElse(null);
        };
    }
}
