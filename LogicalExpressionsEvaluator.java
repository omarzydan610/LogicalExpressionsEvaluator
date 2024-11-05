import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

class Colors {
    static String reset = "\u001B[0m";
    static String red = "\u001B[31m";
    static String green = "\u001B[32m";
    static String blue = "\u001B[34m";
    static String yellow = "\u001B[33m";
    static String bold = "\u001B[1m";
}

interface Expression {
    String getRepresentation();

    void setRepresentation(String representation);
}

interface LogicalExpressionSolver {
    boolean evaluateExpression(Expression expression);
}

class LogicalExp implements Expression {
    private String representation;

    @Override
    public String getRepresentation() {
        return representation;
    }

    @Override
    public void setRepresentation(String representation) {
        this.representation = representation;
    }
}

class EvaluateExp implements LogicalExpressionSolver {
    private static final Scanner sc = new Scanner(System.in);
    Map<Character, Boolean> map = new HashMap<>();

    @Override
    public boolean evaluateExpression(Expression expression) {
        String exp = expression.getRepresentation();
        try {
            validateExpression(exp);
            System.out.println(Colors.green + Colors.bold + "Expression is valid." + Colors.reset);
            return getVariables(exp);
        } catch (Exception e) {
            System.out.println(Colors.red + Colors.bold + "Validation error: " + e.getMessage() + Colors.reset);
            System.exit(0);
            return false;
        }
    }

    private Boolean getVariables(String exp) {
        for (int i = 0; i < exp.length(); i++) {
            char currentChar = exp.charAt(i);
            if (Character.isLetter(currentChar) && currentChar != 'v') {
                if (!map.containsKey(currentChar)) {
                    boolean validInput = false;
                    while (!validInput) {
                        System.out
                                .print(Colors.blue + "Please enter the value of " + exp.charAt(i) + " (true/false): "
                                        + Colors.reset);
                        String input = sc.nextLine().trim();
                        if (input.equals("true")) {
                            map.put(currentChar, true);
                            validInput = true;
                        } else if (input.equals("false")) {
                            map.put(currentChar, false);
                            validInput = true;
                        } else {
                            System.out.println(Colors.red + Colors.bold + "Invalid input." + Colors.reset);
                        }
                    }
                }
            }
        }

        int implyCount = 0;
        for (int i = 0; i < exp.length(); i++) {
            if (exp.charAt(i) == '>') {
                implyCount++;
            }
        }
        exp = exp.replaceAll("\\s+", "");
        exp = replaceImply(exp, implyCount);
        exp = replaceVariables(exp);
        exp = infixToPostfix(exp);
        return evaluatePostfix(exp);
    }

    int precedence(char operator) {
        switch (operator) {
            case '~':
                return 2;
            case '^':
            case 'v':
                return 1;
            default:
                return -1;
        }
    }

