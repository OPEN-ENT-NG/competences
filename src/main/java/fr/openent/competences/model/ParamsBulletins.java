package fr.openent.competences.model;

public class ParamsBulletins {
    private String imGraph;
    private  boolean hasGraphPerDomaine;
    private static final String GRAPH_PER_DOMAINE = "graphPerDomaine";

    public String getImGraph() {
        return imGraph;
    }

    public void setImGraph(String imGraph) {
        this.imGraph = imGraph;
    }

    public boolean isHasGraphPerDomaine() {
        return hasGraphPerDomaine;
    }

    public void setHasGraphPerDomaine(boolean hasGraphPerDomaine) {
        this.hasGraphPerDomaine = hasGraphPerDomaine;
    }
}
