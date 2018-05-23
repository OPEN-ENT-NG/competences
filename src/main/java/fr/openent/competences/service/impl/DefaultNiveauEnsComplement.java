package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.NiveauEnsComplementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public class DefaultNiveauEnsComplement extends SqlCrudService implements NiveauEnsComplementService{

   public DefaultNiveauEnsComplement(String schema,String table){
       super(schema,table);
   }
    @Override
    public void getNiveauEnsComplement(Handler<Either<String, JsonArray>> handler) {

       String query = "SELECT id, libelle, niveau, bareme_brevet FROM "+ Competences.COMPETENCES_SCHEMA +".niveau_ens_complement";

        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }
}
