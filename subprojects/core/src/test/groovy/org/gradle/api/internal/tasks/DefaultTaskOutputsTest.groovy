/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.api.internal.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.internal.TaskInputsInternal
import org.gradle.api.internal.TaskInternal
import org.gradle.api.internal.file.TestFiles
import org.gradle.api.internal.tasks.properties.DefaultPropertyWalker
import org.gradle.api.internal.tasks.properties.DefaultTypeMetadataStore
import org.gradle.api.internal.tasks.properties.OutputFilePropertyType
import org.gradle.api.internal.tasks.properties.PropertyValue
import org.gradle.api.internal.tasks.properties.PropertyVisitor
import org.gradle.api.internal.tasks.properties.annotations.NoOpPropertyAnnotationHandler
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.cache.internal.TestCrossBuildInMemoryCacheFactory
import org.gradle.internal.reflect.annotations.impl.DefaultTypeAnnotationMetadataStore
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.UsesNativeServices
import org.junit.Rule
import spock.lang.Issue
import spock.lang.Specification

import java.util.concurrent.Callable

import static org.gradle.internal.file.TreeType.DIRECTORY
import static org.gradle.internal.file.TreeType.FILE

@UsesNativeServices
class DefaultTaskOutputsTest extends Specification {

    @Rule
    final TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    private def taskStatusNagger = Stub(TaskMutator) {
        mutate(_ as String, _ as Runnable) >> { String method, Runnable action ->
            action.run()
        }
        mutate(_ as String, _ as Callable) >> { String method, Callable<?> action ->
            return action.call()
        }
    }
    private final fileCollectionFactory = TestFiles.fileCollectionFactory(temporaryFolder.testDirectory)
    private final taskDependencyFactory = TestFiles.taskDependencyFactory()

    def task = Mock(TaskInternal) {
        getName() >> "task"
        toString() >> "task 'task'"
        getOutputs() >> { outputs }
        getInputs() >> Stub(TaskInputsInternal)
        getDestroyables() >> Stub(TaskDestroyablesInternal)
        getLocalState() >> Stub(TaskLocalStateInternal)
    }

    def cacheFactory = new TestCrossBuildInMemoryCacheFactory()
    def typeAnnotationMetadataStore = new DefaultTypeAnnotationMetadataStore(
        [],
        [:],
        ["java", "groovy"],
        [],
        [Object, GroovyObject],
        [ConfigurableFileCollection, Property],
        [Internal],
        { false },
        cacheFactory
    )
    def walker = new DefaultPropertyWalker(new DefaultTypeMetadataStore([], [new NoOpPropertyAnnotationHandler(Internal)], [], typeAnnotationMetadataStore, cacheFactory))
    def outputs = new DefaultTaskOutputs(task, taskStatusNagger, walker, taskDependencyFactory, fileCollectionFactory)

    void hasNoOutputsByDefault() {
        setup:
        assert outputs.files.files.isEmpty()
        assert !outputs.hasOutput
    }

    void outputFileCollectionIsBuiltByTask() {
        setup:
        assert outputs.files.buildDependencies.getDependencies(task).toList() == [task]
    }

    def "can register output file"() {
        when: outputs.file("a")
        then:
        outputs.files.files == files('a')
        outputs.fileProperties*.propertyName == ['$1']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a")]
        outputs.fileProperties*.outputFile == [file("a")]
        outputs.fileProperties*.outputType == [FILE]
    }

    def "can register output file with property name"() {
        when: outputs.file("a").withPropertyName("prop")
        then:
        outputs.files.files == files('a')
        outputs.fileProperties*.propertyName == ['prop']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a")]
        outputs.fileProperties*.outputFile == [file("a")]
        outputs.fileProperties*.outputType == [FILE]
    }

    def "can register output dir"() {
        when: outputs.dir("a")
        then:
        outputs.files.files == files('a')
        outputs.fileProperties*.propertyName == ['$1']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a")]
        outputs.fileProperties*.outputFile == [file("a")]
        outputs.fileProperties*.outputType == [DIRECTORY]
    }

    def "can register output dir with property name"() {
        when: outputs.dir("a").withPropertyName("prop")
        then:
        outputs.files.files == files('a')
        outputs.fileProperties*.propertyName == ['prop']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a")]
        outputs.fileProperties*.outputFile == [file("a")]
        outputs.fileProperties*.outputType == [DIRECTORY]
    }

    def "cannot register output file with same property name"() {
        outputs.file("a").withPropertyName("alma")
        outputs.file("b").withPropertyName("alma")
        when:
        outputs.fileProperties
        then:
        def ex = thrown IllegalArgumentException
        ex.message == "Multiple output file properties with name 'alma'"
    }

