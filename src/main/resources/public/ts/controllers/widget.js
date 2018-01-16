/*
 * Copyright (c) Région Hauts-de-France, Département 77, CGI, 2016.
 *
 * This file is part of OPEN ENT NG. OPEN ENT NG is a versatile ENT Project based on the JVM and ENT Core Project.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with OPEN ENT NG is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of OPEN ENT NG, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 */

/**
 * Created by ledunoiss on 08/08/2016.
 */
var mesNotes = model.widgets.findWidget("evaluations");

mesNotes.eleves = [];
var listeMatieres = [];
mesNotes.myType = model.me.type;
mesNotes.activeId = undefined;

var formatDate = function(date){
    return moment(date).format("DD/MM");
};

var getNotes = function (uidEleve,y, callback) {
    http().getJson("/viescolaire/evaluations/widget?userId="+uidEleve).done(function(res){

        for(var i = 0; i < res.length ; i++){
            if(listeMatieres.indexOf(res[i].idmatiere)===-1){
                listeMatieres.push(res[i].idmatiere);
            }
        }
        http().get("/viescolaire/evaluations/widget/matieres", {idmatiere : listeMatieres}).done(function(matieres){
            var matiereIdName = {};
            _.each(matieres , function(matiere){
                matiereIdName[matiere.n.data.id] = matiere.n.data.name;
            });
            _.each(res, function(note){
                note.libelleMatiere = matiereIdName[note.idmatiere];
                note.date = formatDate(note.date);
            });
            callback(res,y);
        });
    });
};

if(model.me.type==="ELEVE"){
    mesNotes.eleves[0] = model.me;
    mesNotes.eleves[0].id = mesNotes.eleves[0].userId;
    mesNotes.activeId = mesNotes.eleves[0].userId;
    getNotes(mesNotes.eleves[0].userId,null, function(evaluations,y){
        mesNotes.eleves[0].evaluations = evaluations;
        model.widgets.apply();
    });
}else if (model.me.type==="PERSRELELEVE"){
    http().getJson("/viescolaire/evaluations/enfants?userId="+model.me.userId).done(function(enfants){
        mesNotes.eleves = enfants;
        if(mesNotes.eleves.length > 0){
            mesNotes.activeId = mesNotes.eleves[0]["n.id"];
            for( var i = 0 ; i< mesNotes.eleves.length;i++){
                mesNotes.eleves[i] = {id:mesNotes.eleves[i]["n.id"], displayName: mesNotes.eleves[i]["n.displayName"], structures:[mesNotes.eleves[i]["s.id"]],evaluations:[]}
                getNotes(mesNotes.eleves[i].id,i,function(evaluations,y){
                    mesNotes.eleves[y].evaluations = evaluations;
                    model.widgets.apply();
                });
            }
        }
    });
}
