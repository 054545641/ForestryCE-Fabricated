package forestry.core.platform.registration;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents a holder of registration declarations. Fabric modules initialize their providers explicitly;
 * this annotation does not trigger loader scanning or class initialization.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FeatureProvider {
}
