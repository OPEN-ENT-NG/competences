//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.07.06 at 05:11:16 PM CEST 
//


package fr.openent.competences.bean.lsun;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CodeEnsCompl.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CodeEnsCompl">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="AUC"/>
 *     &lt;enumeration value="LCA"/>
 *     &lt;enumeration value="LCR"/>
 *     &lt;enumeration value="PRO"/>
 *     &lt;enumeration value="LSF"/>
 *     &lt;enumeration value="LVE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CodeEnsCompl")
@XmlEnum
public enum CodeEnsCompl {

    AUC,
    LCA,
    LCR,
    PRO,
    LSF,
    LVE;

    public String value() {
        return name();
    }

    public static CodeEnsCompl fromValue(String v) {
        return valueOf(v);
    }

}
