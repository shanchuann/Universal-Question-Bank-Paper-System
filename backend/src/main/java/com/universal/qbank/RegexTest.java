import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexTest {
    public static void main(String[] args) {
        String[] lines = {
            "A. 是",
            "B. 不是",
            "A) 封装",
            "(B) 继承",
            "C、多态",
            "A. Option A   B. Option B",
            "   A. Indented Option",
            "Java is good.", // Should not match
            "A is a letter" // Should not match
        };

        // Improved Pattern
        // Matches:
        // 1. Start of string or whitespace
        // 2. Label: (A) or A. or A) or A、
        // 3. Content
        // 4. Lookahead for next option or end
        
        // Group 1: Label if A. or A) or A、
        // Group 2: Label if (A)
        String patternStr = "(?:^|\\s+)(?:([A-Z])\\s*[.、．\\)]|\\(([A-Z])\\))\\s*(.*?)(?=\\s+(?:[A-Z]\\s*[.、．\\)]|\\([A-Z]\\))|$)";
        Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);

        for (String line : lines) {
            System.out.println("Testing: '" + line + "'");
            Matcher m = pattern.matcher(line);
            while (m.find()) {
                String label = m.group(1) != null ? m.group(1) : m.group(2);
                String content = m.group(3);
                System.out.println("  Found: " + label + " -> " + content);
            }
        }
    }
}
