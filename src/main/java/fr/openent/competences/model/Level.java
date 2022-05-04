package fr.openent.competences.model;

import io.vertx.core.json.JsonObject;

public class Level {
    private String name;
    private String image;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public JsonObject toJsonObject() {
        JsonObject result = new JsonObject();
        result.put("level",this.name)
                .put("imgLevel",this.image)

        ;
        return result;
    }
}
