/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.classpath

import org.gradle.util.SetSystemProperties
import org.junit.Rule
import spock.lang.Specification

class InstrumentedTest extends Specification {
    @Rule
    SetSystemProperties systemProperties = new SetSystemProperties()

    def cleanup() {
        Instrumented.discardListener()
    }

    def "notifies listener when system property is used"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "value")

        when:
        def result = Instrumented.systemProperty("prop", "consumer")

        then:
        result == "value"
        1 * listener.systemPropertyQueried("prop", "value", "consumer")
        0 * listener._

        when:
        result = Instrumented.systemProperty("not-set", "consumer")

        then:
        result == null
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when default value for system property is used"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        when:
        def result = Instrumented.systemProperty("not-set", "default", "consumer")

        then:
        result == "default"
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when integer system property is used"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "123")

        when:
        def result = Instrumented.getInteger("prop", "consumer")

        then:
        result == 123
        1 * listener.systemPropertyQueried("prop", "123", "consumer")
        0 * listener._

        System.setProperty("prop", "not an int")

        when:
        result = Instrumented.getInteger("prop", "consumer")

        then:
        result == null
        1 * listener.systemPropertyQueried("prop", "not an int", "consumer")
        0 * listener._

        when:
        result = Instrumented.getInteger("not-set", "consumer")

        then:
        result == null
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when long system property is used"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "123")

        when:
        def result = Instrumented.getLong("prop", "consumer")

        then:
        result == 123
        1 * listener.systemPropertyQueried("prop", "123", "consumer")
        0 * listener._

        when:
        result = Instrumented.getLong("not-set", "consumer")

        then:
        result == null
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when boolean system property is used"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "true")

        when:
        def result = Instrumented.getBoolean("prop", "consumer")

        then:
        result
        1 * listener.systemPropertyQueried("prop", "true", "consumer")
        0 * listener._

        when:
        result = Instrumented.getBoolean("not-set", "consumer")

        then:
        !result
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when system property is used via properties map"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "value")

        when:
        def properties = Instrumented.systemProperties("consumer")
        def result = properties.get("prop")

        then:
        result == "value"
        1 * listener.systemPropertyQueried("prop", "value", "consumer")
        0 * listener._

        when:
        result = properties.getProperty("prop")

        then:
        result == "value"
        1 * listener.systemPropertyQueried("prop", "value", "consumer")
        0 * listener._
    }

    def "notifies listener when default value for system property is used via properties map"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        when:
        def properties = Instrumented.systemProperties("consumer")
        def result = properties.getProperty("not-set", "default")

        then:
        result == "default"
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._

        when:
        result = properties.getOrDefault("not-set", "default")

        then:
        result == "default"
        1 * listener.systemPropertyQueried("not-set", null, "consumer")
        0 * listener._
    }

    def "notifies listener when system properties map is iterated"() {
        def listener = Mock(Instrumented.Listener)
        Instrumented.setListener(listener)

        System.setProperty("prop", "value")

        when:
        Instrumented.systemProperties("consumer").entrySet().forEach { e ->
        }

        then:
        1 * listener.systemPropertyQueried("prop", "value", "consumer")

        when:
        Instrumented.systemProperties("consumer").forEach { k, v ->
        }

        then:
        1 * listener.systemPropertyQueried("prop", "value", "consumer")
    }
}
