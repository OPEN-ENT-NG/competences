//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.07.06 at 05:11:16 PM CEST 
//


package fr.openent.competences.bean.lsun;

import javax.xml.bind.annotation.*;


/**
 * Contient les parcours �ducatifs communs � une classe
 *             
 * 
 * <p>Java class for VieScolaireCommun complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="VieScolaireCommun">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="commentaire" type="{urn:fr:edu:scolarite:lsun:bilans:import}Chaine600"/>
 *       &lt;/sequence>
 *       &lt;attribute name="periode-ref" use="required" type="{http://www.w3.org/2001/XMLSchema}IDREF" />
 *       &lt;attribute name="code-division" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeStructure" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "VieScolaireCommun", propOrder = {
    "commentaire"
})
public class VieScolaireCommun {

    @XmlElement(required = true)
    protected String commentaire;
    @XmlAttribute(name = "periode-ref", required = true)
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected Object periodeRef;
    @XmlAttribute(name = "code-division", required = true)
    protected String codeDivision;

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
        this.commentaire = value;
    }

    /**
     * Gets the value of the periodeRef property.
     * 
     * @return
     *     possible object is
     *     {@link Object }
     *     
     */
    public Object getPeriodeRef() {
        return periodeRef;
    }

    /**
     * Sets the value of the periodeRef property.
     * 
     * @param value
     *     allowed object is
     *     {@link Object }
     *     
     */
    public void setPeriodeRef(Object value) {
        this.periodeRef = value;
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

}
