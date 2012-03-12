package models;

import java.util.Date;

import siena.Id;
import siena.Model;
import siena.Query;

public class Move extends Model {
    @Id
    public Long id;

    public String from;

    public String to;

    public Date when;
    
    public Long owner;
    
    public Move(Rule rule, String from) {
        this.owner = rule.owner;
        this.to = rule.dest;
        this.from = from;
        this.when = new Date();
    }
    
    public static Query<Move> all() {
        return Model.all(Move.class);
    }
    
    @Override
    public String toString() {
        return String.format("Moved file '%s' to '%s' at %s",
			                 this.from, this.to, this.when);
    }
}
