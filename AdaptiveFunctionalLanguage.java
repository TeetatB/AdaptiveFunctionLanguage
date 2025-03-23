import java.util.*;
import java.util.function.Function;

// Modifiable class for automatic updates
class Mod<T> {
    private T value;
    private final List<Runnable> dependents = new ArrayList<>();

    public Mod(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T newValue) {
        this.value = newValue;
        notifyDependents();
    }

    public Mod<T> bind(Function<T, T> func) {
        Mod<T> dependent = new Mod<>(func.apply(value));
        dependents.add(() -> dependent.set(func.apply(this.value)));
        return dependent;
    }

    private void notifyDependents() {
        for (Runnable d : dependents) {
            d.run();
        }
    }
}

// Tokenizer (Lexer)
class Token {
    enum Type { NUMBER, IDENTIFIER, OPERATOR, LPAREN, RPAREN, EOF }
    Type type;
    String value;
    Token(Type type, String value) { this.type = type; this.value = value; }
}

class Tokenizer {
    private String input;
    private int pos = 0;
    private static final Set<Character> OPERATORS = Set.of('+', '-', '*', '/');

    Tokenizer(String input) { this.input = input; }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < input.length()) {
            char current = input.charAt(pos);
            if (Character.isWhitespace(current)) {
                pos++;
            } else if (Character.isDigit(current)) {
                tokens.add(new Token(Token.Type.NUMBER, readNumber()));
            } else if (Character.isLetter(current)) {
                tokens.add(new Token(Token.Type.IDENTIFIER, readIdentifier()));
            } else if (OPERATORS.contains(current)) {
                tokens.add(new Token(Token.Type.OPERATOR, String.valueOf(current)));
                pos++;
            } else {
                pos++;
            }
        }
        tokens.add(new Token(Token.Type.EOF, ""));
        return tokens;
    }

    private String readNumber() {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
            sb.append(input.charAt(pos++));
        }
        return sb.toString();
    }

    private String readIdentifier() {
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && Character.isLetter(input.charAt(pos))) {
            sb.append(input.charAt(pos++));
        }
        return sb.toString();
    }
}

// AST Nodes
abstract class ASTNode {}
class NumberNode extends ASTNode { int value; NumberNode(int value) { this.value = value; } }
class BinaryOpNode extends ASTNode { ASTNode left, right; String operator;
    BinaryOpNode(ASTNode left, String operator, ASTNode right) {
        this.left = left; this.operator = operator; this.right = right;
    }
}
class VariableNode extends ASTNode { String name; VariableNode(String name) { this.name = name; } }
class AssignmentNode extends ASTNode { String varName; ASTNode expression;
    AssignmentNode(String varName, ASTNode expression) {
        this.varName = varName; this.expression = expression;
    }
}

// Parser
class Parser {
    private List<Token> tokens;
    private int pos = 0;
    Parser(List<Token> tokens) { this.tokens = tokens; }

    ASTNode parseExpression() {
        ASTNode left = parseTerm();
        while (pos < tokens.size() && tokens.get(pos).type == Token.Type.OPERATOR) {
            String op = tokens.get(pos++).value;
            ASTNode right = parseTerm();
            left = new BinaryOpNode(left, op, right);
        }
        return left;
    }
    ASTNode parseTerm() {
        Token token = tokens.get(pos++);
        if (token.type == Token.Type.NUMBER) return new NumberNode(Integer.parseInt(token.value));
        else return new VariableNode(token.value);
    }
}

// Evaluator
class Evaluator {
    private final Map<String, Mod<Integer>> variables = new HashMap<>();

    int evaluate(ASTNode node) {
        if (node instanceof NumberNode) {
            return ((NumberNode) node).value;
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node;
            int left = evaluate(binOp.left);
            int right = evaluate(binOp.right);
            return switch (binOp.operator) {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> left / right;
                default -> throw new RuntimeException("Unknown operator: " + binOp.operator);
            };
        } else if (node instanceof VariableNode) {
            return variables.getOrDefault(((VariableNode) node).name, new Mod<>(0)).get();
        } else if (node instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) node;
            int value = evaluate(assign.expression);
            variables.put(assign.varName, new Mod<>(value));
            return value;
        }
        throw new RuntimeException("Unknown AST node");
    }
}

// Main - Example Usage
public class AdaptiveFunctionalLanguage {
    public static void main(String[] args) {
        Mod<Integer> x = new Mod<>(5);
        Mod<Integer> y = x.bind(val -> val + 3);

        System.out.println("Initial y: " + y.get()); // Output: 8
        x.set(10);
        System.out.println("Updated y: " + y.get()); // Output: 13 (adaptive update)
    }
}
