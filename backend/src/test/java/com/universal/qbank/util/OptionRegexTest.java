package com.universal.qbank.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OptionRegexTest {

  private static final Pattern OPTION_PATTERN =
      Pattern.compile(
          "(?:^|\\s+)(?:([A-Z])\\s*[.、．\\)]|\\(([A-Z])\\))\\s*(.*?)(?=\\s+(?:[A-Z]\\s*[.、．\\)]|\\([A-Z]\\))|$)",
          Pattern.DOTALL);

  @Test
  void shouldParseInlineAndBracketOptions() {
    List<String> parsed = parseOptions("A. Option A   B. Option B   (C) Option C");
    assertEquals(List.of("A:Option A", "B:Option B", "C:Option C"), parsed);
  }

  @Test
  void shouldIgnorePlainSentenceWithoutOptionPrefix() {
    List<String> parsed = parseOptions("Java is good. A is a letter");
    assertEquals(List.of(), parsed);
  }

  private List<String> parseOptions(String input) {
    List<String> result = new ArrayList<>();
    Matcher matcher = OPTION_PATTERN.matcher(input);
    while (matcher.find()) {
      String label = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
      String content = matcher.group(3).trim();
      result.add(label + ":" + content);
    }
    return result;
  }
}
