export interface ReportModelPrintExportType {
    //getters
    getId(): mongoId;
    getTitle(): String;
    getSelected(): Boolean;
    getPreferences(): PreferencesReportModel;
    getState(): String;
    //setters
    setTitle(title:String);
    setSelected(selected:Boolean);
    setPreferences(preferences:PreferencesReportModel | {});
    setPreferencesWithInit(preferences:PreferencesReportModel);
    setState(state:String);
    //Methods
    toJSON():toJson;
    isPost():Boolean;
    isPut():Boolean;
    isDelete():Boolean;
    haveId():Boolean;
    haveTitle():Boolean;
    isEqual(reportModel:ReportModelPrintExportType):Boolean;
    //API
    post: () => Promise<any>;
    put: () => Promise<any>;
    delete: () => Promise<any>;
}

export interface toJson {
    title: String;
    selected: Boolean;
    preferences: PreferencesReportModel;
}

export type PreferencesReportModel = {string:boolean} | {}
export type mongoId = String;