package fr.openent.competences;

import org.entcore.common.http.BaseServer;
import org.entcore.common.http.filter.ShareAndOwner;
import org.entcore.common.mongodb.MongoDbConf;

import fr.openent.competences.controllers.CompetencesController;

public class Competences extends BaseServer {

	@Override
	public void start() {
		super.start();
		addController(new CompetencesController());
	}

}
