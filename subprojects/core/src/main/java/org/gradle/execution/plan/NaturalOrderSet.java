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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class NaturalOrderSet<E> implements NavigableSet<E>, SortedSet<E> {
    private TreeSet<ElementWithOrder<E>> realSet = new TreeSet<>();
    private Map<E, ElementWithOrder<E>> observer = new HashMap<>();

    @Override
    public E lower(E e) {
        return realSet.lower(observer.get(e)).getElement();
    }

    @Override
    public E floor(E e) {
        return realSet.floor(observer.get(e)).getElement();
    }

    @Override
    public E ceiling(E e) {
        return realSet.ceiling(observer.get(e)).getElement();
    }

    @Override
    public E higher(E e) {
        return realSet.higher(observer.get(e)).getElement();
    }

    @Override
    public E pollFirst() {
        ElementWithOrder<E> ele = realSet.pollFirst();
        observer.remove(ele.getElement());
        return ele.getElement();
    }

    @Override
    public E pollLast() {
        ElementWithOrder<E> ele = realSet.pollLast();
        observer.remove(ele.getElement());
        return ele.getElement();
    }

    @Override
    public int size() {
        return realSet.size();
    }

    @Override
    public boolean isEmpty() {
        return realSet.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return observer.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<ElementWithOrder<E>> iter = realSet.iterator();
        return new Iterator<E>() {

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public E next() {
                return iter.next().getElement();
            }
        };
    }

    @Override
    public Object[] toArray() {
        return realSet.stream().map(e -> e.getElement()).toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new RuntimeException("NaturalOrderSet toArray!!");
    }

    @Override
    public boolean add(E e) {
        if (observer.containsKey(e)) {
            return false;
        }
        ElementWithOrder<E> ele = new ElementWithOrder<>(realSet.size(), e);
        observer.put(e,ele);
        return realSet.add(ele);
    }

    @Override
    public boolean remove(Object o) {
        ElementWithOrder<E> ele = observer.get(o);
        if ( ele == null ) {
            return  false;
        }
        realSet.remove(ele);
        observer.remove(o);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        AtomicBoolean result = new AtomicBoolean(true);
        c.forEach(e -> result.set(result.get() | observer.containsKey(e)));
        return result.get();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        AtomicBoolean result = new AtomicBoolean(true);
        c.forEach(e -> result.set(result.get() | add(e)));
        return result.get();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new RuntimeException("NaturalOrderSet retainAll");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new RuntimeException("NaturalOrderSet removeAll");
    }

    @Override
    public void clear() {
        realSet.clear();
        observer.clear();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new TreeSet<E>(realSet.descendingSet().stream().map(e -> { e.setOrder(realSet.size()-e.getOrder()); return e.getElement();}).collect(Collectors.toSet()));
    }

    @Override
    public Iterator<E> descendingIterator() {
        Iterator<ElementWithOrder<E>> iter = realSet.descendingIterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public E next() {
                return iter.next().getElement();
            }
        };
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new TreeSet<E>( realSet.subSet(observer.get(fromElement),fromInclusive,observer.get(toElement),toInclusive).stream().map(e -> e.getElement()).collect(Collectors.toSet()));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new TreeSet<E>( realSet.headSet(observer.get(toElement),inclusive).stream().map(e -> e.getElement()).collect(Collectors.toSet()));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new TreeSet<E>( realSet.tailSet(observer.get(fromElement),inclusive).stream().map(e -> e.getElement()).collect(Collectors.toSet()));
    }

    @Override
    public Comparator<? super E> comparator() {
        return new Comparator<E>() {
            @Override
            public int compare(E o1, E o2) {
                return observer.get(o1).getOrder()-observer.get(o2).getOrder();
            }
        };
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement,true,toElement,false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement,false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement,true);
    }

    @Override
    public E first() {
        return realSet.first().getElement();
    }

    @Override
    public E last() {
        return realSet.last().getElement();
    }
}
