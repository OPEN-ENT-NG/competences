import {Me} from "entcore";
import http from 'axios';
export let evaluationCreationCompetencesDevoir  = 'evaluationCreationCompetencesDevoir';
export let evaluationCreationEnseignements  = 'evaluationCreationEnseignements';
export let evaluationCreationCompetences  = 'evaluationCreationCompetences';

export class PreferencesUtils {

    static  competencesStrInPref = 'competences';
    static async initPreference(): Promise<void> {
        await Me.preference(PreferencesUtils.competencesStrInPref);

        if (Me.preferences
               && Object.keys(Me.preferences[PreferencesUtils.competencesStrInPref]).length === 0
               && Me.preferences[PreferencesUtils.competencesStrInPref].constructor === Object) {
            Me.preferences[PreferencesUtils.competencesStrInPref]  = {};
            await Me.savePreference(PreferencesUtils.competencesStrInPref);
        }

    };


    static async savePreferences(keyArrays: any, datasArray:any): Promise<void> {
        if( Me.preferences[PreferencesUtils.competencesStrInPref]    instanceof Array) {
            Me.preferences[PreferencesUtils.competencesStrInPref] = {};
        }

        if(keyArrays.length > 0 && datasArray.length > 0 && keyArrays.length === datasArray.length){
            keyArrays.forEach((key,index) =>{
                let object = { [key]: datasArray[index] };
                Me.preferences[PreferencesUtils.competencesStrInPref] [key] = object[key];
            })
        }
        await Me.savePreference(PreferencesUtils.competencesStrInPref);
    }

    static isNotEmpty(key ?: string) {
        let isNotEmpty ;
        (key)
            ? 
              isNotEmpty =  Me.preferences
            && Me.preferences[PreferencesUtils.competencesStrInPref][key.toString()]
            && Me.preferences[PreferencesUtils.competencesStrInPref][key.toString()].length !== 0
            :
              isNotEmpty =  Me.preferences
            && Me.preferences[PreferencesUtils.competencesStrInPref]
            &&  Me.preferences[PreferencesUtils.competencesStrInPref].length !==0;
          
        return isNotEmpty
    }

    static getPreferences(key: string) {
        return  Me.preferences[PreferencesUtils.competencesStrInPref][key.toString()];
    }
}
