package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.MatiereService;
import fr.wseduc.webutils.Either;
import io.vertx.core.json.JsonArray;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import io.vertx.core.Handler;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.HashMap;
import java.util.Map;



public class DefaultMatiereService extends SqlCrudService implements MatiereService {

    private static final Logger log = LoggerFactory.getLogger(DefaultMatiereService.class);
    public DefaultMatiereService() {
        super(Competences.COMPETENCES_SCHEMA, Competences.MATIERE_TABLE);
    }



    @Override
    public void getLibellesCourtsMatieres(Handler<Either<String, Map<String,String>>> handler) {

        String query = "SELECT code, libelle_court FROM "+ this.resourceTable;
        Map<String,String> mapCodeLibelleCourt = new HashMap<>();

        Sql.getInstance().prepared(query ,new JsonArray(), SqlResult.validResultHandler(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> event) {
                
                if(event.isRight()){
                    JsonArray codesLibellesCourts = event.right().getValue();

                        for(int i = 0; i < codesLibellesCourts.size(); i++ ){
                            if(!mapCodeLibelleCourt.containsKey(codesLibellesCourts.getJsonObject(i).getString("code"))) {
                                mapCodeLibelleCourt.put(codesLibellesCourts.getJsonObject(i).getString("code"),
                                      codesLibellesCourts.getJsonObject(i).getString("libelle_court"));
                            }
                        }
                      handler.handle(new Either.Right<>(mapCodeLibelleCourt));
                }else{
                    handler.handle(new Either.Right<>(mapCodeLibelleCourt));
                    log.error("getLibellesCourtsMatieres : " + event.left().getValue());
                }

            }
        }));

    }
}
