import { Collection } from 'entcore';
import { Devoir, DevoirsCollection } from './index';

export interface Devoirs extends Collection<Devoir>, DevoirsCollection {
    all: Devoir[];
    synchronizedDevoirType: () => any;

    //TODO Delete when infra-front will be fixed
    one: (name, mixin) => void;
    on: (name, mixin) => void;
    findWhere: (params) => any;
    trigger: (action: string) => void;
    updateData: (o) => void;
    collection: (type, mixin, name?) => void;
    where: (params) => any;
    sync: () => any;
    getPercentDone: (mixin?) => any;
}