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

package fr.openent.competences.security.utils;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public enum WorkflowActions {
	CREATE_EVALUATION ("competences.create.evaluation"),

	COMPETENCES_ACCESS("competences.access"),

	ADMIN_RIGHT ("Viescolaire.view"),
	CREATE_DISPENSE_DOMAINE_ELEVE ("create.dispense.domaine.eleve"),
	CREATE_ELEMENT_BILAN_PERIODIQUE ("create.element.bilan.periodique"),
	SAVE_COMPETENCE_NIVEAU_FINAL ("save.competence.niveau.final"),
	SAVE_APPMATIERE_POSITIONNEMENT_BILAN_PERIODIQUE("bilan.periodique.save.appMatiere.positionnement"),
	EXPORT_LSU ("competences.lsun.export"),
	CREATE_SYNTHESE_BILAN_PERIODIQUE("create.synthese.bilan.periodique"),
	SAVE_APPRECIATION_BILAN_PERIODIQUE("bilan.periodique.save.appMatiere.positionnement"),
	CAN_UPDATE_BFC_SYNTHESE("competences.canUpdateBFCSynthese"),
	SET_AVIS_CONSEIL("set.avis.conseil.orientation.bilan.periodique"),
	DIGITAL_SKILLS("digital.skills"),
	ACCESS_CONSEIL_DE_CLASSE("access.conseil.de.classe"),
	PARAM_SUBTOPICS ("competences.paramSubtopics"),
	ACCESS_MODEL_BULLETIN("export.bulletins.periodique"),
	CREATE_AVIS_CONSEIL_BILAN_PERIODIQUE("create.avis.conseil.bilan.periodique"),
	COMPETENCES_ACCESS("competences.access");
	private final String actionName;

	WorkflowActions(String actionName) {
		this.actionName = actionName;
	}

	@Override
	public String toString () {
		return this.actionName;
	}
}
