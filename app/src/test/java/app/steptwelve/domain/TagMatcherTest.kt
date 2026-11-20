package app.steptwelve.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TagMatcherTest {

    @Test fun `matches whole token case-insensitively`() {
        assertTrue(TagMatcher.matches("John AA", "aa"))
        assertTrue(TagMatcher.matches("john aa", "AA"))
        assertTrue(TagMatcher.matches("AA John", "aa"))
    }

    @Test fun `does not match inside a longer word`() {
        assertFalse(TagMatcher.matches("Aaron", "aa"))
        assertFalse(TagMatcher.matches("Aaron Smith", "aa"))
        assertFalse(TagMatcher.matches("Isaac", "saa"))
        assertFalse(TagMatcher.matches("Baa", "aa"))
    }

    @Test fun `splits on punctuation`() {
        assertTrue(TagMatcher.matches("John (AA)", "aa"))
        assertTrue(TagMatcher.matches("John [SAA]", "saa"))
        assertTrue(TagMatcher.matches("Jane-newcomer", "newcomer"))
        assertTrue(TagMatcher.matches("Jane_newcomer", "newcomer"))
        assertTrue(TagMatcher.matches("Jane/AA", "aa"))
        assertTrue(TagMatcher.matches("Jane.AA", "aa"))
        assertTrue(TagMatcher.matches("Jane, AA", "aa"))
        assertTrue(TagMatcher.matches("Jane|AA", "aa"))
    }

    @Test fun `normalizes spacing and dashes in multi-word tags`() {
        assertTrue(TagMatcher.matches("Bob 12step", "12step"))
        assertTrue(TagMatcher.matches("Bob 12 step", "12step"))
        assertTrue(TagMatcher.matches("Bob 12-Step", "12step"))
        assertTrue(TagMatcher.matches("Bob (12 - step)", "12step"))
        assertTrue(TagMatcher.matches("Bob 12step", "12-step"))
    }

    @Test fun `multi-token match still respects token boundaries`() {
        assertFalse(TagMatcher.matches("Bob 112 step", "12step"))
        assertFalse(TagMatcher.matches("Bob 12 steps", "12step"))
        assertFalse(TagMatcher.matches("Bob 12steps", "12step"))
    }

    @Test fun `empty or punctuation-only tag never matches`() {
        assertFalse(TagMatcher.matches("John AA", ""))
        assertFalse(TagMatcher.matches("John AA", " - "))
        assertFalse(TagMatcher.matches("", "aa"))
    }

    @Test fun `matching tags returns every match in given order`() {
        val tags = listOf("12step", "saa", "aa", "newcomer")
        assertEquals(listOf("aa", "newcomer"), TagMatcher.matchingTags("Sam (AA newcomer)", tags))
        assertEquals(emptyList<String>(), TagMatcher.matchingTags("Aaron Isaacs", tags))
    }

    @Test fun `normalize tag`() {
        assertEquals("12step", TagMatcher.normalizeTag(" 12-Step "))
        assertEquals("saa", TagMatcher.normalizeTag("SAA"))
    }
}
