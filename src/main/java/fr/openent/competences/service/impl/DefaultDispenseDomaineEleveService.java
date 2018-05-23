package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.DispenseDomaineEleveService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultDispenseDomaineEleveService extends SqlCrudService implements DispenseDomaineEleveService {

    protected static final Logger log = LoggerFactory.getLogger(DefaultDispenseDomaineEleveService.class);

    public DefaultDispenseDomaineEleveService (String schema, String table){
        super(schema,table);

    }

    @Override
    public void deleteDispenseDomaineEleve(String idEleve,Integer idDomaine, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM "+ Competences.COMPETENCES_SCHEMA +".dispense_domaine_eleve " +
                "WHERE id_eleve = ? AND id_domaines = ? ;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(idEleve)
                .add(idDomaine);
        sql.prepared(query, params,SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createDispenseDomaineEleve(final JsonObject dispenseDomaineEleve, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO "+ Competences.COMPETENCES_SCHEMA +".dispense_domaine_eleve ( id_eleve, id_domaines, dispense )"+
                "VALUES(?,?,?)";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
       .add(dispenseDomaineEleve.getString("id_eleve"))
       .add(dispenseDomaineEleve.getInteger("id_domaine"))
       .add(dispenseDomaineEleve.getBoolean("dispense"));
        sql.prepared(query,params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listDipenseDomainesByClasse(List<String> idsEleves, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_eleve, id_domaines, dispense FROM "+ Competences.COMPETENCES_SCHEMA +".dispense_domaine_eleve "+
                "WHERE id_eleve IN " + Sql.listPrepared(idsEleves.toArray());

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for(String idEleve : idsEleves){
            params.add(idEleve);
        }
      sql.prepared(query,params,SqlResult.validResultHandler(handler));
    }

    @Override
    public void mapOfDispenseDomaineByIdEleve(List<String> idsEleves, final Handler<Either<String, Map<String, Map<Long, Boolean>>>> handler) {

        listDipenseDomainesByClasse(idsEleves, new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> respQuery) {
                if(respQuery.isRight()){

                    final JsonArray idsEleveIdsDomaineDispenses = respQuery.right().getValue();
                    final Map<String,Map<Long,Boolean>> mapIdEleveIdDomainedispense = new HashMap<>();
                    if(idsEleveIdsDomaineDispenses.size() == 0){
                        mapIdEleveIdDomainedispense.put("empty",new HashMap<Long, Boolean>());
                    }
                    for(int i = 0; i < idsEleveIdsDomaineDispenses.size(); i++){
                        JsonObject dispenseDomaine = idsEleveIdsDomaineDispenses.getJsonObject(i);
                        if(!mapIdEleveIdDomainedispense.containsKey(dispenseDomaine.getString("id_eleve"))){
                            Map<Long,Boolean> dispenseIdDomaine = new HashMap<>();
                            dispenseIdDomaine.put(Long.valueOf(dispenseDomaine.getInteger("id_domaines")),dispenseDomaine.getBoolean("dispense"));
                            mapIdEleveIdDomainedispense.put(dispenseDomaine.getString("id_eleve"),dispenseIdDomaine);
                        }else{
                            mapIdEleveIdDomainedispense.get(dispenseDomaine.getString("id_eleve")).put(Long.valueOf(dispenseDomaine.getInteger("id_domaines")),dispenseDomaine.getBoolean("dispense"));
                        }
                    }
                    handler.handle(new Either.Right<String,Map<String,Map<Long,Boolean>>>(mapIdEleveIdDomainedispense));
                }else {
                    handler.handle(new Either.Left<String, Map<String, Map<Long, Boolean>>>("Erreur lors de la recuperation des dispense de Domaine :\n" + respQuery.left().getValue()));
                    log.error("listDipenseDomainesByClasse : " + respQuery.left().getValue());
                }

            }
        });

    }


}
