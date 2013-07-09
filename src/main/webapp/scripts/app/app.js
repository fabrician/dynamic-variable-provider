(function() {
'use strict';

angular.module('vp', ['vp.controllers', 'vp.services']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/list', {templateUrl: 'templates/list.html', controller: 'listCtrl'});
    $routeProvider.when('/ruleSet/:name', {templateUrl: 'templates/ruleSet.html', controller: 'ruleSetCtrl'});
    $routeProvider.when('/ruleSet', {templateUrl: 'templates/ruleSet.html', controller: 'ruleSetCtrl'});
    $routeProvider.when('/config', {templateUrl: 'templates/config.html', controller: 'configCtrl'});
    $routeProvider.when('/about', {templateUrl: 'templates/about.html', controller: 'aboutCtrl'});
    $routeProvider.otherwise({redirectTo: '/list'});
  }]);


})();