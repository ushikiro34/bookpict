package edu.bookpict.aladin.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GradeParserTest {

    @Test
    void parsesFullSemesterNotation() {
        assertEquals("4학년 2학기", GradeParser.detectGradeGroup("", "초등 4학년 2학기 참고서"));
        assertEquals("1학년 1학기", GradeParser.detectGradeGroup("", "중등 1학년 1학기 문제집"));
    }

    @Test
    void parsesShortNotation() {
        assertEquals("4학년 2학기", GradeParser.detectGradeGroup("", "4-2 참고서"));
        assertEquals("4학년 2학기", GradeParser.detectGradeGroup("", "4/2 단원별"));
    }

    @Test
    void parsesSingleGrade() {
        assertEquals("4학년", GradeParser.detectGradeGroup("", "4학년 수학"));
        assertEquals("2학년", GradeParser.detectGradeGroup("", "초등 2학년"));
    }

    @Test
    void returnsAllGradesWhenMissing() {
        assertEquals("전학년", GradeParser.detectGradeGroup("", "참고서"));
        assertEquals("전학년", GradeParser.detectGradeGroup("", "전학년용 문제집"));
    }

    @Test
    void handlesKoreanSemesterWords() {
        assertEquals("4학년 1학기", GradeParser.detectGradeGroup("", "4학년 상"));
        assertEquals("4학년 2학기", GradeParser.detectGradeGroup("", "4학년 하"));
    }

    @Test
    void parsesAttachedShortNotation() {
        assertEquals("3학년 1학기", GradeParser.detectGradeGroup("", "수학3-1 문제집"));
        assertEquals("3학년 1학기", GradeParser.detectGradeGroup("", "고등학교3-1 참고서"));
        assertEquals("3학년 1학기", GradeParser.detectGradeGroup("", "수학 3-1 단원별"));
        assertEquals("3학년 1학기", GradeParser.detectGradeGroup("", "3-1 수학 문제집"));
    }
}