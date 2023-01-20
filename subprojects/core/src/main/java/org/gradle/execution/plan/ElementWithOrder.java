/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.execution.plan;

import java.util.Comparator;

public class ElementWithOrder<E> implements Comparable<ElementWithOrder<E>>{
    private E element;
    private int order;

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
    @Override
    public int compareTo(ElementWithOrder<E> o) {
        return order - o.order;
    }

    public ElementWithOrder(int pOrder, E pElement) {
        order = pOrder;
        element = pElement;
    }

    public E getElement() {
        return element;
    }
}
