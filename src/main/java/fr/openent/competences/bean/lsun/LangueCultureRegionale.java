//
// Ce fichier a �t� g�n�r� par l'impl�mentation de r�f�rence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport�e � ce fichier sera perdue lors de la recompilation du sch�ma source. 
// G�n�r� le : 2019.02.14 � 04:50:35 PM CET 
//


package fr.openent.competences.bean.lsun;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Indique le niveau A2 atteint pour une langue et culture r�gionale
 *             
 * 
 * <p>Classe Java pour LangueCultureRegionale complex type.
 * 
 * <p>Le fragment de sch�ma suivant indique le contenu attendu figurant dans cette classe.
 * 
 * <pre>
 * &lt;complexType name="LangueCultureRegionale">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="code" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeLangueCultureRegionale" />
 *       &lt;attribute name="positionnement" type="{urn:fr:edu:scolarite:lsun:bilans:import}PositionnementLangueCultureRegionale" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "LangueCultureRegionale")
public class LangueCultureRegionale {

    @XmlAttribute(name = "code", required = true)
    protected CodeLangueCultureRegionale code;
    @XmlAttribute(name = "positionnement")
    protected BigInteger positionnement;

    /**
     * Obtient la valeur de la propri�t� code.
     * 
     * @return
     *     possible object is
     *     {@link CodeLangueCultureRegionale }
     *     
     */
    public CodeLangueCultureRegionale getCode() {
        return code;
    }

    /**
     * D�finit la valeur de la propri�t� code.
     * 
     * @param value
     *     allowed object is
     *     {@link CodeLangueCultureRegionale }
     *     
     */
    public void setCode(CodeLangueCultureRegionale value) {
        this.code = value;
    }

    /**
     * Obtient la valeur de la propri�t� positionnement.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getPositionnement() {
        return positionnement;
    }

    /**
     * D�finit la valeur de la propri�t� positionnement.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setPositionnement(BigInteger value) {
        this.positionnement = value;
    }

}
