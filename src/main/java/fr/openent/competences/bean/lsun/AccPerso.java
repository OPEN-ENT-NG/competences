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
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;


/**
 * Contient l'intitul�, la description et les disciplines de
 *             l'accompagnement personnalis�
 *             
 * 
 * <p>Java class for AccPerso complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="AccPerso">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="description" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine600" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="intitule" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine150" />
 *       &lt;attribute name="discipline-refs" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREFS" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccPerso", propOrder = {
    "description"
})
public class AccPerso {

    protected String description;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "intitule", required = true)
    protected String intitule;
    @XmlAttribute(name = "discipline-refs", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREFS")
    protected List<Object> disciplineRefs;

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String value) {
        this.description = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the intitule property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIntitule() {
        return intitule;
    }

    /**
     * Sets the value of the intitule property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIntitule(String value) {
        this.intitule = value;
    }

    /**
     * Gets the value of the disciplineRefs property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the disciplineRefs property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDisciplineRefs().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Object }
     * 
     * 
     */
    public List<Object> getDisciplineRefs() {
        if (disciplineRefs == null) {
            disciplineRefs = new ArrayList<Object>();
        }
        return this.disciplineRefs;
    }

}
