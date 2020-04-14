import {AxiosResponse} from "axios";

export class MongoDBUtils {

    public static controlDataAndGetId(response:AxiosResponse):String{
        const { status, data } = response;
        if(status === 200 || status === 201){
            return data._id;
        }
        return undefined;
    }

    public static controlDataAndGetResult(response:AxiosResponse):Array<any>{
        const { status, data } = response;
        if(status === 200){
            return data.results;
        }
        return undefined;
    }

    public static isDataChangedWithoutResult(response:AxiosResponse):Boolean{
        const { status } = response;
        if(status === 200){
            return true;
        }
        return false;
    }
}