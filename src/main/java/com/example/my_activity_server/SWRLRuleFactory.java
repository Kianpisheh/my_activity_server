package com.example.my_activity_server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.SWRLAtom;
import org.semanticweb.owlapi.model.SWRLBuiltInAtom;
import org.semanticweb.owlapi.model.SWRLClassAtom;
import org.semanticweb.owlapi.model.SWRLDArgument;
import org.semanticweb.owlapi.model.SWRLDataPropertyAtom;
import org.semanticweb.owlapi.model.SWRLDataRangeAtom;
import org.semanticweb.owlapi.model.SWRLObjectPropertyAtom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.semanticweb.owlapi.model.SWRLVariable;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.semanticweb.owlapi.vocab.SWRLBuiltInsVocabulary;

import openllet.owlapi.SWRL;
import openllet.owlapi.XSD;

public class SWRLRuleFactory {

    public static SWRLRule getSWRLRuleFromString(String bodyString, String headString,
            List<List<String>> OREvents,
            OWLOntologyManager manager,
            OWLOntology ontology,
            PrefixManager pm) {

        OWLDataFactory df = manager.getOWLDataFactory();
        Stream<SWRLAtom> body = Stream.empty();
        Stream<SWRLAtom> head = Stream.empty();
        try {
            body = createSWRLAtoms(bodyString, ontology, manager.getOWLDataFactory(), OREvents, pm, "urn:swrl:var");
            head = createSWRLAtoms(headString, ontology, manager.getOWLDataFactory(), null, pm, "urn:swrl:var");
        } catch (Exception e) {
            e.printStackTrace();

        }
        SWRLRule rule = null;
        List<SWRLAtom> bodyList = body.collect(Collectors.toList());
        List<SWRLAtom> headList = head.collect(Collectors.toList());
        if (bodyList.size() > 0 && headList.size() > 0) {
            rule = df.getSWRLRule(bodyList, headList);
        }

        return rule;
    }

    public static Stream<SWRLAtom> createSWRLAtoms(String expressionStr, OWLOntology ontology, OWLDataFactory df,
            List<List<String>> OREvents, PrefixManager pm, String swrlStr) throws Exception {

        List<SWRLAtom> atoms = new ArrayList<>();

        Map<String, List<String>> declarations = getOntologyDeclarations(ontology);
        declarations.put("builtins", new ArrayList<String>() {
            {
                add("subtract");
                add("greaterThan");
                add("lessThan");
            }
        });
        String[] atomStrings = expressionStr.split("\\^");

        int eg = 0; // event group idx
        for (int i = 0; i < atomStrings.length; i++) {
            if (declarations.get("ObjectProperty").contains(atomStrings[i].split("\\(")[0])) {
                atoms.add(createObjectPropertyAtom(atomStrings[i], swrlStr, pm, df));
            } else if (declarations.get("DataProperty").contains(atomStrings[i].split("\\(")[0])) {
                atoms.add(createDataPropertyAtom(atomStrings[i], swrlStr, pm, df));
            } else if (declarations.get("builtins").contains(atomStrings[i].split("\\(")[0])) {
                atoms.add(createBuiltInAtom(atomStrings[i], swrlStr, pm));
            } else if (!atomStrings[i].contains(",")) {
                // create the EventGroup class if does not exist
                if (atomStrings[i].startsWith(Predicate.EVENTGROUP)) {
                    Object[] res = addEventGroupNecessaryAxioms(atomStrings[i], OREvents.get(eg), ontology, df, pm,
                            swrlStr);
                    ontology = (OWLOntology) res[1];
                    SWRLClassAtom atom = (SWRLClassAtom) res[0];
                    atoms.add(atom);
                    eg += 1;
                } else {
                    atoms.add(createClassAtom(atomStrings[i], swrlStr, pm, df));
                }
            } else {
                throw new Exception("Atom is not supported");
            }
        }

        return atoms.stream();

    }

    private static Object[] addEventGroupNecessaryAxioms(String evGroupStr, List<String> OREvents,
            OWLOntology ontology, OWLDataFactory df,
            PrefixManager pm, String swrlStr) {
        // precondition: there is no redundant EventGroup in the ontology

        // create and add the given event group as the subset of the Event class
        OWLClass eventClass = df.getOWLClass(":Event", pm);
        OWLClass eventGroupClass = df.getOWLClass(":" + evGroupStr.split("\\(")[0], pm);
        OWLSubClassOfAxiom subClassaxiom = df.getOWLSubClassOfAxiom(eventGroupClass, eventClass);
        ontology.add(subClassaxiom);
        //
        List<OWLClass> subEvents = new ArrayList<>();
        for (String eventGroupMember : OREvents) {
            subEvents.add(df.getOWLClass(":" + eventGroupMember, pm));
        }

        String evGroupClassName = evGroupStr.split("\\(")[0];
        OWLEquivalentClassesAxiom OREventAxiom = df.getOWLEquivalentClassesAxiom(
                df.getOWLClass(":" + evGroupClassName, pm), df.getOWLObjectUnionOf(subEvents));
        ontology.add(OREventAxiom);
        String variableName = evGroupStr.substring(evGroupStr.indexOf("(") + 1, evGroupStr.indexOf(")"));
        SWRLClassAtom atom = SWRL.classAtom(df.getOWLClass(":" + evGroupClassName, pm),
                SWRL.variable(IRI.create(swrlStr + "#" + variableName)));

        Object[] res = { atom, ontology };
        return res;
    }

