import {_, ng} from "entcore";
import {ReportModelPrintExportServiceType} from "../services/type";
import * as utilsTeacher from '../utils/teacher';
import { ReportModelPrintExportType} from "../models/type";
import {ReportModelPrintExport} from "../models/teacher/ReportModelPrintExport";
import {
    ReportModelPrintExportConstant,
} from "../constants"

const {
    DELETED,
} = ReportModelPrintExportConstant;

const reportModelPrintExportController = ng.controller(
    'reportModelPrintExportController', [
        '$scope',
        'ReportModelPrintExportService',
        async function($scope,
                       ReportModelPrintExportService:ReportModelPrintExportServiceType) {

            const initDataReportModel = await Promise.all([
                ReportModelPrintExportService.getAll(),
                ReportModelPrintExportService.getAll(),
            ]);
            $scope.allReportModelPrintExport = initDataReportModel[0];
            const reportsModelForCheckSubmit = initDataReportModel[1];
            utilsTeacher.safeApply($scope);

            $scope.enableSubmit = $scope.infoProblemInTitle = false;
            $scope.newReportModel = new ReportModelPrintExport(undefined);
            $scope.newReportModel.setSelected(false);

            if ($scope.$parent.preferencesPrint) {
                $scope.newReportModel.setPreferencesWithClean($scope.$parent.preferencesPrint);
            }

            //functions share
            $scope.selectReportModel = function (reportModel: ReportModelPrintExportType): void {
                if (!reportModel.getId()) return;
                deSelectedAll();
                if ($scope.newReportModel.getSelected()) $scope.newReportModel.setSelected(false);
                reportModel.setSelected(true);
                problemInTitle(reportModel);
            };

            $scope.makeNewReportModel = (): void => {
                deSelectedAll();
                $scope.newReportModel.setSelected(true);
            };

            $scope.openUpdateMode = function (reportModel: ReportModelPrintExportType): void {
                reportModel["iAmUpdated"] = true;
                reportModel["backupLastTitle"] = reportModel.getTitle();
            };

            $scope.saveTitleEdit = function (reportModel: ReportModelPrintExportType): void {
                if (reportModel["iAmUpdated"]) delete reportModel["iAmUpdated"];
                problemInTitle(reportModel);
            };
            const reportsModelsPrepareToDelete:Array<ReportModelPrintExportType> = [];
            $scope.remove = async function (reportModel: ReportModelPrintExportType): Promise<void> {
                reportModel.setState(DELETED);
                reportsModelsPrepareToDelete.push(reportModel);
                $scope.allReportModelPrintExport = allReportModelPrintExportChecked()
                    .filter((reportModelSearch: ReportModelPrintExportType): Boolean => {
                        return !reportModelSearch.isDelete();
                    });
                $scope.enableSubmit = true;
                utilsTeacher.safeApply($scope);
            };

            $scope.submit = (): void => {
                if(!$scope.enableSubmit) {
                    return;
                }
                $scope.enableSubmit = false;
                putReportsModels();
                deleteReportsModels();
                if ($scope.newReportModel.getSelected()) {
                    sendNew();
                    $scope.$parent.closeLightBoxSelectModelReport($scope.newReportModel);
                    return;
                }
                if(!($scope.allReportModelPrintExport.length > 0)){
                    $scope.$parent.closeLightBoxSelectModelReport();
                } else {
                    const reportModelSelected:ReportModelPrintExportType = $scope.allReportModelPrintExport
                        .find((findReportModel: ReportModelPrintExportType) => findReportModel.getSelected());
                    $scope.$parent.closeLightBoxSelectModelReport(reportModelSelected);
                }
            };

            function deleteReportsModels():void{
                reportsModelsPrepareToDelete.forEach((forReportModel: ReportModelPrintExportType): void => {
                    if(forReportModel.isDelete())forReportModel.delete();
                });
            }

            function deSelectedAll(): void {
                allReportModelPrintExportChecked().forEach((forReportModel: ReportModelPrintExportType): void => {
                    forReportModel.setSelected(false);
                });
            }

            function problemInTitle(reportModel: ReportModelPrintExportType): void {
                if (!checkSameTitle() && reportModel.getTitle()) {
                    if (reportModel["backupLastTitle"]) delete reportModel["backupLastTitle"];
                    if (reportModel["errorSameTitle"]) delete reportModel["errorSameTitle"];
                    $scope.enableSubmit = true;
                    $scope.infoProblemInTitle = false;
                } else {
                    $scope.enableSubmit = false;
                    $scope.infoProblemInTitle = true;
                    reportModel["errorSameTitle"] = true;
                    if(reportModel.getTitle()) reportModel.setTitle(reportModel["backupLastTitle"])
                }
            }

            function cleanTitleEmptyBeforePut(): void {
                $scope.allReportModelPrintExport = allReportModelPrintExportChecked()
                    .filter((filterReportModel: ReportModelPrintExportType): Boolean => filterReportModel.haveTitle())
            }

            function cleanReportModelNoEdit() {
                $scope.allReportModelPrintExport = allReportModelPrintExportChecked()
                    .filter((filterReportModel: ReportModelPrintExportType): Boolean => {
                        for (const reportModel of reportsModelForCheckSubmit) {
                            if (reportModel.isEqual(filterReportModel)) return false;
                        }
                        return true;
                    });
            }

            function checkSameTitle(): Boolean {
                const titlesAllReportsModels = allReportModelPrintExportChecked()
                    .map((mapReportModel: ReportModelPrintExportType): String => mapReportModel.getTitle());
                return (new Set(titlesAllReportsModels)).size !== titlesAllReportsModels.length;
            }

            async function sendNew() {
                await $scope.newReportModel.post();
            }

            function allReportModelPrintExportChecked():Array<ReportModelPrintExportType>{
                if(!($scope.allReportModelPrintExport.length > 0)) return [];
                return $scope.allReportModelPrintExport;
            }

            function putReportsModels():void {
                if (!_.isEqual(allReportModelPrintExportChecked(), reportsModelForCheckSubmit)) {
                    cleanReportModelNoEdit();
                    cleanTitleEmptyBeforePut();
                    for (const reportModel of allReportModelPrintExportChecked()) {
                        reportModel.put();
                    }
                }

            }
        }
    ]);

export default reportModelPrintExportController;