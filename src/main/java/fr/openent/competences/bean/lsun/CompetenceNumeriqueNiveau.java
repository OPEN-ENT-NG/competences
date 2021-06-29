package fr.openent.competences.bean.lsun;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Décrit le positionnement de l'élève sur une compétence numérique
 *
 *
 * <p>Classe Java pour CompetenceNumeriqueNiveau complex type.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 *
 * <pre>
 * &lt;complexType name="CompetenceNumeriqueNiveau">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="code" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}CodeCompetenceNumerique" />
 *       &lt;attribute name="niveau" use="required" type="{urn:fr:edu:scolarite:lsun:bilans:import}NiveauCompetenceNumerique" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CompetenceNumeriqueNiveau")
public class CompetenceNumeriqueNiveau {
    @XmlAttribute(name = "code", required = true)
    protected CodeCompetenceNumerique code;
    @XmlAttribute(name = "niveau", required = true)
    protected BigInteger niveau;

    /**
     * Obtient la valeur de la propriété code.
     *
     * @return
     *     possible object is
     *     {@link CodeCompetenceNumerique }
     *
     */
    public CodeCompetenceNumerique getCode() {
        return code;
    }

    /**
     * Définit la valeur de la propriété code.
     *
     * @param value
     *     allowed object is
     *     {@link CodeCompetenceNumerique }
     *
     */
    public void setCode(CodeCompetenceNumerique value) {
        this.code = value;
    }

    /**
     * Obtient la valeur de la propriété niveau.
     *
     * @return
     *     possible object is
     *     {@link BigInteger }
     *
     */
    public BigInteger getNiveau() {
        return niveau;
    }

    /**
     * Définit la valeur de la propriété niveau.
     *
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *
     */
    public void setNiveau(BigInteger value) {
        this.niveau = value;
    }

}
