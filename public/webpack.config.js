var path = require("path");

var webpack = require('webpack')

module.exports = {
  entry: {
    app: [
      './src/index.js'
    ]
  },

  output: {
    path: path.resolve(__dirname + '/dist'),
    filename: '[name].js',
  },

  plugins: [
    new webpack.ProvidePlugin({
      $: "jquery",
      jQuery: "jquery"
    }),
    new webpack.DefinePlugin({
      "process.env": {
        BROWSER: JSON.stringify(true)
      }
    })
  ],

  module: {
    loaders: [
      {
        test: /\.(css|scss)$/,
        loaders: [
          'style-loader',
          'css-loader'
        ]
      },
      {
        test:    /\.html$/,
        exclude: /node_modules/,
        loader:  'file-loader?name=[name].[ext]',
      },
      {
        test:    /\.elm$/,
        exclude: [/elm-stuff/, /node_modules/],
        loader:  'elm-webpack-loader',
      },
      {
        test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: 'url-loader?limit=10000&mimetype=application/font-woff',
      },
      {
        test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
        loader: 'file-loader',
      },
      {
        test: /\.(jpg|png)$/,
        loader: 'file-loader',
        options: {
          name: './images/[name].[ext]',
        },
      },
      {
        test: /\.js$/,
        loader: 'babel-loader'
      }
    ],

  },

  devServer: {
    inline: true,
    stats: { colors: true },
  }

};
