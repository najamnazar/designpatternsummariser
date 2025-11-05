package dps.summarygenerator;

import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.*;
import simplenlg.realiser.english.Realiser;
import dps.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ClassInterfaceSummariser {
    public String generateClassDescription(NLGFactory nlgFactory, Realiser realiser, HashMap classDetail,
            HashSet<String> designPatternDescriptionCollect) {

        // retrieve the details from the json file
        String classModifier = retrieveModifiers(classDetail);
        String className = Utils.getClassName(classDetail);
        String classExtends = retrieveExtends(classDetail);
        String classImplements = retrieveImplements(classDetail);
        boolean isInterfaceOrNot = Utils.isInterfaceOrNot(classDetail);

        SPhraseSpec classDescription = nlgFactory.createClause();
        VPPhraseSpec verbBe = nlgFactory.createVerbPhrase("be");

        NPPhraseSpec classType;

        // if it is an interface
        if (isInterfaceOrNot) {
            classType = nlgFactory.createNounPhrase("interface");
        } else {
            classType = nlgFactory.createNounPhrase("class");
        }

        // add details to the sentence
        classType.addPreModifier(classModifier);
        classType.setDeterminer("a");
        classDescription.setSubject("It");
        classDescription.setVerb(verbBe);
        classDescription.setObject(classType);

        CoordinatedPhraseElement implementsAndExtends = nlgFactory.createCoordinatedPhrase();

        // if the class is extended from another class, add the base class
        if (!classExtends.equals("")) {
            SPhraseSpec classExtendsPhrase = nlgFactory.createClause();

            VPPhraseSpec verbExtend = nlgFactory.createVerbPhrase("extend");
            NPPhraseSpec objectExtend = nlgFactory.createNounPhrase(classExtends);

            classExtendsPhrase.setVerb(verbExtend);
            classExtendsPhrase.setObject(objectExtend);
            implementsAndExtends.addCoordinate(classExtendsPhrase);
        }

        // if the class implements an interface, add the interface details
        if (!classImplements.equals("")) {
            SPhraseSpec classImplementsPhrase = nlgFactory.createClause();

            VPPhraseSpec verbImplement = nlgFactory.createVerbPhrase("implement");
            NPPhraseSpec objectImplement = nlgFactory.createNounPhrase(classImplements);

            classImplementsPhrase.setVerb(verbImplement);
            classImplementsPhrase.setObject(objectImplement);
            implementsAndExtends.addCoordinate(classImplementsPhrase);
        }

        classDescription.addComplement(implementsAndExtends);

        String classDescriptionSentence = realiser.realiseSentence(classDescription);

        // add design pattern description
        String designPatternDescriptions = String.join(" ", designPatternDescriptionCollect);
        if (designPatternDescriptions.equals("")) {
            designPatternDescriptions = className + " does not have any design pattern. ";
        }

        return designPatternDescriptions + " " + classDescriptionSentence;
    }

    // retrieve the base class information, if any
    private String retrieveExtends(HashMap classDetail) {
        ArrayList<String> classExtendsArray = Utils.getExtendsFrom(classDetail);
        if (classExtendsArray.size() != 0)
            return classExtendsArray.get(0);
        return "";
    }

    // retrieve the class modifier information
    private String retrieveModifiers(HashMap classDetail) {
        StringBuilder modifiers = new StringBuilder();
        for (String modifier : Utils.getClassModifierType(classDetail))
            modifiers.append(modifier).append(" ");
        return modifiers.toString();
    }

    // retrieve the implemented interface information
    private String retrieveImplements(HashMap classDetail) {
        StringBuilder interfaces = new StringBuilder();
        ArrayList<String> interfaceArray = Utils.getImplementsFrom(classDetail);

        if (interfaceArray.size() == 1) {
            return interfaceArray.get(0);
        } else if (interfaceArray.size() == 2) {
            interfaces = new StringBuilder(interfaceArray.get(0) + " and " + interfaceArray.get(1));
            return interfaces.toString();
        } else if (interfaceArray.size() > 2) {
            interfaces.append(interfaceArray.get(0));
            for (int i = 1; i < interfaceArray.size() - 1; i++) {
                interfaces.append(", ").append(interfaceArray.get(i));
            }
            interfaces.append(" and ").append(interfaceArray.get(interfaceArray.size() - 1));
            return interfaces.toString();
        }
        return "";
    }
}

