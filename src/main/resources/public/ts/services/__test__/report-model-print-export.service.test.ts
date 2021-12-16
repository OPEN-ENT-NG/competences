import axios from 'axios';
import {reportModelPrintExportService} from "../ReportModelPrintExportService";

describe('ReportModelPrintExportService Test', () => {

    describe('ReportModelPrintExportService getAll test', () => {
        // this test invoke a virtual js node's connection which will occurs with a console.error which we prevent it
        beforeEach(() => {
            jest.spyOn(console, 'error').mockImplementation(() => {});
        });

        it('returns data when retrieve request getAll is correctly called', done => {
            let spy = jest.spyOn(axios, "get");
            reportModelPrintExportService.getAll().then(response => {
                expect(spy).toHaveBeenCalledWith("/competences/reports-models-print-export");
                done();
            });
        });

    });
});
