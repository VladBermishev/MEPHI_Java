package org.example;

import org.apache.commons.cli.*;

import java.awt.*;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

enum TokenType {
    STRING,
    NUMBER,
    OPERATOR,
    EOF
}

class Token{
    private TokenType type;
    private String value;
    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }
    public TokenType getType() {return type;}
    public String getValue() {return value;}
}

class Lexer {
    private Reader zzReader;
    private int yychar;
    private Token currentToken;
    Lexer(Reader in) {this.zzReader = in;this.currentToken = null;}

    public Token next(){
        try {
            Token tok = new Token(TokenType.EOF, "");
            yychar = zzReader.read();
            if(yychar == -1){
                currentToken = tok;
                return current();
            }
            while (Character.isWhitespace(yychar))
                yychar = zzReader.read();
            if(Character.isDigit(yychar))
                tok = new Token(TokenType.NUMBER, readNumber());
            else if (Lexer.isOper((char)yychar))
                tok = new Token(TokenType.OPERATOR, Character.toString((char)yychar));
            else if (!Character.isWhitespace(yychar))
                tok = new Token(TokenType.STRING, readString());
            currentToken = tok;
            return current();
        }catch (Exception e) {
            currentToken = new Token(TokenType.EOF, "");
            return currentToken;
        }
    }
    public Token current() { return currentToken; }

    private String readNumber(){
        try {
            StringBuilder value = new StringBuilder();
            while(yychar != -1 && Character.isDigit(yychar)) {
                value.append((char)yychar);
                yychar = zzReader.read();
            }
            return value.toString();
        }catch (Exception e) {
            return "";
        }
    }

    private String readString(){
        try {
            StringBuilder value = new StringBuilder();
            while(yychar != -1 && !Character.isWhitespace(yychar)) {
                value.append((char)yychar);
                yychar = zzReader.read();
            }
            return value.toString();
        }catch (Exception e) {
            return "";
        }
    }

    static private boolean isOper(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }
}
class Expr {
    @Override
    public String toString() {
        return "<Empty Expression>";
    }
}
final class ConstValue extends Expr {
    private String value;
    public ConstValue(String value) {this.value = value;}

    public String getValue() {return value;}
    public int asInt() {return Integer.parseInt(value);}
    public float asFloat() {return Float.parseFloat(value);}

}

class Command extends Expr {
    public String command;
    Command(String command) {this.command = command;}
}
class LinkCommand extends Expr {
    public String link;
    LinkCommand(String link) {this.link = link;}
}
final class UnknownCommand extends Command {
    UnknownCommand(String command) {super(command);}
}
final class QuitCommand extends Command {
    QuitCommand(String command) {super(command);}
}

final class ListCommand extends Command {
    ListCommand(String command) {super(command);}
}

final class HelpCommand extends Command {
    HelpCommand(String command) {super(command);}
}

final class ShortenCommand extends LinkCommand {
    ShortenCommand(String link) {super(link);}
}

final class GoCommand extends LinkCommand {
    GoCommand(String link) {super(link);}
}

class Parser {
    static public Expr parse(String input) {
        Lexer lex = new Lexer(new StringReader(input));
        Token current = lex.next();
        switch (current.getType()) {
            case STRING: return Parser.parseCommand(lex);
            case EOF: return new Expr();
        }
        return new UnknownCommand(current.getValue());
    }

    static private Expr parseCommand(Lexer lex) {
        Parser.expect(lex, TokenType.STRING);
        return switch (lex.current().getValue().toLowerCase()) {
            case "l", "list" -> new ListCommand(lex.current().getValue());
            case "s", "shorten" -> parseShortenCommand(lex);
            case "g", "go" -> parseGoCommand(lex);
            case "h", "help" -> new HelpCommand(lex.current().getValue());
            case "q", "quit" -> new QuitCommand(lex.current().getValue());
            default -> new UnknownCommand(lex.current().getValue());
        };
    }

    static private ConstValue parseConstValue(Lexer lex) {
        Parser.expect(lex, TokenType.NUMBER);
        ConstValue result = new ConstValue(lex.current().getValue());
        lex.next();
        return result;
    }

    static private ShortenCommand parseShortenCommand(Lexer lex){
        lex.next();
        Parser.expect(lex, TokenType.STRING);
        return new ShortenCommand(lex.current().getValue());
    }

    static private GoCommand parseGoCommand(Lexer lex){
        lex.next();
        Parser.expect(lex, TokenType.STRING);
        return new GoCommand(lex.current().getValue());
    }

    static void expect(Lexer lex, TokenType expected) {
        if(lex.current().getType() != expected)
            throw new Error("Lexer Error: Expected " + expected + " but got " + lex.current().getType());
    }
    static String helpString() {
        return "Commands:\n" +
                "\tq, quit - quit from service\n" +
                "\tl, list - list available user links\n" +
                "\ts, shorten - shorten link\n" +
                "\tg, go - go to link\n" +
                "\th, help - list available commands\n";
    }
}

public class ShortenerServiceCLI{
    public static void run(ServiceSettings settings, String[] args){
        Options options = new Options();

        Option user_opt = new Option("u", "user", true, "input file path");
        user_opt.setRequired(false);
        options.addOption(user_opt);

        CommandLine cmd = null;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("link-shortener", options);
            System.exit(1);
        }

        String user = Optional.ofNullable(cmd.getOptionValue("user")).orElse(System.getenv("USER"));
        UUID uid = UUID.nameUUIDFromBytes(user.getBytes());

        FileLinkRepository repo = new FileLinkRepository(Paths.get(settings.getRepositoryPath(), "repo").toString());
        ShortenerService svc = new ShortenerService(repo, new Base62Generator());
        boolean running = true;
        Scanner scanner = new Scanner(System.in);
        while(running){
            try {
                System.out.print(">>>");
                String input = scanner.nextLine();
                Expr expr = Parser.parse(input);
                switch (expr) {
                    case ListCommand command:
                        svc.listLinksByUser(uid).stream().map(LinkRecord::getShortUrl).forEach(System.out::println);
                        break;
                    case ShortenCommand command:
                        LinkRecord link =  svc.createShortLink(uid, command.link, settings.getMaxClics(),
                                                               settings.getTtl(), settings.getCodeLength(),
                                                               settings.getServiceAddress());
                        System.out.println("Shortened link: " + link.getShortUrl());
                        break;
                    case GoCommand command:
                        ResolveResult result = svc.resolveAndRegisterClick(command.link);
                        switch (result.getStatus()) {
                            case EXPIRED:
                            case NOT_FOUND:
                            case LIMIT_REACHED:
                                System.out.println(String.join("\n", svc.popNotifications(uid)));
                                break;
                            case OK:
                                List<String> notifications = svc.popNotifications(uid);
                                if(!notifications.isEmpty())
                                    System.out.println("Notifications:\n" +String.join("\n", notifications));
                                System.out.println("Opening " + result.getLongUrl() + "...");
                                Desktop.getDesktop().browse(URI.create(result.getLongUrl()));
                                break;
                        }
                        break;
                    case HelpCommand command:
                        System.out.println(Parser.helpString());
                        break;
                    case QuitCommand command:
                        running = false;
                        break;
                    case UnknownCommand unknownCommand:
                        throw new IllegalStateException("Unknown command: " + unknownCommand.command);
                    default:
                        throw new IllegalStateException("Unexpected value: " + expr);
                }
            } catch (Exception | Error e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
