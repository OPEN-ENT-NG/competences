export function xmlToJson(xml: any) {
    var obj = {};

    if (xml.nodeType == 1) { // element
        // do attributes
        if (xml.attributes.length > 0) {
            obj["@attributes"] = {};
            for (var j = 0; j < xml.attributes.length; j++) {
                var attribute = xml.attributes.item(j);
                obj["@attributes"][attribute.nodeName] = attribute.nodeValue;
            }
        }
    } else if (xml.nodeType == 3) { // text
        obj = xml.nodeValue;
    }

    // do children
    if (xml.hasChildNodes()) {
        for (var i = 0; i < xml.childNodes.length; i++) {
            var item = xml.childNodes.item(i);
            var nodeName = item.nodeName;
            if (typeof(obj[nodeName]) == "undefined") {
                obj[nodeName] = this.xmlToJson(item);
            } else {
                if (typeof(obj[nodeName].push) == "undefined") {
                    var old = obj[nodeName];
                    obj[nodeName] = [];
                    obj[nodeName].push(old);
                }
                obj[nodeName].push(this.xmlToJson(item));
            }
        }
    }
    return obj;
};

export function cleanJson(array: any) {
    let finalArray = [];
    array.forEach(obj => {
        let finalObj = {};
        //get ID and type from xml attribute
        for (let key in obj["@attributes"]) {
            finalObj[key] = obj["@attributes"][key];
        }
        //remove usefull xml info
        delete obj["@attributes"];
        delete obj["#text"];

        //add propeties, working with neested object
        for (let key in obj) {
            if (Array.isArray(obj[key]["#text"])) {//neested
                delete obj[key]["#text"];
                var tmp = [];
                for (var i in obj[key])
                    tmp.push(obj[key][i]);
                finalObj[key] = cleanJson(tmp);
            }
            else {//regular
                finalObj[key] = obj[key]["#text"];
            }
        }
        finalArray.push(finalObj);
    });
    return finalArray;
};