db.runCommand(
    {
        createIndexes: "notes_export",
        indexes: [
            {
                key: {
                    "status": 1,
                },
                name: "notes_export_tags_index"
            }
        ]
    });