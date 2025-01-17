/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.tasks.properties.bean;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.gradle.api.Buildable;
import org.gradle.api.GradleException;
import org.gradle.api.internal.provider.HasConfigurableValueInternal;
import org.gradle.api.internal.tasks.TaskDependencyContainer;
import org.gradle.api.internal.tasks.properties.PropertyValue;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.internal.tasks.properties.TypeMetadata;
import org.gradle.api.internal.tasks.properties.annotations.PropertyAnnotationHandler;
import org.gradle.api.provider.HasConfigurableValue;
import org.gradle.api.provider.Provider;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.deprecation.DeprecationLogger;
import org.gradle.internal.reflect.PropertyMetadata;
import org.gradle.internal.reflect.validation.TypeValidationContext;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

public abstract class AbstractNestedRuntimeBeanNode extends RuntimeBeanNode<Object> {
    protected AbstractNestedRuntimeBeanNode(@Nullable RuntimeBeanNode<?> parentNode, @Nullable String propertyName, Object bean, TypeMetadata typeMetadata) {
        super(parentNode, propertyName, bean, typeMetadata);
    }

    protected void visitProperties(PropertyVisitor visitor, final Queue<RuntimeBeanNode<?>> queue, final RuntimeBeanNodeFactory nodeFactory, TypeValidationContext validationContext) {
        TypeMetadata typeMetadata = getTypeMetadata();
        typeMetadata.visitValidationFailures(getPropertyName(), validationContext);
        for (PropertyMetadata propertyMetadata : typeMetadata.getPropertiesMetadata()) {
            PropertyAnnotationHandler annotationHandler = typeMetadata.getAnnotationHandlerFor(propertyMetadata);
            if (annotationHandler.shouldVisit(visitor)) {
                String propertyName = getQualifiedPropertyName(propertyMetadata.getPropertyName());
                PropertyValue value = new BeanPropertyValue(getBean(), propertyMetadata.getGetterMethod());
                annotationHandler.visitPropertyValue(
                    propertyName,
                    value,
                    propertyMetadata,
                    visitor,
                    (childPropertyName, bean) -> queue.add(nodeFactory.create(AbstractNestedRuntimeBeanNode.this, childPropertyName, bean))
                );
            }
        }
    }

    private static class BeanPropertyValue implements PropertyValue {
        private final Method method;
        private final Object bean;
        private final Supplier<Object> cachedInvoker = Suppliers.memoize(new Supplier<Object>() {
            @Override
            @Nullable
            public Object get() {
                return DeprecationLogger.whileDisabled(() -> {
                    try {
                        return method.invoke(bean);
                    } catch (InvocationTargetException e) {
                        throw UncheckedException.throwAsUncheckedException(e.getCause());
                    } catch (Exception e) {
                        throw new GradleException(String.format("Could not call %s.%s() on %s", method.getDeclaringClass().getSimpleName(), method.getName(), bean), e);
                    }
                });
            }
        });

        public BeanPropertyValue(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
            method.setAccessible(true);
        }

        @Override
        public TaskDependencyContainer getTaskDependencies() {
            if (isProvider()) {
                return (TaskDependencyContainer) cachedInvoker.get();
            }
            if (isBuildable()) {
                return context -> {
                    Object dependency = cachedInvoker.get();
                    if (dependency != null) {
                        context.add(dependency);
                    }
                };
            }
            return TaskDependencyContainer.EMPTY;
        }

        @Override
        public void maybeFinalizeValue() {
            if (isConfigurable()) {
                Object value = cachedInvoker.get();
                ((HasConfigurableValueInternal) value).implicitFinalizeValue();
            }
        }

        private boolean isProvider() {
            return Provider.class.isAssignableFrom(method.getReturnType());
        }

        private boolean isConfigurable() {
            return HasConfigurableValue.class.isAssignableFrom(method.getReturnType());
        }

        private boolean isBuildable() {
            return Buildable.class.isAssignableFrom(method.getReturnType());
        }

        @Nullable
        @Override
        public Object call() {
            return cachedInvoker.get();
        }
    }
}