    public static Map<String, List<String>> getOntologyDeclarations(OWLOntology ontology) {

        Map<String, List<String>> declarations = new HashMap<>();
        declarations.put("Class", new ArrayList<String>());
        declarations.put("ObjectProperty", new ArrayList<String>());
        declarations.put("DataProperty", new ArrayList<String>());

        ontology.axioms().forEach(ax -> {
            if (ax.getAxiomType() == AxiomType.DECLARATION) {
                String str = ax.toString();
                String key = str.split("\\(")[1];
                String value = str.split("#")[1].split(">")[0];
                if (declarations.containsKey(key)) {
                    declarations.get(key).add(value);
                }
            }
        });
        declarations.get("Class").add(ActivityList.SINGLE_EVENT_ACTIVITY);
        return declarations;
    }

    public static SWRLClassAtom createClassAtom(String classAtomStr, String swrlStr, PrefixManager pm,
            OWLDataFactory df) {

        String className = classAtomStr.split("\\(")[0];
        String variableName = classAtomStr.substring(classAtomStr.indexOf("(") + 1, classAtomStr.indexOf(")"));
        return SWRL.classAtom(df.getOWLClass(":" + className, pm),
                SWRL.variable(IRI.create(swrlStr + "#" + variableName)));
    }

    public static SWRLDataPropertyAtom createDataPropertyAtom(String dataPropStr, String swrlStr,
            PrefixManager pm, OWLDataFactory df) {

        String propName = dataPropStr.split("\\(")[0];
        String classVariableName = dataPropStr.substring(dataPropStr.indexOf("(") + 1, dataPropStr.indexOf(","));
        String dataVariableName = dataPropStr.substring(dataPropStr.indexOf(",") + 1, dataPropStr.indexOf(")"));

        OWLDataPropertyExpression dataPropExpression = df.getOWLDataProperty(":" + propName, pm);
        SWRLVariable classVariable = SWRL.variable(IRI.create(swrlStr + "#" + classVariableName));
        SWRLVariable dataVariable = SWRL.variable(IRI.create(swrlStr + "#" + dataVariableName));

        return SWRL.propertyAtom(dataPropExpression, classVariable, dataVariable);
    }

    public static SWRLObjectPropertyAtom createObjectPropertyAtom(String objPropStr, String swrlStr, PrefixManager pm,
            OWLDataFactory df) {

        String propName = objPropStr.split("\\(")[0];
        String classVariableName1 = objPropStr.substring(objPropStr.indexOf("(") + 1, objPropStr.indexOf(","));
        String classVariableName2 = objPropStr.substring(objPropStr.indexOf(",") + 1, objPropStr.indexOf(")"));

        OWLObjectPropertyExpression objPropExpression = df.getOWLObjectProperty(":" + propName, pm);
        SWRLVariable classVariable1 = SWRL.variable(IRI.create(swrlStr + "#" + classVariableName1));
        SWRLVariable classVariable2 = SWRL.variable(IRI.create(swrlStr + "#" + classVariableName2));

        return SWRL.propertyAtom(objPropExpression, classVariable1, classVariable2);
    }

    public static SWRLBuiltInAtom createBuiltInAtom(String atomStr, String swrlStr, PrefixManager pm) {

        // extract args
        List<SWRLDArgument> args = new ArrayList<>();
        String[] ss = atomStr.split(",");
        int ii = ss[0].indexOf("(");
        ss[0] = ss[0].substring(ii + 1, ss[0].length());
        ss[ss.length - 1] = ss[ss.length - 1].substring(0, ss[ss.length - 1].length() - 1);

        for (int i = 0; i < ss.length; i++) {
            if (isNumeric(ss[i])) {
                SWRLDArgument arg = SWRL.constant(ss[i], XSD.INTEGER);
                args.add(arg);
            } else {
                args.add(SWRL.variable(IRI.create(swrlStr + "#" + ss[i])));
            }
        }

        // parse builtins
        String atomName = atomStr.split("\\(")[0];
        SWRLBuiltInsVocabulary builtInVocab = null;

        switch (atomName) {
            case "subtract":
                builtInVocab = SWRLBuiltInsVocabulary.SUBTRACT;
                break;
            case "add":
                builtInVocab = SWRLBuiltInsVocabulary.ADD;
                break;
            case "mod":
                builtInVocab = SWRLBuiltInsVocabulary.MOD;
                break;
            case "greaterThan":
                builtInVocab = SWRLBuiltInsVocabulary.GREATER_THAN;
                break;
            case "lessThan":
                builtInVocab = SWRLBuiltInsVocabulary.LESS_THAN;
                break;
            default:
                break;
        }

        return SWRL.builtIn(builtInVocab, args);
    }

    public static boolean isNumeric(String strNum) {
        if (strNum == null) {
            return false;
        }
        try {
            double d = Double.parseDouble(strNum);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

}
