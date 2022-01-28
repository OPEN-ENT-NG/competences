import {angular, idiom, moment, toasts} from 'entcore';
import {PeriodsService} from "../../services/periods.service";
import {DevoirsService} from "../../services/devoirs.service";
import {DateUtils} from "../../utils/date";
import {Devoir, Evaluation, Matiere, NiveauCompetence, Structure} from "../../models/teacher";
import {Period} from "../../models/common/Periode";

declare let window: any;

interface IPeriodSubjects {
    period: Period;
    subjects: Array<Matiere>;
}

enum Display {
    LastNotes = 1,
    AverageSubjects = 2,
    SubjectDetail = 3
}

interface IViewModel {
    $eval: any;
    displayEnum: typeof Display;
    that: any;
    disabled: boolean;
    student: string;
    group: Array<string>;
    notes: Array<Evaluation>;
    arrayPeriodSubjects: Array<IPeriodSubjects>;
    display: Display;
    structure: Structure;

    selected: { periodSubjects: IPeriodSubjects, subject: Matiere };

    yearPeriodSubjects: IPeriodSubjects;
    lastNotesPeriodSubjects: IPeriodSubjects;
    defaultSubject: Matiere;

    apply(): void;

    init(student: string, group: Array<string>): Promise<void>;

    //Method to change periods
    /**
     * Allows to go to the next period
     */
    nextPeriod(): void;

    /**
     * Allows to go to the previous period
     */
    previousPeriod(): void;

    /**
     * Allows to change period
     * @param index period index
     */
    changePeriod(index: number): void;

    //Display method
    /**
     * Returns true if we are on the last notes
     */
    isLastNotesDisplay(): boolean;

    /**
     * Returns true if we are on the average subjects
     */
    isAverageSubjectsDisplay(): boolean;

    /**
     * Returns true if we are on the subject detail
     */
    isSubjectDetailDisplay(): boolean;

    //Get periods information
    /**
     * Returns the name of a period
     * @param periodSubjects IPeriodSubjects
     */
    getPeriodLabel(periodSubjects: IPeriodSubjects): string;

    /**
     * Returns the selected period
     */
    getSelectedPeriodLabel(): string;

    /**
     * Returns the current period
     * @param periodSubjects list of all IPeriodSubjects
     */
    getCurrentPeriod(periodSubjects: Array<IPeriodSubjects>): IPeriodSubjects;

    /**
     * Returns true is the selected period at least one subject
     */
    isSelectedPeriodHasSubject(): boolean;

    //Get subject information
    /**
     * Returns a subject according to its id.
     * This material is sought in the subject of the supplied IPeriodSubjects.
     * @param subjectId Matiere id
     * @param periodSubjects IPeriodSubjects to search
     */
    getSubjectInPeriod(subjectId: string, periodSubjects: IPeriodSubjects): Matiere;

    /**
     * Returns the name of a subject
     * @param subject Matiere
     */
    getSubjectName(subject: Matiere): string;

    /**
     * Returns the average of a subject
     * @param subject Matiere
     */
    getSubjectAverage(subject: Matiere): string;

    /**
     * Returns the average of a selected subject
     * @param subject Matiere
     */
    getSubjectAverageSelectedPeriod(subject: Matiere): string;

    /**
     * Returns the coeff of a subject
     * @param subject Matiere
     */
    getSubjectCoeff(subject: Matiere): string;

    /**
     * Returns the teacher of a subject
     * @param subject Matiere
     */
    getSubjectTeacherName(subject: Matiere): string;

    /**
     * Returns the selected subject
     */
    getSelectedSubjectName(): string;

    /**
     * Returns the name of a subject
     * @param subject Matiere
     */
    isLastSubject(subject: Matiere): boolean;

    /**
     * Returns the border color according to the index of subject
     * @param subject Matiere
     */
    getBorderColor(subject: Matiere): string;

    //Get note information
    /**
     * Returns true if the note has skills
     * @param note Evaluation
     */
    isNoteHasCompetences(note: Evaluation): boolean;

    /**
     * Returns true if there is at least one note to display
     */
    hasLastNote(): boolean;

    /**
     * Returns the background color of the skills according to the grade
     * @param note Evaluation
     */
    getNoteBackgroundColor(note: Evaluation): string;

    /**
     * returns the color of the skill according to its value
     * @param evaluation value that was given to the skill
     */
    getCompetencesColor(evaluation: number): string;

