package fr.openent.competences.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Folder extends Model {
    List<Folder> subfolders = new ArrayList<>();
    List<PdfFile> bulletins = new ArrayList<>();


    String id_folder;
    String name;



    String id_parent="";
    public Folder(String name){
        this.name = name;
    }


    public void addFolder(Folder folder){
        subfolders.add(folder);
    }

    public void addBulletin(PdfFile pdf){
        bulletins.add(pdf);
    }

    public String getName() {
        return name;
    }
    public String getId_folder() {
        return id_folder;
    }
    public String getId_parent() { return id_parent; }

    public List<Folder> getSubfolders() {
        return subfolders;
    }

    public List<PdfFile> getBulletins() {
        return bulletins;
    }
    public void setId_parent(String id_parent) { this.id_parent = id_parent; }
    public void setId_folder(String id_folder) {
        this.id_folder = id_folder;
    }

    @Override
    public JsonObject toJsonObject() {
        JsonArray folders = new JsonArray();
        JsonArray bulletinspdf = new JsonArray();
        JsonObject jo = new JsonObject()
                .put("name",this.name)
                .put("parent",this.id_parent)
                .put("id",this.id_folder)
                .put("type","folder");
//        subfolders.stream().forEach(folder -> folders.add(folder.toJsonObject()));
//        bulletins.stream().forEach(bulletinPdf -> bulletinspdf.add(bulletinPdf.toJsonObject()));

        if(bulletinspdf.size() != 0 ){
            jo.put("bulletins",bulletinspdf);
        }
        if(folders.size()!=0){
            jo.put("folders",folders);
        }
        return jo;
    }

    public List<JsonObject> getAllFilesAndFolders() {
        List<JsonObject> results = new ArrayList<>();
        results.add(this.toJsonObject());
        for (Folder f  : this.subfolders) {
            results.addAll(f.getAllFilesAndFolders());
        }
        for( PdfFile bulletin : this.bulletins ){
            results.add(bulletin.toJsonObject());
        }
        return results;
    }
}