    def "can register unnamed output files"() {
        when: outputs.files("a", "b")
        then:
        outputs.files.files == files('a', "b")
        outputs.fileProperties*.propertyName == ['$1$1', '$1$2']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
    }

    def "can register unnamed output files with property name"() {
        when: outputs.files("a", "b").withPropertyName("prop")
        then:
        outputs.files.files == files('a', "b")
        outputs.fileProperties*.propertyName == ['prop$1', 'prop$2']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
    }

    def "can register named output files"() {
        when: outputs.files("fileA": "a", "fileB": "b")
        then:
        outputs.files.files == files('a', "b")
        outputs.fileProperties*.propertyName == ['$1.fileA', '$1.fileB']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
        outputs.fileProperties*.outputFile == [file("a"), file("b")]
        outputs.fileProperties*.outputType == [FILE, FILE]
    }

    def "can register named #name with property name"() {
        when: outputs."$name"("fileA": "a", "fileB": "b").withPropertyName("prop")
        then:
        outputs.files.files == files('a', "b")
        outputs.fileProperties*.propertyName == ['prop.fileA', 'prop.fileB']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
        outputs.fileProperties*.outputFile == [file("a"), file("b")]
        outputs.fileProperties*.outputType == [type, type]
        where:
        name    | type
        "files" | FILE
        "dirs"  | DIRECTORY
    }

    def "can register future named output #name"() {
        when: outputs."$name"({ [one: "a", two: "b"] })
        then:
        outputs.files.files == files('a', 'b')
        outputs.fileProperties*.propertyName == ['$1.one', '$1.two']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
        outputs.fileProperties*.outputFile == [file("a"), file("b")]
        outputs.fileProperties*.outputType == [type, type]
        where:
        name    | type
        "files" | FILE
        "dirs"  | DIRECTORY
    }

    def "can register future named output #name with property name"() {
        when: outputs."$name"({ [one: "a", two: "b"] }).withPropertyName("prop")
        then:
        outputs.files.files == files('a', "b")
        outputs.fileProperties*.propertyName == ['prop.one', 'prop.two']
        outputs.fileProperties*.propertyFiles*.files.flatten() == [file("a"), file("b")]
        outputs.fileProperties*.outputFile == [file("a"), file("b")]
        outputs.fileProperties*.outputType == [type, type]
        where:
        name    | type
        "files" | FILE
        "dirs"  | DIRECTORY
    }

    def "fails when #name registers mapped file with null key"() {
        when:
        outputs."$name"({ [(null): "a"] }).withPropertyName("prop")
        outputs.fileProperties
        then:
        def ex = thrown IllegalArgumentException
        ex.message == "Mapped output property 'prop' has null key"
        where:
        name    | type
        "files" | FILE
        "dirs"  | DIRECTORY
    }

    @Issue("https://github.com/gradle/gradle/issues/4085")
    def "can register more unnamed properties with method #method after properties have been queried"() {
        outputs."$method"("output-1")
        // Trigger naming properties
        outputs.hasOutput
        outputs."$method"("output-2")
        def names = []

        when:
        outputs.visitRegisteredProperties(new PropertyVisitor.Adapter() {
            @Override
            void visitOutputFileProperty(String propertyName, boolean optional, PropertyValue value, OutputFilePropertyType filePropertyType) {
                names += propertyName
            }
        })
        then:
        names == ['$1', '$2']

        where:
        method << ["file", "dir", "files", "dirs"]
    }

    void canRegisterOutputFiles() {
        when:
        outputs.file('a')

        then:
        outputs.files.files == files('a')
    }

    void hasOutputsWhenEmptyOutputFilesRegistered() {
        when:
        outputs.files([])

        then:
        outputs.hasOutput
    }

    void hasOutputsWhenNonEmptyOutputFilesRegistered() {
        when:
        outputs.file('a')

        then:
        outputs.hasOutput
    }

    void hasOutputsWhenUpToDatePredicateRegistered() {
        when:
        outputs.upToDateWhen { false }

        then:
        outputs.hasOutput
    }

    void canSpecifyUpToDatePredicateUsingClosure() {
        boolean upToDate = false

        when:
        outputs.upToDateWhen { upToDate }

        then:
        !outputs.upToDateSpec.isSatisfiedBy(task)

        when:
        upToDate = true

        then:
        outputs.upToDateSpec.isSatisfiedBy(task)
    }

    void getPreviousFilesFailsWhenNoTaskHistoryAvailable() {
        when:
        outputs.previousOutputFiles

        then:
        def e = thrown(IllegalStateException)
        e.message == 'Task history is currently not available for this task.'
    }

    TestFile file(Object path) {
        temporaryFolder.file(path)
    }

    Set<File> files(Object... paths) {
        paths.collect { file(it) } as Set
    }
}
