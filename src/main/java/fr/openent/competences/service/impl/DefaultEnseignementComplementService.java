package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.EnseignementComplementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public class DefaultEnseignementComplementService extends SqlCrudService implements EnseignementComplementService {


    public DefaultEnseignementComplementService(String schema, String table) {
        super(schema, table);
    }


    public void getEnseignementsComplement(Handler<Either<String, JsonArray>> handler) {
       String query = "SELECT id, libelle, code FROM "+ Competences.COMPETENCES_SCHEMA + ".enseignement_complement";

        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));

    }



}
