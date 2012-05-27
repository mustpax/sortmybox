package models;

public class TooManyRulesException extends RuntimeException {
    public TooManyRulesException(String msg) {
        super(msg);
    }
}
