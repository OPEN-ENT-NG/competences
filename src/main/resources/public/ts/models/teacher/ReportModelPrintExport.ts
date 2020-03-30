import {
    ReportModelPrintExportType,
    toJson,
    PreferencesReportModel,
    mongoId,
} from "../type";
import {
    ReportModelPrintExportConstant,
    ReportsModelsPrintExportTypeConst,
} from "../../constants"
import {_, notify} from "entcore";
import http from "axios";
import {MongoDBUtils} from "../../services/utils";

const {
    DELETED,
    POSTED,
    PUTED,
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

export class ReportModelPrintExport implements ReportModelPrintExportType{
    private _id: mongoId;
    private title: String;
    private selected: Boolean;
    private preferences: PreferencesReportModel;
    private state: String;

    constructor(reportModel:ReportModelPrintExport | undefined){
        if(reportModel) this.preparedReportModel(reportModel);
    }

    //Getters
    public getId():mongoId {return this._id || undefined};
    public getTitle():String {return this.title || undefined}
    public getSelected():Boolean {return this.selected || false};
    public getPreferences():PreferencesReportModel {return this.preferences || {}};
    public getState():String {return this.state || undefined};

    //Setters
    private setId(id:mongoId) {this._id = id};
    public setTitle(title:String) {this.title = title};
    public setSelected(selected:Boolean) {this.selected = selected};
    public setPreferences(preferences:PreferencesReportModel) {this.preferences = preferences};
    public setPreferencesWithInit(preferences:PreferencesReportModel) {this.initPreferences(preferences as Array<String>)};
    public setPreferencesWithClean(preferences:PreferencesReportModel) {this.cleanPreferences(preferences)};
    public setState(state:String) {this.state = state};

    //Methods
    private preparedReportModel(reportModel:ReportModelPrintExport):void{
        this.setId(reportModel._id);
        this.setTitle(reportModel.title);
        this.setSelected(reportModel.selected);
        this.setPreferencesWithInit(reportModel.preferences);
    }

    private initPreferences(preferences:Array<String>){
        const preferencesClean:PreferencesReportModel = {};
        for (let keyPreferenceConst in ReportsModelsPrintExportTypeConst) {
            const preferenceConst = ReportsModelsPrintExportTypeConst[keyPreferenceConst];
            if(_.contains(preferences, preferenceConst)){
                preferencesClean[preferenceConst] = true;
            } else {
                preferencesClean[preferenceConst] = false;
            }
        }
        this.preferences =  preferencesClean;
    }

    private cleanPreferences(preferences:PreferencesReportModel){
        const preferencesClean:PreferencesReportModel = {};
        for (let keyPreferenceConst in ReportsModelsPrintExportTypeConst) {
            const preferenceConst = ReportsModelsPrintExportTypeConst[keyPreferenceConst];
            preferencesClean[preferenceConst] = preferences[preferenceConst];
        }
        this.preferences =  preferencesClean;
    }

    public toJSON():toJson{
        return {
            title: this.getTitle(),
            selected: this.getSelected(),
            preferences: this.getPreferences(),
        };
    }

    public isPost():Boolean{
        return this.getState() === POSTED;
    }

    public isPut():Boolean{
        return this.getState() === PUTED;
    }

    public isDelete():Boolean{
        return this.getState() === DELETED;
    }

    public haveId():Boolean{
        return this.getId() !== undefined;
    }

    public haveTitle():Boolean{
        return this.getTitle() !== undefined;
    }

    public isEqual(reportModel:ReportModelPrintExportType):Boolean{
        return this.constructor.name === reportModel.constructor.name
            && this.getId() === reportModel.getId()
            && this.getTitle() === reportModel.getTitle()
            && this.getSelected() === reportModel.getSelected()
            && _.isEqual(this.getPreferences(), reportModel.getPreferences())
    }

    //API
    public async post():Promise<void> {
        try {
            const response = await http.post(URL_API_POST, this.toJSON());
            this.setId(controlDataAndGetId(response));
            if(this.haveId()) this.setState(POSTED);
        } catch (error) {
            notify.error('competences.report-model.api.error.create');
        }
    }

    public async put():Promise<void> {
        try {
            if(this.haveId()){
                const response = await http.put(`${URL_API_PUT}${this.getId()}`, this.toJSON());
                if(isDataChangedWithoutResult(response)) this.setState(PUTED);
            }
        } catch (error) {
            notify.error('competences.report-model.api.error.update');
        }
    }

    public async delete():Promise<void> {
        try {
            if(this.haveId()){
                const response = await http.delete(`${URL_API_DELETE}${this.getId()}`);
                if(isDataChangedWithoutResult(response)) {
                    this.setState(DELETED);
                    this.setId(undefined);
                }
            }
        } catch (error) {
            notify.error('competences.report-model.api.error.remove');
        }
    }
}
