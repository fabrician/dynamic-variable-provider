(function(){

  'use strict';

  var myModule = angular.module('vp.services', ['ngResource']);

  myModule.factory('ruleSetService', 
                  ['$resource', function($resource) {
                                  return $resource('management/providers/:id', {id:'@id'});
                                }]);  
  myModule.factory('configService', 
                  ['$resource', function($resource) {
                                  return $resource('management/config/:id', {id:'@id'});
                                }]);  
})();