/*
For add new preference checkbox or preference text,
just you must add key in reportModelPrintExportConstants frontend and backend
 */

import {
    mongoId,
    PreferencesCheckboxReportModel,
    PreferencesTextReportModel,
    ReportModelPrintExportType,
    toJson,
} from "../type";
import {
    ReportModelPrintExportConstant,
    ReportsModelsPrintExportPreferencesCheckboxConst,
    ReportsModelsPrintExportPreferencesTextConst,
} from "../../constants"
import {_, notify} from "entcore";
import http from "axios";
import {MongoDBUtils} from "../../services/utils";

const {
    DELETED,
    POSTED,
    PUTED,
    KEY_ID,
    KEY_STRUCTUREID,
    KEY_TITLE,
    KEY_PREFERENCES_CHECKBOX,
    KEY_PREFERENCES_TEXT,
} = ReportModelPrintExportConstant;

const {
    isDataChangedWithoutResult,
    controlDataAndGetId,
} = MongoDBUtils;

const {
    URL_API_POST,
    URL_API_PUT,
    URL_API_DELETE,
} = ReportModelPrintExportConstant;

export class ReportModelPrintExport implements ReportModelPrintExportType {
    private _id: mongoId;
    private structureId: String;
    private title: String;
    private selected: Boolean;
    private preferencesCheckbox: PreferencesCheckboxReportModel;
    private preferencesText: PreferencesTextReportModel;
    private state: String;

    constructor(reportModel: ReportModelPrintExport | undefined) {
        if (reportModel) this.preparedReportModel(reportModel);
    }

    //Getters
    public getId(): mongoId {
        return this._id || undefined
    };

    public getStructureId(): String {
        return this.structureId || undefined
    };

    public getTitle(): String {
        return this.title || undefined
    }

    public getSelected(): Boolean {
        return this.selected || false
    };

    public getPreferencesCheckbox(): PreferencesCheckboxReportModel {
        return this.preferencesCheckbox || {}
    };

    public getPreferencesText(): PreferencesTextReportModel {
        return this.preferencesText || {}
    };

    public getState(): String {
        return this.state || undefined
    };

    //Setters
    public setStructureId (structureId: String){
        this.structureId = structureId
    };

    public setTitle(title: String) {
        this.title = title
    };

    public setSelected(selected: Boolean) {
        this.selected = selected
    };

    public setPreferencesCheckbox(preferencesCheckbox: PreferencesCheckboxReportModel) {
        this.preferencesCheckbox = preferencesCheckbox
    };

    public setPreferencesText(preferencesText: PreferencesTextReportModel) {
        this.preferencesText = preferencesText
    };

    public setPreferencesCheckboxWithInit(preferences: PreferencesCheckboxReportModel) {
        this.initPreferencesCheckbox(preferences as Array<String>)
    };

    public setPreferencesCheckboxWithClean(preferences: PreferencesCheckboxReportModel) {
        this.cleanPreferences(KEY_PREFERENCES_CHECKBOX, preferences)
    };

    public setPreferencesTextWithClean(preferences: PreferencesTextReportModel) {
        this.cleanPreferences(KEY_PREFERENCES_TEXT, preferences)
    };

    public setState(state: String) {
        this.state = state
    };

    private setId(id: mongoId) {
        this._id = id
    };

    //API
    public async post(): Promise<void> {
        try {
            const response = await http.post(URL_API_POST, this.toJSON());
            this.setId(controlDataAndGetId(response));
            if (this.haveId()) this.setState(POSTED);
        } catch (error) {
            notify.error('competences.report-model.api.error.create');
        }
    }

    public async put( listKeys?: Array<string> ): Promise<void> {
        try {
            if (this.haveId()) {
                const response = await http.put(
                    `${URL_API_PUT}${this.getId()}`,
                    listKeys? this.filtredToJson(listKeys) : this.toJSON()
                );
                if (isDataChangedWithoutResult(response)) this.setState(PUTED);
            }
        } catch (error) {
            notify.error('competences.report-model.api.error.update');
        }
    }

