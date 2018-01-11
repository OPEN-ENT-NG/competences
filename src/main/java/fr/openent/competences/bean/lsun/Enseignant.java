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
import java.math.BigInteger;


/**
 * Contient les informations relatives � l'enseignant
 *             
 * 
 * <p>Java class for Enseignant complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Enseignant">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="type" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}TypeEnseignant" />
 *       &lt;attribute name="id-sts" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *       &lt;attribute name="civilite" type="{urn:fr:edu:scolarite:lsun:bilans:import}Civilite" />
 *       &lt;attribute name="nom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *       &lt;attribute name="prenom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Enseignant")
public class Enseignant {

    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "type", required = true)
    protected TypeEnseignant type;
    @XmlAttribute(name = "id-sts", required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger idSts;
    @XmlAttribute(name = "civilite")
    protected Civilite civilite;
    @XmlAttribute(name = "nom")
    protected String nom;
    @XmlAttribute(name = "prenom")
    protected String prenom;

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
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link TypeEnseignant }
     *     
     */
    public TypeEnseignant getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link TypeEnseignant }
     *     
     */
    public void setType(TypeEnseignant value) {
        this.type = value;
    }

    /**
     * Gets the value of the idSts property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIdSts() {
        return idSts;
    }

    /**
     * Sets the value of the idSts property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setIdSts(BigInteger value) {
        this.idSts = value;
    }

    /**
     * Gets the value of the civilite property.
     * 
     * @return
     *     possible object is
     *     {@link Civilite }
     *     
     */
    public Civilite getCivilite() {
        return civilite;
    }

    /**
     * Sets the value of the civilite property.
     * 
     * @param value
     *     allowed object is
     *     {@link Civilite }
     *     
     */
    public void setCivilite(Civilite value) {
        this.civilite = value;
    }

    /**
     * Gets the value of the nom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNom() {
        return nom;
    }

    /**
     * Sets the value of the nom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNom(String value) {
        this.nom = value;
    }

    /**
     * Gets the value of the prenom property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrenom() {
        return prenom;
    }

    /**
     * Sets the value of the prenom property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrenom(String value) {
        this.prenom = value;
    }

}