    //Get homeworks information
    /**
     * Returns the list of homework for the selected subject
     */
    getSelectedSubjectHomeworks(): Array<Devoir>;

    /**
     * Returns true if the selected subject contains at least one homework
     */
    isSelectedSubjectHasHomeworks(): boolean;

    //Utility method
    /**
     * Allows to convert a date to a desired format
     * @param date the date
     * @param format the desired format
     */
    formatDate(date: string, format: string): string;

    /**
     * Allows to translate the name of subject of all IPeriodSubjects
     * @param periodSubjects list of all IPeriodSubjects
     */
    translatePeriods(periodSubjects: Array<IPeriodSubjects>): void;

    /**
     * Add the last note period
     */
    addLastNotePeriod(): Promise<void>;

    /**
     * Add the year period
     */
    addYearPeriod(): Promise<void>;

    /**
     * Loads the periods of the establishment
     */
    loadPeriods(): Promise<void>;

    /**
     * Loads subject of the an IPeriodSubjects
     * @param period the IPeriodSubjects
     */
    loadSubjects(period: IPeriodSubjects): Promise<void>;

    /**
     * Change the period. Call by select
     */
    switchPeriod(): void;

    /**
     * Change the subject. Call by select
     */
    switchSubject(): void;
}

const vm: IViewModel = {
    $eval: null,
    displayEnum: Display,
    that: null,
    disabled: false,
    student: null,
    group: null,
    apply: null,
    notes: [],
    arrayPeriodSubjects: [],
    display: Display.LastNotes,
    selected: {
        periodSubjects: {
            period: {
                label: "",
                timestamp_dt: undefined,
                timestamp_fn: undefined
            },
            subjects: []
        },
        subject: new Matiere({id: -1})
    },
    yearPeriodSubjects: null,
    lastNotesPeriodSubjects: null,
    defaultSubject: new Matiere({id: -1, matiere: `${idiom.translate(`matieres`)}`, matiere_rank: -1}),
    structure: null,
    async init(student: string, group: Array<string>): Promise<void> {
        vm.disabled = false;
        vm.student = student;
        vm.group = group;
        vm.notes = [];
        vm.arrayPeriodSubjects = [];
        vm.display = Display.LastNotes;
        vm.selected.subject = new Matiere({id: -1});
        vm.selected.periodSubjects = {
            period: {
                label: "",
                timestamp_dt: undefined,
                timestamp_fn: undefined
            },
            subjects: []
        };
        vm.yearPeriodSubjects = null;
        vm.lastNotesPeriodSubjects = null;
        vm.defaultSubject = new Matiere({id: -1, matiere: `${idiom.translate(`matieres`)}`, matiere_rank: -1});
        vm.structure = new Structure({id: window.structure.id});

        vm.structure.niveauCompetences.sync();
        vm.notes = await DevoirsService.getLatestNotes(window.structure.id, vm.student);
        vm.arrayPeriodSubjects = [];

        await vm.loadPeriods();
        if (vm.arrayPeriodSubjects.length === 0) {
            vm.disabled = true;
            vm.apply();
            return;
        }
        await vm.addYearPeriod();
        await vm.addLastNotePeriod();
        vm.arrayPeriodSubjects.sort((p1: IPeriodSubjects, p2: IPeriodSubjects) => p1.period.ordre - p2.period.ordre);
        vm.selected.periodSubjects = vm.arrayPeriodSubjects[0];

        vm.apply();
    },

    //Method to change periods
    nextPeriod(): void {
        const index = vm.arrayPeriodSubjects.indexOf(vm.selected.periodSubjects) + 1;
        if (index < vm.arrayPeriodSubjects.length) {
            vm.changePeriod(index);
        }
    },
    previousPeriod(): void {
        const index = vm.arrayPeriodSubjects.indexOf(vm.selected.periodSubjects) - 1;
        if (index > 0) {
            vm.changePeriod(index);
        }
    },
    changePeriod(index: number): void {
        vm.loadSubjects(vm.arrayPeriodSubjects[index])
            .then(() => {
                vm.selected.periodSubjects = vm.arrayPeriodSubjects[index];
                if (vm.display == vm.displayEnum.AverageSubjects) {
                    vm.selected.subject = vm.defaultSubject;
                }
                vm.apply();
            }).catch(() => {
                toasts.warning('competences.memento.notes.init.failed');
            }
        );
    },

    //Display method
    isLastNotesDisplay(): boolean {
        return vm.display == vm.displayEnum.LastNotes;
    },
    isAverageSubjectsDisplay(): boolean {
        return vm.display == vm.displayEnum.AverageSubjects;
    },
    isSubjectDetailDisplay(): boolean {
        return vm.display == vm.displayEnum.SubjectDetail;
    },

    //Get periods information
    getPeriodLabel(periodSubjects: IPeriodSubjects): string {
        return periodSubjects.period.label;
    },
    getSelectedPeriodLabel(): string {
        return this.selected.periodSubjects.period.label;
    },
    getCurrentPeriod(periodSubjects: Array<IPeriodSubjects>): IPeriodSubjects {
        let currentPeriod: IPeriodSubjects = periodSubjects.find((period: IPeriodSubjects) => period != vm.yearPeriodSubjects
            && period != vm.lastNotesPeriodSubjects
            && moment().isBetween(period.period.timestamp_dt, period.period.timestamp_fn));

        return currentPeriod || vm.yearPeriodSubjects;
    },
    isSelectedPeriodHasSubject(): boolean {
        return vm.selected.periodSubjects.subjects
            && vm.selected.periodSubjects.subjects.length > 0;
    },

    //Get subject information
    getSubjectInPeriod(subjectId: string, periodSubjects: IPeriodSubjects): Matiere {
        return (subjectId && periodSubjects && periodSubjects.subjects) ? periodSubjects.subjects.find(subject => subject.id == subjectId) : undefined;
    },
    getSubjectName(subject: Matiere): string {
        return (subject && subject.matiere) ? subject.matiere : "-";
    },
    getSubjectAverage(subject: Matiere): string {
        return (subject && subject.moyenne) ? `${subject.moyenne}` : "-";
    },
    getSubjectAverageSelectedPeriod(): string {
        const subjectsSelectedPeriod: Matiere = vm.getSubjectInPeriod(vm.selected.subject.id, vm.selected.periodSubjects);
        return (subjectsSelectedPeriod && subjectsSelectedPeriod.moyenne) ? `${subjectsSelectedPeriod.moyenne}` : "-";
    },
    getSubjectCoeff(subject: Matiere): string {
        return (subject && subject.matiere_coeff) ? `${subject.matiere_coeff}` : "-";
    },
    getSubjectTeacherName(subject: Matiere): string {
        return subject && subject.teacher ? subject.teacher : "";
    },
    getSelectedSubjectName(): string {
        return vm.selected.subject.matiere ? vm.selected.subject.matiere : "";
    },
    isLastSubject(subject: Matiere): boolean {
        return vm.selected.periodSubjects.subjects.indexOf(subject) == vm.selected.periodSubjects.subjects.length - 1;
    },
    getBorderColor(subject: Matiere): string {
        return vm.selected.periodSubjects.subjects.indexOf(subject) % 2 == 0 ? "#ffe0b3" : "orange";
    },

    //Get note information
    isNoteHasCompetences(note: Evaluation): boolean {
        return note.competences ? (note.competences.length > 0) : false;
    },
    hasLastNote(): boolean {
        return vm.notes.length > 0;
    },
    getNoteBackgroundColor(note: Evaluation): string {
        let color: string = Number(note.moyenne) < Number(note.note) ? "#46BFAF" : "#FA9701";
        return Number(note.moyenne) > Number(note.note) ? "#E61610" : color;
    },
    getCompetencesColor(evaluation: number): string {
        const niveauCompetence: NiveauCompetence = vm.structure.niveauCompetences.all.find(niveauCompetence => niveauCompetence.id_niveau == evaluation);
        let color: string = niveauCompetence !== undefined ? niveauCompetence.couleur : '#DDDDDD';
        return color ? color : niveauCompetence.default;
    },

    //Get homeworks information
    getSelectedSubjectHomeworks(): Array<Devoir> {
        const subject: Matiere = vm.getSubjectInPeriod(vm.selected.subject.id, vm.selected.periodSubjects);
        const homeworks: Array<Devoir> = [];
        return (subject && subject.devoirs) ? subject.devoirs : homeworks;
    },
    isSelectedSubjectHasHomeworks(): boolean {
        return vm.getSelectedSubjectHomeworks().length > 0;
    },

    //Utility method
    formatDate(date: string, format: string): string {
        return DateUtils.format(date, format);
    },
    translatePeriods(periodSubjects: Array<IPeriodSubjects>): void {
        periodSubjects.forEach(element => element.period.label = `${idiom.translate(`viescolaire.periode.${element.period.type}`)}  ${element.period.ordre}`);
    },
    async addLastNotePeriod(): Promise<void> {
        vm.lastNotesPeriodSubjects = {
            period: {
                ordre: -1,
                timestamp_dt: vm.yearPeriodSubjects.period.timestamp_dt,
                timestamp_fn: vm.yearPeriodSubjects.period.timestamp_fn,
                label: `${idiom.translate(`competences.memento.notes.last.notes`)}`
            },
            subjects: undefined
        }
        await vm.loadSubjects(vm.lastNotesPeriodSubjects);
        vm.lastNotesPeriodSubjects.subjects.push(vm.defaultSubject);
        vm.lastNotesPeriodSubjects.subjects.sort((m1: Matiere, m2: Matiere) => m1.matiere_rank - m2.matiere_rank);
        vm.selected.subject = vm.defaultSubject;
        vm.arrayPeriodSubjects.push(vm.lastNotesPeriodSubjects);
    },
    async addYearPeriod(): Promise<void> {
        let yearPeriod: Period = {
            id: null,
            label: `${idiom.translate(`presences.year`)}`,
            ordre: 0,
            timestamp_dt: vm.arrayPeriodSubjects[0].period.timestamp_dt,
            timestamp_fn: vm.arrayPeriodSubjects[vm.arrayPeriodSubjects.length - 1].period.timestamp_fn
        }
        vm.yearPeriodSubjects = {period: yearPeriod, subjects: undefined};
        await vm.loadSubjects(vm.yearPeriodSubjects);
        vm.arrayPeriodSubjects.push(vm.yearPeriodSubjects);
    },
    async loadPeriods(): Promise<void> {
        const periods: Array<Period> = await PeriodsService.getPeriods(window.structure.id, vm.group);
        periods.forEach(period => vm.arrayPeriodSubjects.push({period: period, subjects: undefined}));

        vm.translatePeriods(vm.arrayPeriodSubjects);
    },
    async loadSubjects(period: IPeriodSubjects): Promise<void> {
        if (!period.subjects) {
            const subjects: Array<Matiere> = await DevoirsService.getDetailsStudentSubject(window.structure.id, vm.student, period.period);
            subjects.sort((m1: Matiere, m2: Matiere) => m1.matiere_rank - m2.matiere_rank);
            period.subjects = subjects;
        }
    },
    switchPeriod(): void {
        vm.selected.subject = vm.defaultSubject;
        if (vm.selected.periodSubjects.period.ordre < 0) {
            vm.display = Display.LastNotes;
        } else {
            vm.switchSubject();
        }
    },
    switchSubject(): void {
        // if the selected subject is valid
        if (vm.selected.subject.id != -1 && vm.selected.subject.matiere_rank >= 0) {
            if (vm.selected.periodSubjects == vm.lastNotesPeriodSubjects) {
                vm.selected.periodSubjects = vm.getCurrentPeriod(vm.arrayPeriodSubjects);
            }
            vm.loadSubjects(vm.selected.periodSubjects).then(() => {
                vm.display = Display.SubjectDetail;
                vm.apply();
            }).catch(() => {
                toasts.warning('competences.memento.notes.init.failed');
            });

        }
        // if the selected period is valid
        else if (vm.selected.periodSubjects.period.ordre >= 0) {
            vm.loadSubjects(vm.selected.periodSubjects).then(() => {
                vm.display = Display.AverageSubjects;
                vm.apply();
            }).catch(() => {
                toasts.warning('competences.memento.notes.init.failed');
            });
        }
        // else it's the last notes
        else {
            vm.display = Display.LastNotes;
        }
    },
};


export const notesMementoWidget = {
    title: 'competences.memento.notes.title',
    public: false,
    controller: {
        init: function () {
            this.vm = vm;
            this.setHandler();
            vm.$eval = this.$eval;
        },
        setHandler: function () {
            if (!window.memento) return;
            this.$on('memento:init', (evt, {student, group}) => {
                const sniplet = document.getElementById('memento-notes-sniplet');
                vm.apply = angular.element(sniplet).scope().$apply;
                vm.init(student, group).catch(() => {
                    toasts.warning('competences.memento.notes.init.failed');
                })
            });
            this.$on('memento:close', () => {
            });
        }
    }
};