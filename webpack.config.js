var webpack = require('webpack');
var path = require('path');

module.exports = {
    context: path.resolve(__dirname, './src/main/resources/public'),
    entry: {
        parents: './ts/parents.ts',
        teachers: './ts/teachers.ts',
        behaviours: './ts/behaviours.ts'
    },
    output: {
        filename: './[name].js'
    },
    externals: {
        "entcore/entcore": "entcore",
        "entcore": "entcore",
        "moment": "entcore",
        "underscore": "entcore",
        "jquery": "entcore",
        "angular": "angular"
    },
    resolve: {
        modulesDirectories: ['node_modules'],
        extensions: ['', '.ts', '.js'],
        alias: {
            'chart.js': path.resolve(__dirname, './node_modules/chart.js'),
            'color': path.resolve(__dirname, './node_modules/color'),
            'animejs': path.resolve(__dirname, './node_modules/animejs'),
        }
    },
    devtool: "source-map",
    module: {
        loaders: [
            {
                test: /\.ts$/,
                loader: 'ts-loader'
            }
        ]
    }
}