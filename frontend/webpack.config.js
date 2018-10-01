var webpack = require("webpack");
var merge = require("webpack-merge");
var common = require("./webpack.common.js");

var UglifyJsPlugin = require('uglifyjs-webpack-plugin');

module.exports = merge(common, {
    devtool: "source-map",
    plugins: [
        new webpack.DefinePlugin({
            'process.env': {
                NODE_ENV: JSON.stringify('production')
            }
        })//,
        // new webpack.optimize.UglifyJsPlugin({
        //     sourceMap: true
        // })
    ],
    optimization: {
        minimizer: [
            // we specify a custom UglifyJsPlugin here to get source maps in production
            new UglifyJsPlugin({
                cache: true,
                parallel: true,
                uglifyOptions: {
                    compress: true,
                    ecma: 6,
                    mangle: true
                },
                sourceMap: true
            })
        ]
    }
});