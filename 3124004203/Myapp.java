package jisuanqi;

import java.io.*;
import java.util.*;

// ================= 1. 分数运算类 =================
class Fraction implements Comparable<Fraction> {
    int numerator; // 分子
    int denominator; // 分母

    public Fraction(int n, int d) {
        if (d == 0) throw new ArithmeticException("分母不能为0");
        if (d < 0) { n = -n; d = -d; }
        int gcd = gcd(Math.abs(n), Math.abs(d));
        this.numerator = n / gcd;
        this.denominator = d / gcd;
    }

    private int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public Fraction add(Fraction other) {
        return new Fraction(this.numerator * other.denominator + other.numerator * this.denominator, this.denominator * other.denominator);
    }

    public Fraction subtract(Fraction other) {
        return new Fraction(this.numerator * other.denominator - other.numerator * this.denominator, this.denominator * other.denominator);
    }

    public Fraction multiply(Fraction other) {
        return new Fraction(this.numerator * other.numerator, this.denominator * other.denominator);
    }

    public Fraction divide(Fraction other) {
        if (other.numerator == 0) throw new ArithmeticException("除数不能为0");
        return new Fraction(this.numerator * other.denominator, this.denominator * other.numerator);
    }

    @Override
    public int compareTo(Fraction other) {
        return Integer.compare(this.numerator * other.denominator, other.numerator * this.denominator);
    }

    public boolean isInteger() {
        return denominator == 1;
    }

    @Override
    public String toString() {
        if (denominator == 1) return String.valueOf(numerator);
        if (Math.abs(numerator) < denominator) {
            return numerator + "/" + denominator;
        } else {
            int integerPart = numerator / denominator;
            int remainder = Math.abs(numerator % denominator);
            return integerPart + "'" + remainder + "/" + denominator;
        }
    }
}

// ================= 2. 表达式树节点 =================
class ExpressionNode {
    Fraction value;
    char operator;
    ExpressionNode left, right;
    boolean isLeaf;

    public ExpressionNode(Fraction value) { this.value = value; this.isLeaf = true; }
    public ExpressionNode(char operator, ExpressionNode left, ExpressionNode right) {
        this.operator = operator; this.left = left; this.right = right; this.isLeaf = false;
    }

    public Fraction evaluate() {
        if (isLeaf) return value;
        Fraction lVal = left.evaluate();
        Fraction rVal = right.evaluate();
        switch (operator) {
            case '+': return lVal.add(rVal);
            case '-': return lVal.subtract(rVal);
            case '*': return lVal.multiply(rVal);
            case '/': return lVal.divide(rVal);
            default: throw new IllegalArgumentException("未知运算符");
        }
    }

    public String toExpressionString() {
        if (isLeaf) return value.toString();
        String leftStr = left.toExpressionString();
        String rightStr = right.toExpressionString();
        if (isLowerPriority(left.operator, this.operator, true)) leftStr = "(" + leftStr + ")";
        if (isLowerPriority(right.operator, this.operator, false)) rightStr = "(" + rightStr + ")";
        return leftStr + " " + operator + " " + rightStr;
    }

    // 修复 Bug 2：过滤叶子节点，避免产生 2 / (3) 这样的多余括号
    private boolean isLowerPriority(char childOp, char parentOp, boolean isLeft) {
        // 如果子节点不是运算符（是叶子节点），不需要加括号
        if (childOp != '+' && childOp != '-' && childOp != '*' && childOp != '/') return false;
        // 如果父节点不是运算符，不需要加括号
        if (parentOp != '+' && parentOp != '-' && parentOp != '*' && parentOp != '/') return false;

        if (parentOp == '*' || parentOp == '/') {
            return childOp == '+' || childOp == '-';
        }
        if (parentOp == '-' && !isLeft) {
            return childOp == '+' || childOp == '-';
        }
        if (parentOp == '/' && !isLeft) {
            return true;
        }
        return false;
    }

    public String getCanonicalString() { return canonicalize(this); }

    private String canonicalize(ExpressionNode node) {
        if (node.isLeaf) return node.value.toString();
        if (node.operator == '+' || node.operator == '*') {
            List<String> operands = new ArrayList<>();
            collectOperands(node, node.operator, operands);
            Collections.sort(operands);
            return "(" + String.join(" " + node.operator + " ", operands) + ")";
        } else {
            return "(" + canonicalize(node.left) + " " + node.operator + " " + canonicalize(node.right) + ")";
        }
    }

