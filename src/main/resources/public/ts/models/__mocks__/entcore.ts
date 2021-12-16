declare let require: any;

export const moment = require('moment');
export const ng = {
    service: jest.fn()
};

export const model = {
    calendar: {
        dayForWeek: '2017-01-12T14:00:00.000+01:00'
    },
    me: {
        userId: '7b6459f5-2765-45b5-8086-d5b3f422e69e',
        type: 'PERSEDUCNAT',
        hasWorkflow: jest.fn(() => true),
        hasRight: jest.fn(() => true)
    },
};

export const notify = {
    error: jest.fn(() => true)
};