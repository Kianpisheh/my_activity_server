package com.example.my_activity_server;

import java.util.List;

import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.vocab.OWLFacet;

public class AxiomType {
    public static final int INTERACTION = 0;
    public static final int INTERACTION_DURATION_LESS = 1;
    public static final int INTERACTION_DURATION_MORE = 2;
    public static final int INTERACTION_DURATION_LESS_MORE = 3;
    public static final int INTERACTION_ORDER = 4;
    public static final int INTERACTION_TIME_DISTANCE_LESS = 5;
    public static final int INTERACTION_TIME_DISTANCE_MORE = 6;
    public static final int INTERACTION_TIME_DISTANCE_LESS_MORE = 7;

    public static int getType(int numEvents, List<OWLFacet> facets, List<OWLLiteral> facetValues) {

        int type = -1;
        int facetNum = facets.size();

        if (facetNum == 0)
            type = INTERACTION;

        else if (facetNum == 1) {
            String facetType = facets.get(0).getShortForm();
            if (facetType.equals("MIN_INCLUSIVE") & (numEvents == 1))
                type = INTERACTION_DURATION_MORE;
            else if (facetType.equals("MIN_INCLUSIVE") & (numEvents == 2))
                type = INTERACTION_TIME_DISTANCE_MORE;
            else if (facetType.equals("MAX_INCLUSIVE") & (numEvents == 1))
                type = INTERACTION_DURATION_LESS;
            else if (facetType.equals("MAX_INCLUSIVE") & (numEvents == 2))
                type = INTERACTION_TIME_DISTANCE_LESS;
        }

        else if (facetNum == 2) {
            if (numEvents == 1)
                type = INTERACTION_DURATION_LESS_MORE;
            else if (numEvents == 2)
                type = INTERACTION_TIME_DISTANCE_LESS_MORE;
        }

        if (type == -1) {
            System.err.println("proposition type not recognized.");
        }

        return type;
    }

}
