package com.lagou.utils;

import java.util.function.Predicate;

public final class CollectionUtils {

    public static <T> boolean contains(Iterable<T> collection, Predicate<T> predicate) {
        for (T t: collection) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    public static <T> T getSingleOrDefault(Iterable<T> collection, Predicate<T> predicate) {
        for (T t: collection) {
            if (predicate.test(t)) {
                return t;
            }
        }
        return null;
    }

}
