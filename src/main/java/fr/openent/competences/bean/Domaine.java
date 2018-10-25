/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 *     This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as
 *   published by the Free Software Foundation (version 3 of the License).
 *   For the sake of explanation, any module that communicate over native
 *   Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 *   license and could be license under its own terms. This is merely considered
 *   normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rollinq
 *
 * Domaine de compétences.
 * Implémente plusieurs méthodes de manipulation des domaines et des ensembles de compétences.
 *
 */
public class Domaine {

    private Long id;

    private Domaine domaine_parent;

    private List<Domaine> sousDomaines;

    private List<Long> competences;

    private boolean is_evaluated;

    /**
     * Constructeur de la classe Domaine.
     *
     * @param id            Id du domaine.
     * @param is_evaluated  Est-ce que le domaine est evalue ou non.
     */
    public Domaine(Long id, boolean is_evaluated) {
        this.id = id;
        this.domaine_parent = null;
        this.sousDomaines = new ArrayList<>();
        this.competences = new ArrayList<>();
        this.is_evaluated = is_evaluated;
    }

    /**
     * Retourne l'id du domaine.
     *
     * @return l'id du domaine.
     */
    public Long getId() {
        return id;
    }

    /**
     * Attribue un domaine parent à ce domaine, s'il en possède un.
     *
     * @param d  Domaine parent du ce domaine.
     */
    public void addParent(Domaine d) {
        this.domaine_parent = d;
    }

    /**
     * Retourne le domaine parent racine de ce domaine.
     * Si ce domaine est evalue et qu'il ne possède aucun ancetre qui l'est, se retourne lui-meme.
     * Si un de ses ancetre est evalue et ne possede lui-meme pas d'ancetre evalue, retourne cet ancetre.
     * Si ce domaine n'est pas evalue et ne possede aucun ancetre evalue, retourne null.
     *
     * @return this si ce domaine ne possede aucun ancetre evalue et qu'il est evalue;
     *         null si ce domaine ne possede aucun ancetre et qu'il n'est pas evalue;
     *         Son ancetre evalue sinon.
     */
    public Domaine getParentRacine() {
        if(this.domaine_parent != null) {
            Domaine result = this.domaine_parent.getParentRacine();
            if (result != null) {
                return result;
            } else {
                return this.is_evaluated ? this : null;
            }
        } else {
            return this.is_evaluated ? this : null;
        }
    }

    /**
     * Ajoute un sous-domaine à ce domaine. Permet la gestion d'un arborescence de domaine.
     *
     * @param d  Le sous-domaine a ajoute.
     */
    public void addSousDomaine(Domaine d) {
        this.sousDomaines.add(d);
    }

    /**
     * Ajoute une competence a la liste des competences evaluees par ce domaine.
     *
     * @param competence  L'id de la competence a ajoute a la collection de competence de ce domaine.
     */
    public void addCompetence(Long competence) {
        this.competences.add(competence);
    }

    /**
     * Retourne l'ensemble des competences de ce domaine ainsi que de ses sous-domaines.
     *
     * @return  l'ensemble des competences de ce domaine ainsi que de ses sous-domaines.
     */
    public List<Long> getCompetences() {
        List<Long> result = new ArrayList<>(this.competences);
        for(Domaine d : this.sousDomaines) {
            result.addAll(d.getCompetences());
        }
        return result;
    }

    /**
     * Retourne un booleen indiquant si ce domaine est evalue.
     *
     * @return un booleen indiquant si ce domaine est evalue.
     */
    public boolean isEvaluated() {
        return this.is_evaluated;
    }

}
