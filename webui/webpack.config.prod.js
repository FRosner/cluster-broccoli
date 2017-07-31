const webpack = require('webpack');
const merge = require('webpack-merge');
const config = require('./webpack.config.js');

/**
 * Production configuration for webpack, via yarn package.  Provides more control about the yarn
 * development settings than webpack -p.  In particular we can get rid of UglifyJS which we don't
 * need.
 */
module.exports = merge(config, {
  plugins: [
    // Force all loaders to minimize their output and disable debugging tools
    new webpack.LoaderOptionsPlugin({
      minimize: true,
      debug: false
    }),
    // Switch libraries into production mode
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production')
      }
    })
  ]
});
