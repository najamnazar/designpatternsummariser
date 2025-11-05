package dps.summarygenerator.messages;

import java.util.ArrayList;

public class DesignPatternMethodMessage extends Message {
    private String methodVerb;
    private String methodAction;
    private ArrayList<Object> methodModifier;
    private String methodReturn;
    private ArrayList<String> incomingMethod;
    private ArrayList<String> outgoingMethod;
    private ArrayList<String> overrideMethod;
    private String methodName;
    private String target;
    private String originateClass;
    private ArrayList<String> parameter;
    private String calledMethod;
    private ArrayList<String> callerMethodArr;
    private String callerMethod;
    private ArrayList<String> calledMethodArr;

    public ArrayList<Object> getMethodModifier() {
        return methodModifier;
    }

    public void setMethodModifier(ArrayList<Object> methodModifier) {
        this.methodModifier = methodModifier;
    }

    public String getMethodReturn() {
        return methodReturn;
    }

    public void setMethodReturn(String methodReturn) {
        this.methodReturn = methodReturn;
    }

    public ArrayList<String> getIncomingMethod() {
        return incomingMethod;
    }

    public void setIncomingMethod(ArrayList<String> incomingMethod) {
        this.incomingMethod = incomingMethod;
    }

    public ArrayList<String> getOutgoingMethod() {
        return outgoingMethod;
    }

    public void setOutgoingMethod(ArrayList<String> outgoingMethod) {
        this.outgoingMethod = outgoingMethod;
    }

    public String getMethodVerb() {
        return methodVerb;
    }

    public void setMethodVerb(String methodVerb) {
        this.methodVerb = methodVerb;
    }

    public String getMethodAction() {
        return methodAction;
    }

    public void setMethodAction(String methodAction) {
        this.methodAction = methodAction;
    }

    public ArrayList<String> getOverrideMethod() {
        return overrideMethod;
    }

    public void setOverrideMethod(ArrayList<String> overrideMethod) {
        this.overrideMethod = overrideMethod;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getOriginateClass() {
        return originateClass;
    }

    public void setOriginateClass(String originateClass) {
        this.originateClass = originateClass;
    }

    public ArrayList<String> getParameter() {
        return parameter;
    }

    public void setParameter(ArrayList<String> parameter) {
        this.parameter = parameter;
    }

    public String getCalledMethod() {
        return calledMethod;
    }

    public void setCalledMethod(String calledMethod) {
        this.calledMethod = calledMethod;
    }

    public ArrayList<String> getCallerMethodArr() {
        return callerMethodArr;
    }

    public void setCallerMethodArr(ArrayList<String> callerMethodArr) {
        this.callerMethodArr = callerMethodArr;
    }

    public String getCallerMethod() {
        return callerMethod;
    }

    public void setCallerMethod(String callerMethod) {
        this.callerMethod = callerMethod;
    }

    public ArrayList<String> getCalledMethodArr() {
        return calledMethodArr;
    }

    public void setCalledMethodArr(ArrayList<String> calledMethodArr) {
        this.calledMethodArr = calledMethodArr;
    }
}

