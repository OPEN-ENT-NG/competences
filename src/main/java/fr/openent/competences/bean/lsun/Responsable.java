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

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.07.06 at 05:11:16 PM CEST 
//


package fr.openent.competences.bean.lsun;

import fr.openent.competences.Competences;
import io.vertx.core.json.JsonArray;

import javax.xml.bind.annotation.*;
import java.util.regex.Pattern;


/**
 * D�crit un responsable d'un �l�ve
 *             
 * 
 * <p>Java class for Responsable complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Responsable">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="adresse" type="{urn:fr:edu:scolarite:lsun:bilans:import}Adresse" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="civilite" type="{urn:fr:edu:scolarite:lsun:bilans:import}Civilite" />
 *       &lt;attribute name="nom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *       &lt;attribute name="prenom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *       &lt;attribute name="legal1" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="legal2" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="lien-parente" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine40" />
 *       &lt;attribute name="denomination" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine600" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Responsable", propOrder = {
    "adresse"
})
public class Responsable {

    protected Adresse adresse;
    @XmlAttribute(name = "civilite")
    protected Civilite civilite;
    @XmlAttribute(name = "nom")
    protected String nom;
    @XmlAttribute(name = "prenom")
    protected String prenom;
    @XmlAttribute(name = "legal1")
    protected Boolean legal1;
    @XmlAttribute(name = "legal2")
    protected Boolean legal2;
    @XmlAttribute(name = "lien-parente")
    protected String lienParente;
    @XmlAttribute(name = "denomination")
    protected String denomination;
    @XmlTransient
    protected String externalId;


    public Responsable(){}
    /*Attention pour la civilité il faudra tenir compte de la class enum Civilte*/
    public Responsable(String externalId, Civilite civilite,String nom, String prenom, String relative, Adresse adresse ){
        this.externalId=externalId;
        this.civilite=civilite;
        this.nom=nom;
        this.prenom=prenom;
        this.addProprietesResponsable(relative);
        this.adresse=adresse;
    }
    public Responsable(String externalId, String nom, String prenom, String relative, Adresse adresse ){
        this.externalId=externalId;
        this.nom=nom;
        this.prenom=prenom;
        this.addProprietesResponsable(relative);
        this.adresse=adresse;
    }
    public Responsable (String externalId, String nom, String prenom, String relative){
        this.externalId=externalId;
        this.nom=nom;
        this.prenom=prenom;
        this.addProprietesResponsable(relative);
    }


    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    /**
     * Gets the value of the adresse property.
     *
     * @return possible object is
     * {@link Adresse }
     */
    public Adresse getAdresse() {
        return adresse;
    }

    /**
     * Sets the value of the adresse property.
     *
     * @param value allowed object is
     *              {@link Adresse }
     */
    public void setAdresse(Adresse value) {
        this.adresse = value;
    }

    /**
     * Gets the value of the civilite property.
     *
     * @return possible object is
     * {@link Civilite }
     */
    public Civilite getCivilite() {
        return civilite;
    }

    /**
     * Sets the value of the civilite property.
     *
     * @param civiliteNeo4j allowed object is
     */
    public void setCivilite(String civiliteNeo4j) {
        //expression regex vérifie que civiliteNeo4j commence par M ou m et contient un seul M ou m
        String regex = Competences.LSUN_CONFIG.getString("civilite_regex");

        if(civiliteNeo4j != null) {
            boolean isCiviliteMadame = Pattern.matches(regex,civiliteNeo4j);
            if (isCiviliteMadame) {
                this.civilite = Civilite.MME;
            } else {
                this.civilite = Civilite.M;
            }
        }

        //si civilite pas trouvee, on regarde à partir des liens de parente
        if(this.civilite == null) {
            if(this.lienParente != null) {
                if(this.lienParente.equals(Competences.LIEN_PERE)) {
                    this.civilite = Civilite.M;
                } else if(this.lienParente.equals(Competences.LIEN_MERE)) {
                    this.civilite = Civilite.MME;
                }
            }
        }

        // si tojours pas de civilite, on en met une par defaut
        if(this.civilite == null) {
            //civilite par defaut
            this.civilite = Civilite.M;
        }
    }

    /**
     * Gets the value of the nom property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getNom() {
        return nom;
    }

    /**
     * Sets the value of the nom property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setNom(String value) {
        this.nom = value;
    }

    /**
     * Gets the value of the prenom property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPrenom() {
        return prenom;
    }

    /**
     * Sets the value of the prenom property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPrenom(String value) {
        this.prenom = value;
    }

    /**
     * Gets the value of the legal1 property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public Boolean isLegal1() {
        return legal1;
    }

    /**
     * Sets the value of the legal1 property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setLegal1(Boolean value) {
        this.legal1 = value;
    }

    /**
     * Gets the value of the legal2 property.
     *
     * @return possible object is
     * {@link Boolean }
     */
    public Boolean isLegal2() {
        return legal2;
    }

    /**
     * Sets the value of the legal2 property.
     *
     * @param value allowed object is
     *              {@link Boolean }
     */
    public void setLegal2(Boolean value) {
        this.legal2 = value;
    }

    /**
     * Gets the value of the lienParente property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getLienParente() {
        return lienParente;
    }

    /**
     * Sets the value of the lienParente property.
     *
     * @param codeParent allowed object is
     *              {@link String }
     */
   /* public void setLienParente(String value) {
        this.lienParente = value;
    }*/

    public void setLienParente(String codeParent) {

        switch (codeParent) {
            case "20":
                this.lienParente = Competences.LIEN_PERE;
                break;
            case "10":
                this.lienParente = Competences.LIEN_MERE;
                break;
            case "50":
                this.lienParente = Competences.LIEN_TUTEUR;
                break;
            case "39":
                this.lienParente = Competences.LIEN_FAMILLE;
                break;
            case "51":
                this.lienParente = Competences.LIEN_SOCIALE;
                break;
            case "90":
                this.lienParente = Competences.LIEN_AUTRE;
                break;
            case "70":
                this.lienParente = Competences.LIEN_ELEVE;
                break;
            case "37":
                this.lienParente = Competences.LIEN_FRATRIE;
                break;
            case "38":
                this.lienParente = Competences.LIEN_ASCENDANT;
                break;
            case "41":
                this.lienParente = Competences.LIEN_EDUCATEUR;
                break;
            case "42":
                this.lienParente = Competences.LIEN_ASSISTANT_FAMILIAL;
                break;
            case "43":
                this.lienParente = Competences.LIEN_GARDE_ENFANT;
                break;
            default:
                break;
        }
    }

    public void setLegals(String code){
        switch (code) {
            case "0":
                setLegal1(false);
                setLegal2(false);
                break;
            case "1":
                setLegal1(true);
                setLegal2(false);
                break;
            case "2":
                setLegal1(false);
                setLegal2(true);
                break;
            default:
                break;
        }

    }
    //méthode qui permet de compléter les attributs legal1, legal2 et lienParente
    //relative = externalId$type_relation$resp_financier$resp_legal$contact$paiement
    public void addProprietesResponsable(String relative){

        String[] paramRelative = relative.toString().split("\\$");
        if (this.externalId.equals(paramRelative[0])) {
            this.setLienParente(paramRelative[1]);
            this.setLegals(paramRelative[3]);
        }
    }

    /**
     * Obtient la valeur de la propriete denomination.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getDenomination() {
        return denomination;
    }

    /**
     * Definit la valeur de la propriete denomination.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setDenomination(String value) {
        this.denomination = value;
    }

}