    String infixToPostfix(String expression) {
        Stack<Character> stack = new Stack<>();
        StringBuilder postfix = new StringBuilder();

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == 'T' || c == 'F') {
                postfix.append(c);
            } else if (c == '(') {
                stack.push(c);
            } else if (c == ')') {
                while (!stack.isEmpty() && stack.peek() != '(') {
                    postfix.append(stack.pop());
                }
                stack.pop();
            } else {
                if (!stack.isEmpty() && (stack.peek() == c)) {
                    stack.pop();
                } else {
                    while (!stack.isEmpty() && precedence(stack.peek()) >= precedence(c)) {
                        postfix.append(stack.pop());
                    }
                    stack.push(c);
                }
            }
        }
        while (!stack.isEmpty()) {
            postfix.append(stack.pop());
        }
        return postfix.toString();
    }

    Boolean evaluatePostfix(String postfix) {
        Stack<Boolean> stack = new Stack<>();

        for (int i = 0; i < postfix.length(); i++) {
            char c = postfix.charAt(i);
            if (c == 'T') {
                stack.push(true);
            } else if (c == 'F') {
                stack.push(false);
            } else if (c == '~') {
                boolean operand = stack.pop();
                stack.push(!operand);
            } else {
                boolean operand2 = stack.pop();
                boolean operand1 = stack.pop();
                switch (c) {
                    case '^':
                        stack.push(operand1 && operand2);
                        break;
                    case 'v':
                        stack.push(operand1 || operand2);
                        break;
                }
            }
        }
        return stack.pop();
    }

    String replaceImply(String x, int n) {
        for (int j = 0; j < n; j++) {
            int open = 0, close = 0, flag = 0, lastopen = 0, firstclose = 0, imply = -1;
            for (int i = x.length() - 1; i >= 0; i--) {
                if (x.charAt(i) == '(') {
                    open++;
                    if (flag == 1 && open > close) {
                        lastopen = i;
                        break;
                    }
                } else if (x.charAt(i) == ')') {
                    close++;
                    if (flag == 0) {
                        firstclose = i;
                    }
                }
                if (x.charAt(i) == '>' && imply == -1) {
                    imply = i;
                    if (open == close) {
                        break;
                    } else {
                        flag = 1;
                        open = 0;
                        close = 0;
                    }
                }
            }
            if (flag == 0) {
                x = "~(" + x.substring(0, imply) + ")v(" + x.substring(imply + 1) + ')';
            } else {
                x = x.substring(0, lastopen) + "(~" + x.substring(lastopen, imply) + ")v("
                        + x.substring(imply + 1, firstclose)
                        + ')' + x.substring(firstclose);
            }
        }
        return x;
    }

    String replaceVariables(String s) {
        for (Map.Entry<Character, Boolean> entry : map.entrySet()) {
            if (entry.getValue() == true) {
                s = s.replace(entry.getKey(), 'T');
            } else {
                s = s.replace(entry.getKey(), 'F');
            }
        }
        return s;
    }

    private void validateExpression(String exp) {
        if (exp == null || exp.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty.");
        }

        if (isOperator(exp.charAt(exp.length() - 1))) {
            throw new IllegalArgumentException("Expression ends with an operator.");
        }

        if (isOperator(exp.charAt(0)) && exp.charAt(0) != '~') {
            throw new IllegalArgumentException("Expression starts with an operator.");
        }

        Stack<Character> parenthesesStack = new Stack<>();
        char prevChar = '\0';

        for (int i = 0; i < exp.length(); i++) {
            char currentChar = exp.charAt(i);

            if (!isAllowedCharacter(currentChar)) {
                throw new IllegalArgumentException("Invalid character: " + currentChar);
            }

            if (currentChar == '(') {
                parenthesesStack.push(currentChar);
            } else if (currentChar == ')') {
                if (parenthesesStack.isEmpty() || parenthesesStack.pop() != '(') {
                    throw new IllegalArgumentException("Unmatched parentheses at position: " + i);
                }
            }

            if ((isOperator(currentChar) && currentChar != '~') && isOperator(prevChar)) {
                throw new IllegalArgumentException("Invalid consecutive operators at position: " + i);
            }

            if ((currentChar == ')' && isOperator(prevChar))
                    || (prevChar == '(' && (isOperator(currentChar) && currentChar != '~'))
                    || (currentChar == '(' && (Character.isLetter(prevChar) && !isOperator(prevChar)))
                    || (prevChar == ')' && (Character.isLetter(prevChar) && !isOperator(prevChar)))) {
                throw new IllegalArgumentException("Invalid operator placement around parentheses at position: " + i);
            }

            if (((Character.isLetter(currentChar) && !isOperator(currentChar))
                    || currentChar == '~')
                    && (Character.isLetter(prevChar) && !isOperator(prevChar))) {
                throw new IllegalArgumentException("Invalid consecutive variablis at position: " + i);
            }

            if (currentChar != ' ') {
                prevChar = currentChar;
            }
        }

        if (!parenthesesStack.isEmpty()) {
            throw new IllegalArgumentException("Unmatched opening parentheses.");
        }
    }

    private boolean isAllowedCharacter(char c) {
        return (Character.isLetter(c) || c == '~' || c == '^' || c == 'v' || c == '>' || c == '(' || c == ')'
                || c == ' ');
    }

    private boolean isOperator(char c) {
        return (c == '~' || c == '^' || c == 'v' || c == '>');
    }

    public void closeScanner() {
        sc.close();
    }
}

public class LogicalExpressionsEvaluator {
    public static void main(String[] args) {
        System.err.print(Colors.bold + Colors.blue + "Enter the expression: " + Colors.reset);
        Scanner sc = new Scanner(System.in);
        String x = sc.nextLine();
        LogicalExp exp = new LogicalExp();
        exp.setRepresentation(x);
        System.out.println(
                Colors.bold + "Output: "
                        + (new EvaluateExp().evaluateExpression(exp) ? Colors.green + "true" : Colors.red + "false")
                        + Colors.reset);
        sc.close();
    }
}