    private void collectOperands(ExpressionNode node, char op, List<String> operands) {
        if (node.isLeaf) operands.add(node.value.toString());
        else if (node.operator == op) {
            collectOperands(node.left, op, operands);
            collectOperands(node.right, op, operands);
        } else operands.add(canonicalize(node));
    }
}

// ================= 3. 题目生成器 =================
class Generator {
    private int range;
    private Random random = new Random();
    private Set<String> uniqueExpressions = new HashSet<>();

    public Generator(int range) { this.range = range; }

    private Fraction randomFraction() {
        if (range <= 2 || random.nextBoolean()) return new Fraction(random.nextInt(range), 1);
        else {
            int den = random.nextInt(range - 1) + 2;
            int num = random.nextInt(den - 1) + 1;
            return new Fraction(num, den);
        }
    }

    // 修复 Bug 1：抛出异常，让外层捕获，避免死循环
    public ExpressionNode generateExpression(int maxOperators) throws ArithmeticException {
        ExpressionNode root = generateNode(maxOperators);
        String canonical = root.getCanonicalString();
        if (uniqueExpressions.contains(canonical)) {
            throw new ArithmeticException("重复表达式");
        }
        uniqueExpressions.add(canonical);
        return root;
    }

    private ExpressionNode generateNode(int maxOperators) {
        if (maxOperators == 0 || random.nextDouble() < 0.3) return new ExpressionNode(randomFraction());
        char[] ops = {'+', '-', '*', '/'};
        char op = ops[random.nextInt(ops.length)];
        int leftOps = random.nextInt(maxOperators);
        int rightOps = maxOperators - 1 - leftOps;
        ExpressionNode left = generateNode(leftOps);
        ExpressionNode right = generateNode(rightOps);
        Fraction leftVal = left.evaluate();
        Fraction rightVal = right.evaluate();
        if (op == '-') {
            if (leftVal.compareTo(rightVal) < 0) throw new ArithmeticException("减法产生负数");
        } else if (op == '/') {
            if (rightVal.numerator == 0) throw new ArithmeticException("除数为0");
            if (leftVal.divide(rightVal).isInteger()) throw new ArithmeticException("除法结果必须是真分数(非整数)");
        }
        return new ExpressionNode(op, left, right);
    }
}

