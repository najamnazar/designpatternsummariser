package summarise.messages;

public class DesignPatternClassMessage extends Message {
    private String className;
    private String designPattern;
    private String relatedClassName;
    private String relatedClassDesignPattern;

    public DesignPatternClassMessage(String className, String designPattern) {
        this.className = className;
        this.designPattern = designPattern;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDesignPattern() {
        return designPattern;
    }

    public void setDesignPattern(String designPattern) {
        this.designPattern = designPattern;
    }

    public String getRelatedClassName() {
        return relatedClassName;
    }

    public void setRelatedClassName(String relatedClassName) {
        this.relatedClassName = relatedClassName;
    }

    public String getRelatedClassDesignPattern() {
        return relatedClassDesignPattern;
    }

    public void setRelatedClassDesignPattern(String relatedClassDesignPattern) {
        this.relatedClassDesignPattern = relatedClassDesignPattern;
    }
}
