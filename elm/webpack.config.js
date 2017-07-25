const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const template = require('html-webpack-template');

const outputPath = 'dist';

module.exports = {
  entry: './src/index.js',
  output: {
    path: path.resolve(__dirname, outputPath),
    filename: '[name]-[hash].js'
  },
  devServer: {
    // Serve from content base
    contentBase: outputPath,
    // We have a single-page application so do not navigate
    historyApiFallback: true,
    // Forward API requests to the running backend
    proxy: {
      '/api': 'http://localhost:9000',
      '/ws': {
        target: 'ws://localhost:9000/',
        ws: true
      }
    }
  },
  plugins: [
    // Provide jquery to bootstrap
    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery'
    }),
    // Tell the websocket library that it runs in browser
    new webpack.DefinePlugin({
      'process.env': {
        BROWSER: JSON.stringify(true)
      }
    }),
    // Extract css
    new ExtractTextPlugin({
      filename: '[name]-[contenthash].css'
    }),
    // Clean our distribution folder on every build to only include fresh files
    new CleanWebpackPlugin([outputPath]),
    // Generate an index.html to serve our application
    new HtmlWebpackPlugin({
      // Required settings
      inject: false,
      template: template,
      // Application window title
      title: 'Cluster Broccoli',
      // Create a mount point for our elm app
      appMountId: 'main',
      links: [
        // Link favicons
        {
          href: 'images/favicon-300.png',
          rel: 'apple-touch-icon',
        },
        {
          href: 'images/favicon-300.png',
          rel: 'shortcut icon',
        }
      ]
    })
  ],
  module: {
    rules: [
      // Feed JS through babel for maximum support
      {
        test: /\.js$/,
        use: {
          loader: 'babel-loader',
          options: {
            cacheDirectory: true,
            presets: ['env'],
          }
        }
      },
      // Load our Elm files
      {
        test: /\.elm$/,
        exclude: [/elm-stuff/, /node-modules/],
        use: [
          {
            loader: 'babel-loader',
            options: {
              cacheDirectory: true,
              presets: ['env'],
            }
          },
          { loader: 'elm-webpack-loader' }
        ]
      },
      // Load styles, images, and web-fonts
      {
        test: /\.css$/,
        use: ExtractTextPlugin.extract({
          use: 'css-loader',
          fallback: 'style-loader'
        })
      },
      {
        test: /\.(png|jpg|gif)$/,
        use: [
          {
            options: {
              name: './images/[name].[ext]',
            },
            loader: 'file-loader',
          }
        ]
      },
      {
        test: /\.(woff|woff2|eot|ttf|otf|svg)$/,
        use: [
          'file-loader'
        ]
      }
    ]
  }
}
