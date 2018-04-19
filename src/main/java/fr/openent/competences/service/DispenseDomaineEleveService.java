package fr.openent.competences.service;

import fr.wseduc.webutils.Either;
import org.entcore.common.service.CrudService;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

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
