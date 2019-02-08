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

import javax.xml.bind.annotation.*;
import java.math.BigInteger;


/**
 * Contient les informations relatives � l'�l�ve pour le module
 *                 "Communication avec la famille"
 *             
 * 
 * <p>Java class for VieScolaire complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VieScolaire">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="commentaire" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine600" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="nb-retards" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="nb-abs-justifiees" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="nb-abs-injustifiees" use="required" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *       &lt;attribute name="nb-heures-manquees" type="{http://www.w3.org/2001/XMLSchema}nonNegativeInteger" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VieScolaire", propOrder = {
    "commentaire"
})
public class VieScolaire {

    protected String commentaire;
    @XmlAttribute(name = "nb-retards", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nbRetards;
    @XmlAttribute(name = "nb-abs-justifiees", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nbAbsJustifiees;
    @XmlAttribute(name = "nb-abs-injustifiees", required = true)
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nbAbsInjustifiees;
    @XmlAttribute(name = "nb-heures-manquees")
    @XmlSchemaType(name = "nonNegativeInteger")
    protected BigInteger nbHeuresManquees;

    /**
     * Gets the value of the commentaire property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCommentaire() {
        return commentaire;
    }

    /**
     * Sets the value of the commentaire property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCommentaire(String value) {
        if(value != null){
            this.commentaire = value;
        }
    }

    /**
     * Gets the value of the nbRetards property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNbRetards() {
        return nbRetards;
    }

    /**
     * Sets the value of the nbRetards property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNbRetards(BigInteger value) {
        this.nbRetards = value;
    }

    /**
     * Gets the value of the nbAbsJustifiees property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNbAbsJustifiees() {
        return nbAbsJustifiees;
    }

    /**
     * Sets the value of the nbAbsJustifiees property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNbAbsJustifiees(BigInteger value) {
        this.nbAbsJustifiees = value;
    }

    /**
     * Gets the value of the nbAbsInjustifiees property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNbAbsInjustifiees() {
        return nbAbsInjustifiees;
    }

    /**
     * Sets the value of the nbAbsInjustifiees property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNbAbsInjustifiees(BigInteger value) {
        this.nbAbsInjustifiees = value;
    }

    /**
     * Gets the value of the nbHeuresManquees property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getNbHeuresManquees() {
        return nbHeuresManquees;
    }

    /**
     * Sets the value of the nbHeuresManquees property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setNbHeuresManquees(BigInteger value) {
        this.nbHeuresManquees = value;
    }

}
