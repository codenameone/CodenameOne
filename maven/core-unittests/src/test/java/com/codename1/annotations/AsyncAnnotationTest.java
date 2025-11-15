package com.codename1.annotations;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncAnnotationTest extends UITestBase {

    @FormTest
    void testScheduleAnnotationMetadata() throws Exception {
        Retention retention = Async.Schedule.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());

        Target target = Async.Schedule.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expected = new ElementType[]{ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER};
        assertArrayEquals(expected, target.value());
    }

    @FormTest
    void testExecuteAnnotationMetadataMatchesSchedule() throws Exception {
        Retention retention = Async.Execute.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.CLASS, retention.value());

        Target target = Async.Execute.class.getAnnotation(Target.class);
        assertNotNull(target);
        ElementType[] expected = new ElementType[]{ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PARAMETER};
        assertArrayEquals(expected, target.value());
    }

    @FormTest
    void testConstructorThrowsAssertion() throws Exception {
        Constructor<Async> constructor = Async.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
            fail("Expected AssertionError");
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof AssertionError);
            assertEquals("Async should not be instantiated", cause.getMessage());
        }
    }
}