// ================= 4. 主程序与判卷逻辑 =================
public class Myapp {
    public static void main(String[] args) {
        if (args.length == 0) { printHelp(); return; }
        int n = -1, r = -1;
        String exerciseFile = null, answerFile = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-n": n = Integer.parseInt(args[++i]); break;
                case "-r": r = Integer.parseInt(args[++i]); break;
                case "-e": exerciseFile = args[++i]; break;
                case "-a": answerFile = args[++i]; break;
                default: System.out.println("未知参数: " + args[i]); printHelp(); return;
            }
        }

        if (exerciseFile != null && answerFile != null) { grade(exerciseFile, answerFile); return; }
        if (r == -1) { System.out.println("错误：必须给定 -r 参数控制数值范围！"); printHelp(); return; }
        if (n == -1) n = 10;
        generate(n, r);
    }

    private static void printHelp() {
        System.out.println("用法:");
        System.out.println("  生成题目: Myapp.exe -n <题目数量> -r <数值范围>");
        System.out.println("  批改作业: Myapp.exe -e <题目文件> -a <答案文件>");
    }

    private static void generate(int n, int r) {
        Generator generator = new Generator(r);
        List<String> exercises = new ArrayList<>();
        List<String> answers = new ArrayList<>();
        System.out.println("正在生成题目，请稍候...");
        int failCount = 0;
        for (int i = 1; i <= n; i++) {
            try {
                ExpressionNode root = generator.generateExpression(3);
                exercises.add(i + ". " + root.toExpressionString() + " = ");
                answers.add(i + ". " + root.evaluate().toString());
                failCount = 0;
            } catch (Exception e) {
                i--; failCount++;
                if (failCount > 1000) {
                    System.out.println("范围太小，无法生成足够不重复题目。已生成: " + (i - 1) + " 题。");
                    break;
                }
            }
        }
        writeFile("Exercises.txt", exercises);
        writeFile("Answers.txt", answers);
        System.out.println("成功生成 " + exercises.size() + " 道题目。");
    }

    private static void grade(String exerciseFile, String answerFile) {
        List<String> correctList = new ArrayList<>();
        List<String> wrongList = new ArrayList<>();
        try (BufferedReader exReader = new BufferedReader(new FileReader(exerciseFile));
             BufferedReader ansReader = new BufferedReader(new FileReader(answerFile))) {
            String exLine, ansLine;
            int index = 1;
            while ((exLine = exReader.readLine()) != null && (ansLine = ansReader.readLine()) != null) {
                String exprStr = exLine.substring(exLine.indexOf(". ") + 2, exLine.lastIndexOf(" =")).trim();
                String userAns = ansLine.substring(ansLine.indexOf(". ") + 2).trim();
                Fraction correctAns = evaluateString(exprStr);
                if (correctAns != null && correctAns.toString().equals(userAns)) {
                    correctList.add(String.valueOf(index));
                } else {
                    wrongList.add(String.valueOf(index));
                }
                index++;
            }
        } catch (IOException e) { System.out.println("读取文件失败: " + e.getMessage()); return; }

        List<String> gradeResult = new ArrayList<>();
        gradeResult.add("Correct: " + correctList.size() + " (" + String.join(", ", correctList) + ")");
        gradeResult.add("Wrong: " + wrongList.size() + " (" + String.join(", ", wrongList) + ")");
        writeFile("Grade.txt", gradeResult);
        System.out.println("批改完成，结果已保存至 Grade.txt");
    }

    private static Fraction evaluateString(String exprStr) {
        try {
            return parseExpression(exprStr);
        } catch (Exception e) { return null; }
    }

    private static Fraction parseExpression(String expr) {
        final String cleanExpr = expr.replace(" ", "");
        return new Object() {
            int pos = -1, ch;
            void nextChar() { ch = (++pos < cleanExpr.length()) ? cleanExpr.charAt(pos) : -1; }
            boolean eat(int charToEat) {
                if (ch == charToEat) { nextChar(); return true; }
                return false;
            }
            Fraction parse() { nextChar(); return parseExpression(); }
            Fraction parseExpression() {
                Fraction x = parseTerm();
                for (;;) {
                    if (eat('+')) x = x.add(parseTerm());
                    else if (eat('-')) x = x.subtract(parseTerm());
                    else return x;
                }
            }
            Fraction parseTerm() {
                Fraction x = parseFactor();
                for (;;) {
                    if (eat('*')) x = x.multiply(parseFactor());
                    else if (eat('/')) x = x.divide(parseFactor());
                    else return x;
                }
            }
            Fraction parseFactor() {
                if (eat('(')) { Fraction x = parseExpression(); eat(')'); return x; }
                // 修复 Bug 3：支持一元负号
                if (eat('-')) return parseFactor().multiply(new Fraction(-1, 1));

                int startPos = this.pos;
                if (ch >= '0' && ch <= '9') {
                    while (ch >= '0' && ch <= '9') nextChar();
                    if (ch == '\'') {
                        int intPart = Integer.parseInt(cleanExpr.substring(startPos, this.pos));
                        nextChar();
                        int fracStart = this.pos;
                        while (ch >= '0' && ch <= '9' || ch == '/') nextChar();
                        String[] parts = cleanExpr.substring(fracStart, this.pos).split("/");
                        Fraction fraction = new Fraction(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                        return new Fraction(intPart, 1).add(fraction);
                    } else if (ch == '/') {
                        nextChar();
                        while (ch >= '0' && ch <= '9') nextChar();
                        String numStr = cleanExpr.substring(startPos, this.pos);
                        String[] parts = numStr.split("/");
                        return new Fraction(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    } else {
                        return new Fraction(Integer.parseInt(cleanExpr.substring(startPos, this.pos)), 1);
                    }
                }
                throw new RuntimeException("Unexpected: " + (char) ch);
            }
        }.parse();
    }

    private static void writeFile(String filename, List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (String line : lines) { writer.write(line); writer.newLine(); }
        } catch (IOException e) { System.out.println("写入文件失败: " + filename); }
    }
}