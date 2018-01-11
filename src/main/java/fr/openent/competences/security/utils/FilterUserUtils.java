/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

package fr.openent.competences.security.utils;

import org.entcore.common.user.UserInfos;

/**
 * Created by ledunoiss on 20/10/2016.
 */
public class FilterUserUtils{

    private UserInfos user;

    public FilterUserUtils (UserInfos user) {
        this.user = user;
    }

    public boolean validateUser(String idUser) {
        return user.getUserId().equals(idUser);
    }

    public boolean validateStructure(String idEtablissement) {
        return user.getStructures().contains(idEtablissement);
    }

    public boolean validateClasse(String idClasse) {
        return user.getClasses().contains(idClasse);
    }

}