    public async delete(): Promise<void> {
        try {
            if (this.haveId()) {
                const response = await http.delete(`${URL_API_DELETE}${this.getId()}`);
                if (isDataChangedWithoutResult(response)) {
                    this.setState(DELETED);
                    this.setId(undefined);
                }
            }
        } catch (error) {
            notify.error('competences.report-model.api.error.remove');
        }
    }

    //Methods
    public isPost(): Boolean {
        return this.getState() === POSTED;
    }

    public isPut(): Boolean {
        return this.getState() === PUTED;
    }

    public isDelete(): Boolean {
        return this.getState() === DELETED;
    }

    public haveId(): Boolean {
        return this.getId() !== undefined;
    }

    public haveTitle(): Boolean {
        return this.getTitle() !== undefined;
    }

    public isEqual(reportModel: ReportModelPrintExportType): Boolean {
        return this.constructor.name === reportModel.constructor.name
            && this.getId() === reportModel.getId()
            && this.getStructureId() === reportModel.getStructureId()
            && this.getTitle() === reportModel.getTitle()
            && _.isEqual(this.getPreferencesCheckbox(), reportModel.getPreferencesCheckbox())
            && _.isEqual(this.getPreferencesText(), reportModel.getPreferencesText())
    }

    private toJSON(): toJson {
        return {
            structureId: this.getStructureId(),
            title: this.getTitle(),
            preferencesCheckbox: this.getPreferencesCheckbox(),
            preferencesText: this.getPreferencesText(),
        };
    }

    private filtredToJson(listKeys: Array<string>): {} {
        let jsonSend: {} = {};
        const json = this.toJSON();
        for (const key of listKeys) {
            jsonSend[key] = json[key];
        }
        jsonSend[KEY_ID] = json[KEY_ID];
        return jsonSend;
    }

    private preparedReportModel(reportModel: ReportModelPrintExport): void {
        this.setId(reportModel[KEY_ID]);
        this.setStructureId(reportModel[KEY_STRUCTUREID]);
        this.setTitle(reportModel[KEY_TITLE]);
        this.setPreferencesCheckboxWithInit(reportModel[KEY_PREFERENCES_CHECKBOX]);
        this.setPreferencesTextWithClean(reportModel[KEY_PREFERENCES_TEXT]);
    }

    private initPreferencesCheckbox(preferences: Array<String>) {
        const preferencesClean: PreferencesCheckboxReportModel = {};
        for (let keyPreferenceConst in ReportsModelsPrintExportPreferencesCheckboxConst) {
            const preferenceConst = ReportsModelsPrintExportPreferencesCheckboxConst[keyPreferenceConst];
            if (_.contains(preferences, preferenceConst)) {
                preferencesClean[preferenceConst] = true;
            } else {
                preferencesClean[preferenceConst] = false;
            }
        }
        this.setPreferencesCheckbox(preferencesClean);
    }

    private cleanPreferences(
        preferenceKey: String,
        preferencesDirty: PreferencesTextReportModel | PreferencesTextReportModel
    ): void {
        let preferencesConstants;
        if (preferenceKey === KEY_PREFERENCES_CHECKBOX) {
            preferencesConstants = ReportsModelsPrintExportPreferencesCheckboxConst;
            this.setPreferencesCheckbox(this.makePreferenceData(preferencesDirty, preferencesConstants));
        } else if (preferenceKey === KEY_PREFERENCES_TEXT) {
            preferencesConstants = ReportsModelsPrintExportPreferencesTextConst;
            this.setPreferencesText(this.makePreferenceData(preferencesDirty, preferencesConstants));
        } else {
            return;
        }
    }

    private makePreferenceData<T>(
        preferencesDirty: T,
        preferencesConstants: { string: string } | any
    ):{} {
        const preferencesClean: T | {} = {};
        if(!preferencesDirty) return preferencesClean;
        for (let keyPreferenceConst in preferencesConstants) {
            const preferenceConst = preferencesConstants[keyPreferenceConst];
            preferencesClean[preferenceConst] = preferencesDirty[preferenceConst];
        }
        return preferencesClean;
    }
}
