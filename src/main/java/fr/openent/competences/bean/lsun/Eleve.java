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
import java.util.ArrayList;
import java.util.List;


/**
 * La description de l'�l�ve avec ses informations de debogage (nom, prenom)
 *             
 * 
 * <p>Java class for Eleve complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Eleve">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="id-be" use="required" type="{http://www.w3.org/2001/XMLSchema}positiveInteger" />
 *       &lt;attribute name="nom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *       &lt;attribute name="prenom" type="{urn:fr:edu:scolarite:lsun:bilans:import}NomPrenom" />
 *       &lt;attribute name="code-division" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeStructure" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Eleve")
public class Eleve {

    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "id-be", required = true)
    @XmlSchemaType(name = "positiveInteger")
    protected BigInteger idBe;
    @XmlAttribute(name = "nom")
    protected String nom;
    @XmlAttribute(name = "prenom")
    protected String prenom;
    @XmlAttribute(name = "code-division", required = true)
    protected String codeDivision;
    @XmlTransient
    protected  String idNeo4j;
    @XmlTransient
    protected List<Responsable> responsableList;
    @XmlTransient
    protected String id_Class;


    @XmlTransient
    protected String level;




    public Eleve() {
        responsableList = new ArrayList<>();
    }
    public Eleve(String externalId,String attachementId,String firstName, String lastName, String nameClass, String idNeo4j,String idClass,String level){
        this.id="EL_"+externalId;
        this.idBe=new BigInteger(attachementId);
        this.prenom=firstName;
        this.nom=lastName;
        this.codeDivision=nameClass;
        this.idNeo4j=idNeo4j;
        this.id_Class=idClass;
        this.level = level;
        responsableList = new ArrayList<>();
    }
    public String getId_Class() {
        return id_Class;
    }

    public void setId_Class(String id_Class) {
        this.id_Class = id_Class;
    }

    public String getLevel() { return level;    }

    public void setLevel(String level) {  this.level = level;  }

    public String getIdNeo4j() {
        return idNeo4j;
    }

    public void setIdNeo4j(String idNeo4j) {
        this.idNeo4j = idNeo4j;
    }

    public List<Responsable> getResponsableList() {

        return responsableList;
    }

    public void setResponsableList(List<Responsable> responsableList) {
        this.responsableList = responsableList;
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
     * Gets the value of the idBe property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getIdBe() {
        return idBe;
    }

    /**
     * Sets the value of the idBe property.
     *
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *
     */
    public void setIdBe(BigInteger value) {
        this.idBe = value;
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

    /**
     * Gets the value of the codeDivision property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCodeDivision() {
        return codeDivision;
    }

    /**
     * Sets the value of the codeDivision property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCodeDivision(String value) {
        this.codeDivision = value;
    }

    /*public Eleve returnEleveObject(Object o){
        if(equals(o)){
        }
    } */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Eleve eleve = (Eleve) o;

        return id.equals(eleve.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
