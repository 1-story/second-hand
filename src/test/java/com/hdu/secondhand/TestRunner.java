package com.hdu.secondhand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 离线环境手动测试运行器（本机仓库缺少 surefire-junit-platform 与
 * junit-platform-launcher，无法用 mvn test 自动跑，故提供本运行器）：
 *
 * 用法（在有网/标准环境请优先使用 mvn test）：
 *   java -cp <test-classpath> com.hdu.secondhand.TestRunner
 *
 * 行为与 JUnit 5 一致：每个测试方法使用独立实例、独立 Mockito 注解
 * 初始化，并执行 @BeforeEach。
 */
public class TestRunner {

    public static void main(String[] args) throws Exception {
        List<Class<?>> testClasses = List.of(
                Class.forName("com.hdu.secondhand.ai.rules.ValuationRuleEngineTest"),
                Class.forName("com.hdu.secondhand.service.ProductServiceImplTest"),
                Class.forName("com.hdu.secondhand.service.AiPublishServiceImplTest"),
                Class.forName("com.hdu.secondhand.service.AiEstimateServiceImplTest")
        );

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (Class<?> clazz : testClasses) {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            List<Method> beforeEachMethods = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();
            for (Method m : clazz.getDeclaredMethods()) {
                m.setAccessible(true);
                if (m.isAnnotationPresent(BeforeEach.class)) {
                    beforeEachMethods.add(m);
                } else if (m.isAnnotationPresent(Test.class)) {
                    testMethods.add(m);
                }
            }

            for (Method test : testMethods) {
                Object instance = ctor.newInstance();
                try (AutoCloseable ignored = MockitoAnnotations.openMocks(instance)) {
                    for (Method b : beforeEachMethods) {
                        b.invoke(instance);
                    }
                    try {
                        test.invoke(instance);
                        passed++;
                        System.out.println("PASS  " + clazz.getSimpleName() + "." + test.getName());
                    } catch (Throwable t) {
                        failed++;
                        Throwable cause = t.getCause() != null ? t.getCause() : t;
                        failures.add(clazz.getSimpleName() + "." + test.getName() + " -> " + cause);
                        System.out.println("FAIL  " + clazz.getSimpleName() + "." + test.getName() + " -> " + cause);
                    }
                }
            }
        }

        System.out.println("==========================================");
        System.out.println("TOTAL: passed=" + passed + ", failed=" + failed);
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  " + f));
            System.exit(1);
        }
        System.out.println("ALL TESTS PASSED");
    }
}
