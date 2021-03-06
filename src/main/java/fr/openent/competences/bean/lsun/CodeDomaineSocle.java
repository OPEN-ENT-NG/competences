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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for CodeDomaineSocle.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="CodeDomaineSocle">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="CPD_FRA"/>
 *     &lt;enumeration value="CPD_ETR"/>
 *     &lt;enumeration value="CPD_SCI"/>
 *     &lt;enumeration value="CPD_ART"/>
 *     &lt;enumeration value="MET_APP"/>
 *     &lt;enumeration value="FRM_CIT"/>
 *     &lt;enumeration value="SYS_NAT"/>
 *     &lt;enumeration value="REP_MND"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "CodeDomaineSocle")
@XmlEnum
public enum CodeDomaineSocle {

    CPD_FRA,
    CPD_ETR,
    CPD_SCI,
    CPD_ART,
    MET_APP,
    FRM_CIT,
    SYS_NAT,
    REP_MND;

    public String value() {
        return name();
    }

    public static CodeDomaineSocle fromValue(String v) {
        return valueOf(v);
    }



}
