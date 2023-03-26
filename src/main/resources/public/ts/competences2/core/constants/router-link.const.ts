import {RouterLink} from "../../models/router-link.model";
import {competencesLink} from "./competences-link.const";

export const routerLinks: Array<RouterLink> = [
    {
        icon: "competences-home",
        link: competencesLink.HOME,
        name: "home",
        isActive: true
    },
    {
        icon: "competences-note",
        link: competencesLink.NOTES,
        name: "ariane.competences.viewNotesDevoir",
        children: {
            name: "ariane.competences.viewNotesDevoir",
            routerLinks: [
                {
                    icon: "competences-note",
                    link: "competences#/notes",
                    name: "ariane.competences.viewNotesDevoir",
                },
                {
                    icon: "competences-note",
                    link: "competences#/notes",
                    name: "ariane.competences.viewNotesDevoir",
                }
            ]
        }
    },
    {
        icon: "competences-orientations",
        link: competencesLink.FOLLOW,
        name: "follow.area"
    },
    {
        icon: "competences-chart",
        link: competencesLink.ORIENTATIONS,
        name: "orientations"
    },
    {
        icon: "competences-exports",
        link: competencesLink.EXPORTS,
        name: "evaluations.exports",
        children: {
            name: "ariane.competences.viewNotesDevoir",
            routerLinks: [
                {
                    icon: "competences-note",
                    link: "competences#/notes",
                    name: "ariane.competences.viewNotesDevoir",
                },
                {
                    icon: "competences-note",
                    link: "competences#/notes",
                    name: "ariane.competences.viewNotesDevoir",
                }
            ]
        }
    }
];
