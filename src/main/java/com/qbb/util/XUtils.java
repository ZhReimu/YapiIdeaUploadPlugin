package com.qbb.util;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class XUtils {

    /**
     * 为 {@link Enumeration} 类创建一个 Stream
     */
    public static <T> Stream<T> stream(Enumeration<T> enumeration) {
        Iterator<T> iterator = asIterator(enumeration);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }

    /**
     * 为 {@link Enumeration} 类创建一个 Iterator
     */
    public static <T> Iterator<T> asIterator(Enumeration<T> enumeration) {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return enumeration.hasMoreElements();
            }

            @Override
            public T next() {
                return enumeration.nextElement();
            }
        };
    }

}
