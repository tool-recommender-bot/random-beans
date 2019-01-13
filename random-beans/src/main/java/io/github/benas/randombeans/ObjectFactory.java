/**
 * The MIT License
 *
 *   Copyright (c) 2017, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package io.github.benas.randombeans;

import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;

import io.github.benas.randombeans.api.ObjectGenerationException;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.SynchronousQueue;

import static io.github.benas.randombeans.util.CollectionUtils.randomElementOf;
import static io.github.benas.randombeans.util.ReflectionUtils.getPublicConcreteSubTypesOf;
import static io.github.benas.randombeans.util.ReflectionUtils.isAbstract;

/**
 * Factory to create "fancy" objects: immutable beans, generic types, abstract and interface types.
 * Encapsulates the logic of type introspection, classpath scanning for abstract types, etc
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
@SuppressWarnings({"unchecked"})
class ObjectFactory {

    private final Objenesis objenesis = new ObjenesisStd();

    private boolean scanClasspathForConcreteTypes;
    private boolean enforceJavaBeanConventions;

    <T> T createInstance(final Class<T> type) {
        if (scanClasspathForConcreteTypes && isAbstract(type)) {
            Class<?> randomConcreteSubType = randomElementOf(getPublicConcreteSubTypesOf((type)));
            if (randomConcreteSubType == null) {
                throw new InstantiationError("Unable to find a matching concrete subtype of type: " + type + " in the classpath");
            } else {
                return (T) createNewInstance(randomConcreteSubType);
            }
        } else {
            try {
                return createNewInstance(type);
            } catch (Error e) {
                throw new ObjectGenerationException("Unable to create an instance of type: " + type, e);
            }
        }
    }

    // TODO refactor this method
    private <T> T createNewInstance(final Class<T> type) {
            Constructor<T> noArgConstructor = getDefaultConstructor(type);
            if (noArgConstructor == null) {
                if (enforceJavaBeanConventions) {
                    throw new RuntimeException(String.format("Type %s does not provide a public default constructor", type));
                } else {
                    return objenesis.newInstance(type);
                }
            } else {
                if (!noArgConstructor.isAccessible()) {
                    if (enforceJavaBeanConventions) {
                        throw new RuntimeException(String.format("Type %s does not provide a public default constructor", type));
                    } else {
                        noArgConstructor.setAccessible(true);
                        try {
                            return noArgConstructor.newInstance();
                        } catch (Exception e) {
                            return objenesis.newInstance(type);
                        }
                    }
                }
            }
        return null;
    }

    private <T> Constructor<T> getDefaultConstructor(final Class<T> type) {
        try {
            return type.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    Collection<?> createEmptyCollectionForType(Class<?> fieldType, int initialSize) {
        rejectUnsupportedTypes(fieldType);
        Collection<?> collection;
        try {
            collection = (Collection<?>) fieldType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            // Creating an ArrayBlockingQueue with objenesis by-passes the constructor.
            // This leads to inconsistent state of the collection (locks are not initialized) that causes NPE at elements insertion time..
            if (fieldType.equals(ArrayBlockingQueue.class)) {
                collection = new ArrayBlockingQueue<>(initialSize);
            } else {
                collection = (Collection<?>) objenesis.newInstance(fieldType);
            }
        }
        return collection;
    }

    void setScanClasspathForConcreteTypes(boolean scanClasspathForConcreteTypes) {
        this.scanClasspathForConcreteTypes = scanClasspathForConcreteTypes;
    }

    void setEnforceJavaBeanConventions(boolean enforceJavaBeanConventions) {
        this.enforceJavaBeanConventions = enforceJavaBeanConventions;
    }

    private void rejectUnsupportedTypes(Class<?> type) {
        if (type.equals(SynchronousQueue.class)) {
            // SynchronousQueue is not supported since it requires a consuming thread at insertion time
            throw new UnsupportedOperationException(SynchronousQueue.class.getName() + " type is not supported");
        }
        if (type.equals(DelayQueue.class)) {
            // DelayQueue is not supported since it requires creating dummy delayed objects
            throw new UnsupportedOperationException(DelayQueue.class.getName() + " type is not supported");
        }
    }
}
