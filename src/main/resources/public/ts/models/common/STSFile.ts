import httpAxios from 'axios';
import {Mix} from 'entcore-toolkit';
import {_, moment, notify, http, toasts} from 'entcore';

export class STSFile {

    id: number;
    id_etablissement: string;
    name_file: string;
    creation_date: Date;
    content: any;

    constructor ( id_structure: string, name: string, content: any ){
        this.name_file = name;
        this.id_etablissement = id_structure;
        this.content = content;
    }

    toJson(){
        return{
            id_structure: this.id_etablissement,
            name_file: this.name_file,
            content: JSON.stringify(this.content)
        }
    }

    async create () {
        try {

           let res = await httpAxios.post(`/competences/lsu/data/sts`,this.toJson());
            if (res.status == 200 ||  res.status == 201) {
                this.id = res.data.id;
                this.creation_date =  moment(res.data.creation_date).format("DD/MM/YYYY HH:mm");
                toasts.confirm('evaluation.lsu.confirm.save.sts.file')
            }else{
                toasts.info('evaluation.lsu.error.sts.file.create');
            }

        }catch(e){
            toasts.info('evaluation.lsu.error.sts.file.create');
        }

    }

}

export class STSFiles {
    all: STSFile[];
    selected: STSFile;

    constructor (){
        this.all = [];
        this.selected = null;
    }

    async sync(id_structure : string) {

        try {
            let res = await httpAxios.get(`/competences/lsu/sts/files/${id_structure}`);
            if (res.status == 200 ||  res.status == 201) {
                if (res.data !== null && res.data.length > 0) {
                    this.all = Mix.castArrayAs(STSFile, res.data);
                    this.all.map((stsFile) => {
                        stsFile.content = JSON.parse(stsFile.content);
                        stsFile.creation_date = moment(stsFile.creation_date).format("DD/MM/YYYY HH:mm");
                    });
                    this.selected = this.all[0];
                }
            }else{
                toasts.info('evaluation.lsu.error.sts.files.get');
            }
        }catch (e){

            toasts.info('evaluation.lsu.error.sts.files.get');
        }

    }
}