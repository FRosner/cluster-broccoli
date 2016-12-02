'use strict';

require('font-awesome/css/font-awesome.css');
require('jquery/dist/jquery.min.js');
require('bootstrap/dist/css/bootstrap.min.css');
require('animate.css/animate.css');
require('bootstrap/dist/js/bootstrap.min.js');

// Require index.html so it gets copied to dist
require('./index.html');

var Elm = require('./Main.elm');
var mountNode = document.getElementById('main');

// .embed() can take an optional second argument. This would be an object describing the data we need to start a program, i.e. a userID or some token
var app = Elm.Main.embed(mountNode);
