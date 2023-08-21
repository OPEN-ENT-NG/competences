import {moment} from 'entcore';

export class NumberUtils {
    static roundUp(number: number, precision: number): number {
        return (+(Math.round(+(number * precision)) / precision));
    }

    static roundUpTenth(number: number): number {
        return this.roundUp(number, 1e1);
    }
}