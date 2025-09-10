# @ConditionalOnBooleanProperty Usage Guide

## Overview

The `@ConditionalOnBooleanProperty` annotation is a new feature introduced in Spring Boot 3.5 that provides a more convenient way to conditionally create beans based on boolean properties. This annotation simplifies the common pattern of using `@ConditionalOnProperty` with boolean values.

## Basic Usage

### Simple Boolean Condition

```java
@Bean
@ConditionalOnBooleanProperty(name = "feature.enabled", havingValue = true)
public FeatureService enabledFeatureService() {
    return new EnabledFeatureService();
}
```

This bean will be created only when `feature.enabled=true` is set in the application properties.

### Default Behavior with matchIfMissing

```java
@Bean
@ConditionalOnBooleanProperty(name = "feature.caching", havingValue = true, matchIfMissing = true)
public CacheService cacheService() {
    return new CacheService();
}
```

This bean will be created when:
- `feature.caching=true` is explicitly set, OR
- The property is not defined at all (matchIfMissing = true)

### Mutually Exclusive Beans

```java
@Configuration
public class ValidationConfiguration {

    @Bean
    @ConditionalOnBooleanProperty(name = "validation.enhanced", havingValue = true)
    public ValidationService enhancedValidationService() {
        return new EnhancedValidationService();
    }

    @Bean
    @ConditionalOnBooleanProperty(name = "validation.enhanced", havingValue = false, matchIfMissing = true)
    public ValidationService basicValidationService() {
        return new BasicValidationService();
    }
}
```

This pattern ensures that exactly one validation service is always available.

## Comparison with @ConditionalOnProperty

### Before (Spring Boot < 3.5)

```java
@Bean
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
public FeatureService featureService() {
    return new FeatureService();
}
```

### After (Spring Boot 3.5+)

```java
@Bean
@ConditionalOnBooleanProperty(name = "feature.enabled", havingValue = true)
public FeatureService featureService() {
    return new FeatureService();
}
```

## Benefits

1. **Type Safety**: No need to use string values for boolean properties
2. **Cleaner Code**: More explicit intent when dealing with boolean conditions
3. **Better IDE Support**: IDEs can provide better autocomplete and validation
4. **Reduced Errors**: Eliminates common mistakes like using "True" instead of "true"

## Common Patterns

### Feature Toggles

```java
@Bean
@ConditionalOnBooleanProperty(name = "features.new-ui", havingValue = true)
public UIService newUIService() {
    return new NewUIService();
}

@Bean
@ConditionalOnBooleanProperty(name = "features.new-ui", havingValue = false, matchIfMissing = true)
public UIService legacyUIService() {
    return new LegacyUIService();
}
```

### Environment-Specific Services

```java
@Bean
@ConditionalOnBooleanProperty(name = "app.debug-mode", havingValue = false, matchIfMissing = true)
public LoggingService productionLoggingService() {
    return new ProductionLoggingService();
}

@Bean
@ConditionalOnBooleanProperty(name = "app.debug-mode", havingValue = true)
public LoggingService debugLoggingService() {
    return new DebugLoggingService();
}
```

### Optional Features

```java
@Bean
@ConditionalOnBooleanProperty(name = "monitoring.metrics", havingValue = true)
public MetricsCollector metricsCollector() {
    return new MetricsCollector();
}

@Bean
@ConditionalOnBooleanProperty(name = "monitoring.health-checks", havingValue = true)
public HealthCheckService healthCheckService() {
    return new HealthCheckService();
}
```

## Best Practices

1. **Use Descriptive Property Names**: Choose property names that clearly indicate their purpose
2. **Document Default Behavior**: Always document whether `matchIfMissing = true` is used
3. **Group Related Properties**: Use consistent naming conventions for related features
4. **Provide Fallbacks**: When using mutually exclusive beans, ensure one is always available
5. **Test All Scenarios**: Test with property present (true/false) and absent

## Testing

Always test your conditional beans with different property configurations:

```java
@SpringBootTest
@TestPropertySource(properties = "feature.enabled=true")
class FeatureEnabledTest {
    // Test when feature is enabled
}

@SpringBootTest
@TestPropertySource(properties = "feature.enabled=false")
class FeatureDisabledTest {
    // Test when feature is disabled
}

@SpringBootTest
class FeatureDefaultTest {
    // Test default behavior (no property set)
}
```

## Migration from @ConditionalOnProperty

When migrating from `@ConditionalOnProperty` to `@ConditionalOnBooleanProperty`:

1. Change string values to boolean values
2. Update property files to use `true`/`false` instead of string representations
3. Update tests to use boolean values in `@TestPropertySource`
4. Verify that `matchIfMissing` behavior is consistent with your expectations