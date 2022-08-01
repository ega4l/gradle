/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.jvm.toolchain;

import org.gradle.api.Incubating;
import org.gradle.internal.HasInternalProtocol;

/**
 * The build level object/service provided by Gradle which Java Toolchain SPI plugins can access
 * and register their JavaToolchainRepository implementations/build services into. //TODO (#21082): more/better docs
 *
 * @since 7.6
 */
@Incubating
@HasInternalProtocol
public interface JavaToolchainRepositoryRegistry {

    /**
     * TODO (#21082): docs
     *
     */
    <T extends JavaToolchainRepository> void register(String name, Class<T> implementationType);
    //TODO MAJOR (#21082): do we also need a configure action, like we have in BuildServiceRegistry
    //TODO MAJOR (#21082): does this method need to be a "registerIfAbsent" instead

}
