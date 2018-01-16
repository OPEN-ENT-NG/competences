package fr.openent.competences.utils;

/**
 * Created by ledunoiss on 01/06/2017.
 */
public enum CompetencesWorkflowActions {
	CREATE_EVALUATION ("competences.create.evaluation");

	private final String actionName;

	CompetencesWorkflowActions(String actionName) {
		this.actionName = actionName;
	}

	@Override
	public String toString () {
		return this.actionName;
	}
}
