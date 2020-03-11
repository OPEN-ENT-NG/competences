export class Attachment {
    file: File;
    progress: {
        total: number,
        completion: number
    };
    _id: string;
    filename: string;
    size: number;
    contentType: string;

    constructor(file: File) {
        this.file = file;
        this.filename = file.name;
        this.progress = {
            total: 100,
            completion: 0
        };
    }
}