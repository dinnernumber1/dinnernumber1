import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("用法: java Main <原文文件> <抄袭版文件> <答案文件>");
            System.exit(1);
        }
        try {
            // 读取原文和抄袭版论文，统一使用 UTF-8 编码
            String orig = new String(Files.readAllBytes(Paths.get(args[0])), StandardCharsets.UTF_8);
            String copy = new String(Files.readAllBytes(Paths.get(args[1])), StandardCharsets.UTF_8);

            // 计算重复率
            double rate = calcSimilarity(orig, copy);

            // 格式化输出，保留两位小数，使用 Locale.US 确保小数点为 '.'
            String result = String.format(Locale.US, "%.2f", rate);

            // 写入答案文件
            Files.write(Paths.get(args[2]), result.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * 计算重复率：使用字符二元组（bigram）的召回率。
     * 即：抄袭版中出现在原文中的 bigram 数量 / 抄袭版总 bigram 数量。
     * 返回值范围 [0.0, 1.0]。
     */
    public static double calcSimilarity(String orig, String copy) {
        orig = clean(orig);
        copy = clean(copy);

        if (copy.isEmpty()) {
            return 0.0;
        }

        // 存储原文的所有字符二元组（用 int 编码两个 char，无哈希冲突）
        Set<Integer> origSet = new HashSet<>();
        for (int i = 0; i + 1 < orig.length(); i++) {
            int h = (orig.charAt(i) << 16) | orig.charAt(i + 1);
            origSet.add(h);
        }

        int total = 0;
        int match = 0;
        for (int i = 0; i + 1 < copy.length(); i++) {
            int h = (copy.charAt(i) << 16) | copy.charAt(i + 1);
            total++;
            if (origSet.contains(h)) {
                match++;
            }
        }

        // 如果抄袭版长度小于 2，直接比较整体字符串
        if (total == 0) {
            return orig.equals(copy) ? 1.0 : 0.0;
        }

        return (double) match / total;
    }

    /**
     * 清洗文本：只保留汉字、英文字母、数字，去除标点、空白等。
     */
    public static String clean(String s) {
        return s.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
    }
}
