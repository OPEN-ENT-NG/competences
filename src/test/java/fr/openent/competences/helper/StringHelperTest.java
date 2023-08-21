package fr.openent.competences.helper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StringHelperTest {

    @Test
    public void testRepeat() {
        String repeated = StringHelper.repeat("ab", 3);
        assertEquals("ababab", repeated);
    }
}