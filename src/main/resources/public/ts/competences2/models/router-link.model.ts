export type RouterLink = {
    icon: string;
    link: string;
    name: string;
    isActive?: boolean;
    canAccess?: boolean;
    children?: RouterLinkChildren;
};

export type RouterLinkChildren = {
    name: string;
    routerLinks?: Array<RouterLink>;
}