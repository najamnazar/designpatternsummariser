package swum.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the SWUM (Software Word Usage Model) parse tree.
 * Each node has a type, word, and optional children.
 */
public class SWUMNode {
    
    public enum NodeType {
        VERB_PHRASE,     // VP - Action or operation
        NOUN_PHRASE,     // NP - Object or entity
        PREPOSITION,     // P - Relationship
        DETERMINER,      // D - Article (the, a, an)
        SUBJECT,         // S - Subject of action
        OBJECT,          // O - Object of action
        MODIFIER,        // M - Adjective or adverb
        CONJUNCTION,     // C - And, or, but
        TERMINAL         // Terminal word
    }
    
    private NodeType type;
    private String word;
    private List<SWUMNode> children;
    private SWUMNode parent;
    private String pos; // Part of speech
    
    public SWUMNode(NodeType type, String word) {
        this.type = type;
        this.word = word;
        this.children = new ArrayList<>();
        this.pos = "";
    }
    
    public SWUMNode(NodeType type) {
        this(type, "");
    }
    
    // Getters and setters
    public NodeType getType() { return type; }
    public void setType(NodeType type) { this.type = type; }
    
    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }
    
    public List<SWUMNode> getChildren() { return children; }
    public void addChild(SWUMNode child) { 
        this.children.add(child); 
        child.setParent(this);
    }
    
    public SWUMNode getParent() { return parent; }
    public void setParent(SWUMNode parent) { this.parent = parent; }
    
    public String getPos() { return pos; }
    public void setPos(String pos) { this.pos = pos; }
    
    public boolean isTerminal() {
        return type == NodeType.TERMINAL;
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
    
    /**
     * Returns the yield (terminal words) of this subtree
     */
    public List<String> getYield() {
        List<String> yield = new ArrayList<>();
        if (isTerminal()) {
            if (!word.isEmpty()) {
                yield.add(word);
            }
        } else {
            for (SWUMNode child : children) {
                yield.addAll(child.getYield());
            }
        }
        return yield;
    }
    
    /**
     * Returns a string representation of the tree structure
     */
    public String toTreeString() {
        if (isTerminal()) {
            return word.isEmpty() ? type.toString() : word;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(type);
        if (!word.isEmpty()) {
            sb.append(" ").append(word);
        }
        for (SWUMNode child : children) {
            sb.append(" ").append(child.toTreeString());
        }
        sb.append(")");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.join(" ", getYield());
    }
}