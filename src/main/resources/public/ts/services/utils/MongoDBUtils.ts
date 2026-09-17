import { HttpResponse } from 'entcore-toolkit';

export class MongoDBUtils {

    public static controlDataAndGetId(response:HttpResponse):String{
        const { status, data } = response;
        if(status === 200 || status === 201){
            return data._id;
        }
        return undefined;
    }

    public static controlDataAndGetResult(response:HttpResponse):Array<any>{
        const { status, data } = response;
        if(status === 200){
            return data.results;
        }
        return undefined;
    }

    public static isDataChangedWithoutResult(response:HttpResponse):Boolean{
        const { status } = response;
        if(status === 200){
            return true;
        }
        return false;
    }
}