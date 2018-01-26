package fr.openent.competences.security.utils;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public enum WorkflowActions {
	CREATE_EVALUATION ("competences.create.evaluation"),
	PARAM_COMPETENCE_RIGHT ("competences.param.competences");

	private final String actionName;

	WorkflowActions(String actionName) {
		this.actionName = actionName;
	}

	@Override
	public String toString () {
		return this.actionName;
	}
}
