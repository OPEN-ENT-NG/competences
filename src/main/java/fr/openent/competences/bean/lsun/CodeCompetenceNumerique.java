package fr.openent.competences.bean.lsun;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Classe Java pour CodeCompetenceNumerique.
 *
 * <p>Le fragment de schéma suivant indique le contenu attendu figurant dans cette classe.
 * <p>
 * <pre>
 * &lt;simpleType name="CodeCompetenceNumerique">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CN_INF_MEN"/>
 *     &lt;enumeration value="CN_INF_GER"/>
 *     &lt;enumeration value="CN_INF_TRA"/>
 *     &lt;enumeration value="CN_COM_INT"/>
 *     &lt;enumeration value="CN_COM_PAR"/>
 *     &lt;enumeration value="CN_COM_COL"/>
 *     &lt;enumeration value="CN_COM_SIN"/>
 *     &lt;enumeration value="CN_CRE_TEX"/>
 *     &lt;enumeration value="CN_CRE_MUL"/>
 *     &lt;enumeration value="CN_CRE_ADA"/>
 *     &lt;enumeration value="CN_CRE_PRO"/>
 *     &lt;enumeration value="CN_PRO_SEC"/>
 *     &lt;enumeration value="CN_PRO_DON"/>
 *     &lt;enumeration value="CN_PRO_SAN"/>
 *     &lt;enumeration value="CN_ENV_RES"/>
 *     &lt;enumeration value="CN_ENV_EVO"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "CodeCompetenceNumerique")
@XmlEnum
public enum CodeCompetenceNumerique {

    CN_INF_MEN,
    CN_INF_GER,
    CN_INF_TRA,
    CN_COM_INT,
    CN_COM_PAR,
    CN_COM_COL,
    CN_COM_SIN,
    CN_CRE_TEX,
    CN_CRE_MUL,
    CN_CRE_ADA,
    CN_CRE_PRO,
    CN_PRO_SEC,
    CN_PRO_DON,
    CN_PRO_SAN,
    CN_ENV_RES,
    CN_ENV_EVO;

    public String value() {
        return name();
    }

    public static CodeCompetenceNumerique fromValue(String v) {
        return valueOf(v);
    }
}
