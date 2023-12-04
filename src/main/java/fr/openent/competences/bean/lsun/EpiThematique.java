/*
 * Copyright (c) Région Hauts-de-France, Département de la Seine-et-Marne, CGI, 2016.
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */

package fr.openent.competences.bean.lsun;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Contient l'intitulé, la thématique, la description et les disciplines
 *                 de l'EPI
 *                 (enseignement pratique inter-disciplinaire)
 *
 *
 * <p>Classe Java pour EpiThematique complex type.
 *
 * <p>Le fragment de schema suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="EpiThematique">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="code" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeThematiqueEpi" />
 *       &lt;attribute name="libelle" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine100" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EpiThematique")
public class EpiThematique {

    @XmlAttribute(name = "code", required = true)
    protected String code;
    @XmlAttribute(name = "libelle", required = true)
    protected String libelle;

    /**
     * Obtient la valeur de la propriété code.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCode() {
        return code;
    }

    /**
     * Définit la valeur de la propriété code.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCode(String value) {
        this.code = value;
    }

    /**
     * Obtient la valeur de la propriété libelle.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * D�finit la valeur de la propriété libelle.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setLibelle(String value) {
        this.libelle = value;
    }

}
