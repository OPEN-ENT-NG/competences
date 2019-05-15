//
// Ce fichier a �t� g�n�r� par l'impl�mentation de r�f�rence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apport�e � ce fichier sera perdue lors de la recompilation du sch�ma source. 
// G�n�r� le : 2019.02.14 � 04:50:35 PM CET 
//


package fr.openent.competences.bean.lsun;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour CodeLangueCultureRegionale.
 * 
 * <p>Le fragment de sch�ma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="CodeLangueCultureRegionale">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="AUC"/>
 *     &lt;enumeration value="BAQ"/>
 *     &lt;enumeration value="BRE"/>
 *     &lt;enumeration value="CAT"/>
 *     &lt;enumeration value="COS"/>
 *     &lt;enumeration value="CPF"/>
 *     &lt;enumeration value="FUD"/>
 *     &lt;enumeration value="GAL"/>
 *     &lt;enumeration value="GSW"/>
 *     &lt;enumeration value="MEL"/>
 *     &lt;enumeration value="MOL"/>
 *     &lt;enumeration value="OCI"/>
 *     &lt;enumeration value="TAH"/>
 *     &lt;enumeration value="WLS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CodeLangueCultureRegionale")
@XmlEnum
public enum CodeLangueCultureRegionale {

    AUC,
    BAQ,
    BRE,
    CAT,
    COS,
    CPF,
    FUD,
    GAL,
    GSW,
    MEL,
    MOL,
    OCI,
    TAH,
    WLS;

    public String value() {
        return name();
    }

    public static CodeLangueCultureRegionale fromValue(String v) {
        return valueOf(v);
    }

}
