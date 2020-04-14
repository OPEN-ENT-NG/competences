export interface ReportModelPrintExportType {
    //getters
    getId(): mongoId;
    getTitle(): String;
    getSelected(): Boolean;
    getPreferencesCheckbox(): PreferencesCheckboxReportModel;
    getPreferencesText(): PreferencesTextReportModel;
    getState(): String;

    //setters
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
    title: String;
    selected: Boolean;
    preferencesCheckbox: PreferencesCheckboxReportModel;
    preferencesText: PreferencesTextReportModel;
}

export type PreferencesCheckboxReportModel = {string:boolean} | {}
export type PreferencesTextReportModel = {string:String} | {}
export type mongoId = String;