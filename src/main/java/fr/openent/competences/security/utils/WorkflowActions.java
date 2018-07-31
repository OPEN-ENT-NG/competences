package fr.openent.competences.security.utils;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public enum WorkflowActions {
	CREATE_EVALUATION ("competences.create.evaluation"),
	ADMIN_RIGHT ("Viescolaire.view"),
	CREATE_DISPENSE_DOMAINE_ELEVE ("create.dispense.domaine.eleve"),
	CREATE_ELEMENT_BILAN_PERIODIQUE ("create.element.bilan.periodique");

	private final String actionName;

	WorkflowActions(String actionName) {
		this.actionName = actionName;
	}

	@Override
	public String toString () {
		return this.actionName;
	}
}
