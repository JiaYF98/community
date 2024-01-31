package com.nowcoder.community.util;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SensitiveFilterTest {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    void test1() {
        System.out.println(sensitiveFilter.filter("aaa"));
    }

    @Test
    void test2() {
        System.out.println(sensitiveFilter.filter("a*a*a"));
    }

    @Test
    void test3() {
        System.out.println(sensitiveFilter.filter("*a*a*a*"));
    }

    @Test
    void test4() {
        System.out.println(sensitiveFilter.filter("♪a♪a♪a♪"));
    }

    @Test
    void test5() {
        System.out.println(sensitiveFilter.filter("♪a♪a♪a♪b♪b♪b♪c♪c♪c♪"));
    }

    @Test
    void test6() {
        System.out.println(sensitiveFilter.filter("♪a♪a♪a♪a♪b♪b♪b♪b♪c♪c♪c♪c♪"));
    }
}
