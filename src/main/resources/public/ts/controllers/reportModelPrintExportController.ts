import {_, ng} from "entcore";
import {ReportModelPrintExportServiceType} from "../services/type";
import * as utilsTeacher from '../utils/teacher';
import {ReportModelPrintExportType} from "../models/type";
import {ReportModelPrintExport} from "../models/teacher/ReportModelPrintExport";
import {ReportModelPrintExportConstant,} from "../constants"

const {
    DELETED,
    KEY_STRUCTUREID,
    KEY_TITLE,
    KEY_SELECTED,
} = ReportModelPrintExportConstant;

const reportModelPrintExportController = ng.controller(
    'reportModelPrintExportController', [
        '$scope',
        'ReportModelPrintExportService',
        async function ($scope,
                        ReportModelPrintExportService: ReportModelPrintExportServiceType) {

            const initDataReportModel = await Promise.all([
                ReportModelPrintExportService.getAll($scope.evaluations.structure.id),
            ]);
            $scope.allReportModelPrintExport = initDataReportModel[0];
            const reportsModelForCheckSubmit = initDataReportModel[1];
            utilsTeacher.safeApply($scope);

            $scope.enableSubmit = $scope.infoProblemInTitle = false;
            $scope.newReportModel = new ReportModelPrintExport(undefined);
            $scope.newReportModel.setSelected(false);

            //functions share
            $scope.selectReportModel = function (reportModel: ReportModelPrintExportType): void {
                if (!reportModel.getId()) return;
                deSelectedAll();
                problemInTitle(reportModel);
                if ($scope.newReportModel.getSelected()) $scope.newReportModel.setSelected(false);
                if (reportModel["errorSameTitle"]) {
                    $scope.newReportModel.setSelected(true);
                    return;
                }
                reportModel.setSelected(true);
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
            const reportsModelsPrepareToDelete: Array<ReportModelPrintExportType> = [];
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

            $scope.submit = function (): void {
                if (!$scope.enableSubmit) {
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
                if (!($scope.allReportModelPrintExport.length > 0)) {
                    $scope.$parent.closeLightBoxSelectModelReport();
                } else {
                    const reportModelSelected: ReportModelPrintExportType = $scope.allReportModelPrintExport
                        .find((findReportModel: ReportModelPrintExportType) => findReportModel.getSelected());
                    $scope.$parent.closeLightBoxSelectModelReport(reportModelSelected);
                }
            };

            $scope.permanentControlTitle = function (title: String): void {
                $scope.enableSubmit = !allReportModelPrintExportChecked()
                    .some((reportModel: ReportModelPrintExport) => reportModel.getTitle() === title);
            };

            function deleteReportsModels(): void {
                reportsModelsPrepareToDelete.forEach((forReportModel: ReportModelPrintExportType): void => {
                    if (forReportModel.isDelete()) forReportModel.delete();
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
                    $scope.infoProblemInTitle = false;
                } else {
                    $scope.enableSubmit = false;
                    $scope.infoProblemInTitle = true;
                    reportModel["errorSameTitle"] = true;
                    if (reportModel["backupLastTitle"]) reportModel.setTitle(reportModel["backupLastTitle"])
                }
                if (!reportModel["errorSameTitle"]) $scope.enableSubmit = true;
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
                titlesAllReportsModels.push($scope.newReportModel.getTitle());
                return (new Set(titlesAllReportsModels)).size !== titlesAllReportsModels.length;
            }

            async function sendNew() {
                if ($scope.$parent) {
                    let scope: any = $scope.$parent;
                    let print: any = {};
                    if (scope.print) {
                        print = scope.print;
                        if (scope.mentionClass) {
                            $scope.$parent.mentionClass = print.mentionClass = scope.mentionClass
                        }
                        if (scope.orientationOpinion) {
                            $scope.$parent.orientationOpinion = print.orientationOpinion = scope.orientationOpinion
                        }
                        $scope.newReportModel.setPreferencesCheckboxWithClean(print);
                        $scope.newReportModel.setPreferencesTextWithClean(print);
                    }
                    $scope.newReportModel.setStructureId($scope.evaluations.structure.id);
                }
                await $scope.newReportModel.post();
            }

            function allReportModelPrintExportChecked(): Array<ReportModelPrintExportType> {
                if (!($scope.allReportModelPrintExport.length > 0)) return [];
                return $scope.allReportModelPrintExport;
            }

            function putReportsModels(): void {
                if (!_.isEqual(allReportModelPrintExportChecked(), reportsModelForCheckSubmit)) {
                    cleanReportModelNoEdit();
                    cleanTitleEmptyBeforePut();
                    for (const reportModel of allReportModelPrintExportChecked()) {
                        reportModel.put([KEY_TITLE, KEY_STRUCTUREID, KEY_SELECTED]);
                    }
                }

            }
        }
    ]);

export default reportModelPrintExportController;