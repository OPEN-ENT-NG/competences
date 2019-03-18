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

/**
 * Created by ledunoiss on 05/08/2016.
 */
public class NoteDevoir {
    /**
     * Valeur de la note
     */
    private Double note;

    /**
     * Sur combien est la note.
     */
    private Double diviseur;

    /**
     * Booleen pour savoir s'il faut ramner la note sur le diviseur.
     */
    private Boolean ramenerSur;

    /**
     * Coefficient de la note.
     */
    private Double coefficient;

    /**
     * identifiant de l'élève à qui appartient la note.
     */
    private String idEleve;

    public static final Double DIVISEUR_DEFAULT_VALUE = 20.0;

    private Long idPeriode;

    /**
     * @param note valeur de la note
     * @param diviseur sur combien est la note.
     * @param ramenerSur booleen pour savoir s'il faut ramner la note sur le diviseur.
     * @param coefficient coefficient de la note.
     */
    public NoteDevoir(Double note, Double diviseur, Boolean ramenerSur, Double coefficient) {
        this.note = note;
        this.diviseur = diviseur;
        this.ramenerSur = ramenerSur;
        this.coefficient = coefficient;
    }

    /**
     * @param note valeur de la note
     * @param diviseur sur combien est la note.
     * @param ramenerSur booleen pour savoir s'il faut ramner la note sur le diviseur.
     * @param coefficient coefficient de la note.
     * @param idEleve identifiant de l'élève à qui appartient la note.
     */
    public NoteDevoir(Double note, Double diviseur, Boolean ramenerSur, Double coefficient, String idEleve) {
        this.note = note;
        this.diviseur = diviseur;
        this.ramenerSur = ramenerSur;
        this.coefficient = coefficient;
        this.idEleve = idEleve;
    }

    /**
     * Construis un objet {@link NoteDevoir} avec diviseur initialisé à 20
     *
     * @param note valeur de la note
     * @param ramenerSur booleen pour savoir s'il faut ramner la note sur le diviseur.
     * @param coefficient coefficient de la note.
     */
    public NoteDevoir(Double note, Boolean ramenerSur, Double coefficient) {
        this.note = note;
        this.diviseur = NoteDevoir.DIVISEUR_DEFAULT_VALUE;
        this.ramenerSur = ramenerSur;
        this.coefficient = coefficient;
    }

    public NoteDevoir(Double note, Double diviseur, Boolean ramenerSur, Double coefficient,String idEleve, Long idPeriode) {
        this.note = note;
        this.diviseur = diviseur;
        this.ramenerSur = ramenerSur;
        this.coefficient = coefficient;
        this.idPeriode = idPeriode;
        this.idEleve = idEleve;
    }


    public Double getNote() {
        return note;
    }

    public void setNote(Double note) {
        this.note = note;
    }

    public Double getDiviseur() {
        return diviseur;
    }

    public void setDiviseur(Double diviseur) {
        this.diviseur = diviseur;
    }

    public Boolean getRamenerSur() {
        return ramenerSur;
    }

    public void setRamenerSur(Boolean ramenerSur) {
        this.ramenerSur = ramenerSur;
    }

    public Double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(Double coefficient) {
        this.coefficient = coefficient;
    }

    public String getIdEleve() {
        return idEleve;
    }

    public void setIdEleve(String idEleve) {
        this.idEleve = idEleve;
    }

    public Long getIdPeriode() {return this.idPeriode;}

}
