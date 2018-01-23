import { Collection } from 'entcore';
import { Devoir, DevoirsCollection } from './index';

export interface Devoirs extends Collection<Devoir>, DevoirsCollection {
    all: Devoir[];
    synchronizedDevoirType: () => any;
}