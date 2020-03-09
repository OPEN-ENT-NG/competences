import http, {AxiosResponse} from "axios";
import {notify, idiom as lang} from "entcore";

interface Iframe {
    status:Number;
    text:String;
    urlSrc:String;
}

export const getIframeFromPdfDownload = async function (url:string, strictContextualEscaping:any):Promise<Iframe> {
    /*
    Create the content for src in a iframe tag from PDF download url.
    strictContextualEscaping is $sce of AngularJs.
    Example in public/ts/controllers/eval_suivi_competences_classe_ctl.ts line 38.
     */
    try{
        try{
            const {data, status, statusText}:AxiosResponse = await http.get(url, {responseType: 'arraybuffer'});
            if(status === 200) {
                const file = new Blob([data], {type: 'application/pdf'});
                const fileURL = window.URL.createObjectURL(file);
                return {
                    status,
                    text: statusText,
                    urlSrc: strictContextualEscaping.trustAsResourceUrl(fileURL),
                };
            }
            throw new Error("Axios isn't status 200");
        } catch (errorNoData) {
            return {
                status: 400,
                text: lang.translate("competance.error.iframe.statusText"),
                urlSrc: "",
            };
        }
    } catch (errorGetPdf) {
        console.error(errorGetPdf);
        notify.error(`${lang.translate("competance.error.iframe.error")}: ${errorGetPdf}`);
        return {
            status: 418,
            text: "",
            urlSrc: "",
        };
    }
};