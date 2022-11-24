export interface ReportModelPrintExportType {
    //getters
    getId(): mongoId;
    getUserId(): String;
    getStructureId(): String;
    getTitle(): String;
    getSelected(): Boolean;
    getPreferencesCheckbox(): PreferencesCheckboxReportModel;
    getPreferencesText(): PreferencesTextReportModel;
    getState(): String;

    //setters
    setUserId(userId: String);
    setStructureId(structureId:String);
    setTitle(title:String);
    setSelected(selected:Boolean);
    setPreferencesCheckbox(preferences:PreferencesCheckboxReportModel | {});
    setPreferencesText(preferences:PreferencesTextReportModel | {});
    setPreferencesCheckboxWithInit(preferences:PreferencesCheckboxReportModel);
    setState(state:String);

    //Methods
    isPost():Boolean;
    isPut():Boolean;
    isDelete():Boolean;
    haveId():Boolean;
    haveTitle():Boolean;
    isEqual(reportModel:ReportModelPrintExportType):Boolean;
    //API
    post: () => Promise<void>;
    put: (listKeys?:Array<string>) => Promise<void>;
    delete: () => Promise<void>;
}

export interface toJson {
    structureId: String;
    title: String;
    preferencesCheckbox: PreferencesCheckboxReportModel;
    preferencesText: PreferencesTextReportModel;
}

export type PreferencesCheckboxReportModel = {string:boolean} | {}
export type PreferencesTextReportModel = {string:String} | {}
export type mongoId = String;