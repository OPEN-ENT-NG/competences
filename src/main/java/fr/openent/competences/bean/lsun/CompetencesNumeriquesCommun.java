package fr.openent.competences.bean.lsun;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * Contient une appréciation commune pour les compétences numériques
 *             d'une classe, ou d'un groupe. Cette structure pédagogique est décrite par les attributs
 *             code-structure et type-structure.
 *
 *
 * <p>Classe Java pour CompetencesNumeriquesCommun complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="CompetencesNumeriquesCommun">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;urn:fr:edu:scolarite:lsun:bilans:import>ChaineEmptyWithMax600">
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}ID" />
 *       &lt;attribute name="code-structure" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeStructure" />
 *       &lt;attribute name="type-structure" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}TypeStructure" />
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompetencesNumeriquesCommun", propOrder = {
        "value"
})
public class CompetencesNumeriquesCommun {
    @XmlValue
    protected String value;
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAttribute(name = "code-structure", required = true)
    protected String codeStructure;
    @XmlAttribute(name = "type-structure", required = true)
    protected TypeStructure typeStructure;

    /**
     * Type utilisé pour les chaînes de caractères qui peut être vide et de taille max 600
     *
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getValue() {
        return value;
    }

    /**
     * Définit la valeur de la propriété value.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Obtient la valeur de la propriété id.
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
     * Définit la valeur de la propriété id.
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
     * Obtient la valeur de la propriété codeStructure.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getCodeStructure() {
        return codeStructure;
    }

    /**
     * Définit la valeur de la propriété codeStructure.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setCodeStructure(String value) {
        this.codeStructure = value;
    }

    /**
     * Obtient la valeur de la propriété typeStructure.
     *
     * @return
     *     possible object is
     *     {@link TypeStructure }
     *
     */
    public TypeStructure getTypeStructure() {
        return typeStructure;
    }

    /**
     * Définit la valeur de la propriété typeStructure.
     *
     * @param value
     *     allowed object is
     *     {@link TypeStructure }
     *
     */
    public void setTypeStructure(TypeStructure value) {
        this.typeStructure = value;
    }
}
