package fr.openent.competences.service.impl;

import fr.openent.competences.Competences;
import fr.openent.competences.service.EleveEnseignementComplementService;
import fr.wseduc.webutils.Either;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

/**
 * Created by agnes.lapeyronnie on 28/11/2017.
 */
public class DefaultEleveEnseignementComplementService extends SqlCrudService implements EleveEnseignementComplementService {

    public DefaultEleveEnseignementComplementService(String schema, String table){
        super(schema,table);

    }

    public void createEnsCplByELeve(JsonObject data, UserInfos user, Handler<Either<String, JsonObject>> handler){
        super.create(data,user,handler);
    }

    @Override
    public void updateEnsCpl(Integer id, JsonObject data, Handler<Either<String, JsonObject>> handler) {
        JsonArray values = new JsonArray();
        String query = "UPDATE "+ Competences.COMPETENCES_SCHEMA+".eleve_enseignement_complement SET id_enscpl = ?, id_niveau = ?, id_langue = ?, niveau_lcr = ?"
             +" WHERE id = ?";
        values.addNumber(data.getNumber("id_enscpl"));
        values.addNumber(data.getNumber("id_niveau"));
        values.addNumber(data.getNumber("id_langue"));
        values.addNumber(data.getNumber("niveau_lcr"));
        values.addNumber(id);
        Sql.getInstance().prepared(query,values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getNiveauEnsCplByEleve(String idEleve, Handler<Either<String, JsonObject>> handler) {
        JsonArray values =  new JsonArray();
        String query ="SELECT eleve_enseignement_complement.*, enseignement_complement.libelle " +
                "FROM " + Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement " +
                "INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".enseignement_complement ON enseignement_complement.id = eleve_enseignement_complement.id_enscpl " +
                "WHERE id_eleve = ?" ;

            values.addString(idEleve);

        Sql.getInstance().prepared(query,values, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void listNiveauCplByEleves( final String[] idsEleve, final  Handler<Either<String, JsonArray>> handler) {
        JsonArray valuesCount = new JsonArray();
        String queryCount = "SELECT count(*) FROM "+ Competences.COMPETENCES_SCHEMA +".eleve_enseignement_complement INNER JOIN "+Competences.COMPETENCES_SCHEMA+".enseignement_complement "
                +"ON notes.eleve_enseignement_complement.id_enscpl = notes.enseignement_complement.id WHERE id_eleve IN "+ Sql.listPrepared(idsEleve);
        for(String idEleve : idsEleve){
            valuesCount.addString(idEleve);
        }

        Sql.getInstance().prepared(queryCount, valuesCount, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> sqlResultCount) {
                Long nbEnsCpl = SqlResult.countResult(sqlResultCount);
                if(nbEnsCpl > 0){
                    JsonArray values = new JsonArray();
                    String query = "SELECT eleve_enseignement_complement.*, enseignement_complement.libelle,enseignement_complement.code, niveau_ens_complement.niveau FROM " + Competences.COMPETENCES_SCHEMA + ".eleve_enseignement_complement" +
                            " INNER JOIN " + Competences.COMPETENCES_SCHEMA + ".enseignement_complement " +
                            "ON notes.eleve_enseignement_complement.id_enscpl = notes.enseignement_complement.id " +
                            "INNER JOIN "+ Competences.COMPETENCES_SCHEMA + ".niveau_ens_complement ON niveau_ens_complement.id = eleve_enseignement_complement.id_niveau "+
                            " WHERE id_eleve IN " + Sql.listPrepared(idsEleve);
                    for (String idEleve : idsEleve) {
                        values.addString(idEleve);
                    }

                    Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
                }else{
                    handler.handle(new Either.Right<String,JsonArray>(new JsonArray()) );
                }
            }
        });
    }
}
