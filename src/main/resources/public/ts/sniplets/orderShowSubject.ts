import {$, toasts} from 'entcore';
import * as utils from '../utils/teacher';
import http, {AxiosResponse} from "axios";
import {Subject} from "../models/type";
import {Common} from '../constants'

const {
    DRAG_UP,
    DRAG_DOWN,
    UP,
    URL_LIFE_SCHOOL,
    URL_COMPETENCE
} = Common;

export const orderShowSubject = {
    title: 'Manage rank subjects',
    description: 'Change, delete or create rank\'s subjects',
    that: undefined,
    controller: {
        initServices: async function (): Promise<void> {
            await orderShowSubject.that.init();
        },
        init: async function (): Promise<void> {
            console.log("OrderShowSubject");
            this.isLoadingOrderShowSubject = true;
            this.isDragStart = false;
            const dirtySubjects = await this.getSubjects(this.source.id);
            this.subjects = this.prepareSubjectsData(dirtySubjects);
            this.stopLoader(true);
            orderShowSubject.that = this;
        },
        dropped: async function (dragSubject: Subject, index: number): Promise<void> {
            orderShowSubject.that.cleanDrag();
            if (index === dragSubject.rank) return; // subject is same in the list

            this.isLoadingOrderShowSubject = true;
            let direction: String = undefined;
            let newRank: number = undefined;

            if (index === undefined || index < dragSubject.rank) {
                direction = DRAG_UP;
                newRank = index === undefined ? 0 : index + 1;
            } else if (index > dragSubject.rank) {
                direction = DRAG_DOWN;
                newRank = index;
            } else {
                return;
            }

            if (!direction) return;

            let isSuccess: Boolean = await orderShowSubject.that.reshuffleRank(
                dragSubject,
                direction === DRAG_UP ? newRank : dragSubject.rank,
                direction === DRAG_UP ? -1 : newRank
            );

            if (isSuccess) await orderShowSubject.that.refreshOrder();
        },
        startDrag: function (subject): void {
            orderShowSubject.that.subjects.forEach((subjectForEach: Subject): void => {
                subjectForEach.isDrag = false;
            });
            subject.isDrag = true;
            orderShowSubject.that.isDragStart = true;
            $(".main").mouseup(() => {
                orderShowSubject.that.cleanDrag();
            })
        },
        cleanDrag: function (): void {
            orderShowSubject.that.isDragStart = false;
            $(".main").off("mouseup");
            orderShowSubject.that.subjects.forEach(subjectForEach => {
                delete subjectForEach.isDrag;
            })
        },
        refreshOrder: async function (): Promise<void> {
            orderShowSubject.that.isLoadingOrderShowSubject = true;
            const dirtySubjects = await orderShowSubject.that.getSubjects(orderShowSubject.that.source.id);
            orderShowSubject.that.subjects = orderShowSubject.that.prepareSubjectsData(dirtySubjects);
            orderShowSubject.that.stopLoader();
        },
        moveSubject: async function (indexSubjectSelected: number, direction: String): Promise<void> {
            if (!direction) return;

            const subjectDirect: Subject = orderShowSubject.that.subjects
                .find((subjectFind: Subject, index: number): Boolean => index === indexSubjectSelected);

            const isSuccess: Boolean = await orderShowSubject.that.reshuffleRank(
                subjectDirect,
                direction === UP ? indexSubjectSelected - 1 : indexSubjectSelected,
                direction === UP ? -1 : indexSubjectSelected + 1 );
            if (isSuccess) await orderShowSubject.that.refreshOrder();
        },
        prepareSubjectsData: function (subjects: Array<any>): Array<Subject> {
            if (!subjects) return [];
            return subjects.map((subjectMap: any): any => {
                return {
                    name: subjectMap.name,
                    id: subjectMap.id,
                    rank: subjectMap.rank,
                    code: subjectMap.externalId,
                    source: subjectMap.source
                }
            })
        },
        stopLoader: function (isInitiation = false): void {
            const scope = isInitiation ? this : orderShowSubject.that;
            scope.isLoadingOrderShowSubject = false;
            utils.safeApply(scope);
        },
        getSubjects: async function (idStructure: string): Promise<Array<any>> {
            try {
                const {data, status}: AxiosResponse = await http.get(`${URL_LIFE_SCHOOL}/matieres?idEtablissement=${idStructure}`);
                if (status === 200) return data;
                throw new Error("getSubjects");
            } catch (error) {
                toasts.warning('evaluations.service.error.matiere');
                this.stopLoader(true);
            }
        },
        reshuffleRank: async function ({id}: Subject, indexStart: number, indexEnd): Promise<Boolean> {
            try {
                let urlUpdateSubjectRank = `${URL_COMPETENCE}/subjects/reshuffle-rank?idEtablissement=${orderShowSubject.that.source.id}`;

                const body = {
                    idStructure: orderShowSubject.that.source.id,
                    id,
                    indexStart,
                    indexEnd,
                };

                const {data, status}: AxiosResponse = await http.put(urlUpdateSubjectRank, body);
                if (status === 200) return "ids" in data;
                throw new Error("updateRank");
            } catch (e) {
                toasts.warning('evaluations.service.error.matiere');
                orderShowSubject.that.stopLoader();
                return false;
            }
        },
        initOrderDefault: async function (): Promise<void> {
            try {
                orderShowSubject.that.isLoadingOrderShowSubject = true;
                const URL_UPDATE = `${URL_COMPETENCE}/subjects/${orderShowSubject.that.source.id}/id-structure/initialization-rank`;
                const {status, data}: AxiosResponse = await http.delete(URL_UPDATE);
                if (status === 200 && ("ids" in data)) await orderShowSubject.that.refreshOrder();
            } catch (error) {
                orderShowSubject.that.failRequest('evaluations.service.error.matiere');
            }
        },
        failRequest: function (keyI18n: String): Boolean {
            toasts.warning(keyI18n);
            orderShowSubject.that.stopLoader();
            return false;
        }
    }
};