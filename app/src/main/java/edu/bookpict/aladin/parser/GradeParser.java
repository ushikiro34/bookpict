package edu.bookpict.aladin.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradeParser {

    /**
     * 학년/학기 파싱 결과
     */
    public static class GradeInfo {
        private final String grade;    // "1학년" ~ "6학년" 등, 또는 null
        private final String semester; // "1학기", "2학기", 또는 "전체"

        public GradeInfo(String grade, String semester) {
            this.grade = grade;
            this.semester = semester;
        }

        public String getGrade() { return grade; }
        public String getSemester() { return semester; }
    }

    // 오탐 방지: 연도 패턴 (2008학년도 등)
    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}\\s*학년");

    // 오탐 방지: 영문+숫자 조합 (basic4-80words 등)
    private static final Pattern ALPHANUMERIC_DASH_PATTERN = Pattern.compile("[a-zA-Z]\\d+\\s*[-/]\\s*\\d+");

    // 패턴1: "4학년 2학기" 또는 "4학년 1학기" / 상/하
    private static final Pattern P_GRADE_SEMESTER = Pattern.compile("(\\d)학년\\s*(?:([12])학기|상|하)");

    // 패턴2: "4-2" 또는 "4/2" 형식 (과목명 뒤에 붙는 경우만)
    private static final Pattern P_SHORT_NOTATION = Pattern.compile(
            "(?:초등|중등|고등|학교|수학|영어|국어|과학|사회|수능)\\s*(\\d)\\s*[-/·\\u00B7]\\s*([12])(?!\\d)");

    // 패턴3: "중등3", "중3", "초등5", "고2" 등 학교급+학년 축약 표기
    private static final Pattern P_LEVEL_GRADE = Pattern.compile(
            "(?:초등|중등|고등|초|중|고)\\s*(\\d)(?!\\d|학년|\\s*[-/·\\u00B7]\\s*\\d)");

    // 패턴4: 단순 "4학년"
    private static final Pattern P_GRADE_ONLY = Pattern.compile("(\\d)학년");

    /**
     * 학년/학기(grade, semester) 추출
     * - 학기가 없는 경우 semester = "전체"
     * - 학년도 없는 경우 grade = null, semester = "전체"
     */
    public static GradeInfo detectGrade(String category, String title) {
        String combined = (category + " " + title);
        String lower = combined.toLowerCase();

        // 이미 전학년 표기가 있으면 바로 반환
        if (lower.contains("전학년") || lower.contains("all grades")) {
            return new GradeInfo(null, "전체");
        }

        // 오탐 필터링: 연도 표기 제거 (2008학년도 -> 빈 문자열로 치환 후 매칭)
        String filtered = YEAR_PATTERN.matcher(combined).replaceAll("");

        // 오탐 필터링: 영문+숫자-숫자 패턴 제거 (basic4-80words 등)
        filtered = ALPHANUMERIC_DASH_PATTERN.matcher(filtered).replaceAll("");

        // 패턴1: "4학년 2학기" 또는 "4학년 상/하"
        Matcher m1 = P_GRADE_SEMESTER.matcher(filtered);
        if (m1.find()) {
            String grade = m1.group(1);
            String sem = m1.group(2);
            if (sem == null) {
                // 상/하 처리
                if (filtered.substring(m1.start()).contains("상")) sem = "1";
                else if (filtered.substring(m1.start()).contains("하")) sem = "2";
            }
            if (sem != null) {
                return new GradeInfo(grade + "학년", sem + "학기");
            }
            return new GradeInfo(grade + "학년", "전체");
        }

        // 패턴2: "수학4-2" 같은 짧은 표기 (과목명 prefix 필수)
        Matcher m2 = P_SHORT_NOTATION.matcher(filtered);
        if (m2.find()) {
            String grade = m2.group(1);
            String sem = m2.group(2);
            return new GradeInfo(grade + "학년", sem + "학기");
        }

        // 패턴3: "중등3", "중3", "초등5" 같은 학교급+학년 축약 표기
        Matcher mLevel = P_LEVEL_GRADE.matcher(filtered);
        if (mLevel.find()) {
            String grade = mLevel.group(1);
            return new GradeInfo(grade + "학년", "전체");
        }

        // 패턴4: 단순 "4학년"
        Matcher m3 = P_GRADE_ONLY.matcher(filtered);
        if (m3.find()) {
            String grade = m3.group(1);
            return new GradeInfo(grade + "학년", "전체");
        }

        // 기본값: 학년/학기 정보 없음
        return new GradeInfo(null, "전체");
    }

    /**
     * 하위 호환용: 기존 gradeGroup 형식 문자열 반환
     */
    public static String detectGradeGroup(String category, String title) {
        GradeInfo info = detectGrade(category, title);
        if (info.getGrade() == null) return "전학년";
        if ("전체".equals(info.getSemester())) return info.getGrade();
        return info.getGrade() + " " + info.getSemester();
    }
}
