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

package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface DispenseDomaineEleveService extends CrudService{

    /**
     * Supprime une dispense de domaine pour un élève
     * @param idEleve idEleve
     * @param idDomaine idDomaine
     * @param handler handler portant le résultat de la requête
     */
    public void deleteDispenseDomaineEleve(String idEleve, Integer idDomaine, Handler<Either<String, JsonObject>> handler);

    /**
     * insertion d'une dispense pour un domaine et pour un élève
     * @param dispenseDomaineEleve
     * @param handler handler portant le résultat de la requête
     */
    public void createDispenseDomaineEleve(JsonObject dispenseDomaineEleve,Handler<Either<String, JsonObject>> handler);

    /**
     * tous les domaines dispenses pour tous les élèves d'une classe
     * @param idsEleves list des identifiants des élèves pour une classe
     * @param handler handler portant le résultat de la requête
     */
    public void listDipenseDomainesByClasse(List<String> idsEleves, Handler<Either<String,JsonArray>> handler);

    /**
     * convert the query of dispenseDomaineByEleve to map<idEleve,map<idDomaine,dispense>
     * @param idsEleves list des élèves
     * @param handler contain the map <idEleve,Map<idDomaine,dispense>>
     */
    public void mapOfDispenseDomaineByIdEleve(List<String> idsEleves, Handler<Either<String,Map<String,Map<Long,Boolean>>>> handler);

}
