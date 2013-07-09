(function(){

'use strict';
/* Controllers */

var myModel = angular.module('vp.controllers', ['ngResource', 'vp.services']);

myModel.controller('listCtrl', ['$scope', 'ruleSetService','$location', '$http',  function($scope, ruleSetService, $location, $http) {
    $scope.list = [];
    $scope.filtered = [];
    $scope.pagination = [];
    $scope.stagingList = [];
    $scope.currentPage = 1;
    $scope.pageSize = 5;
    $scope.sorting = { column: '', ascending : true };  
    $scope.search = "";   
    
    ruleSetService.query( function(data) {
        $scope.list = data;
        $scope.filtered = data;
        $scope.gotoPage($scope.currentPage);
      }
    );
    
    $scope.gotoPage = function(p){
      if ( p <= 0 ) {
        return;
      }
      var len = $scope.filtered.length;
      if ( len == 0 ){
        $scope.currentPage = p;
        $scope.stagingList = [];
        $scope.pagination = [];
        return;
      }
      var start = $scope.pageSize * (p - 1);
      if ( start >= len ){
        return;
      }
      $scope.currentPage = p;
      $scope.stagingList = [];
      for ( var i = 0; i < $scope.pageSize; i++ ){
        if ( i + start >= len ){
          break;
        } else {
          $scope.stagingList[i] = $scope.filtered[i + start];
        }
      }
      $scope.pagination = [];
      var p =  { value : "Prev",
                  page : $scope.currentPage - 1,
                  status : 1 == $scope.currentPage ? "disabled" : ""
                };
      $scope.pagination.push(p);                
      var pages = Math.ceil(len / $scope.pageSize);
      for ( var i = 1; i <= pages; i++ ){
        var p = { value : i.toString(),
                  page : i == $scope.currentPage ? 0 : i,
                  status : i == $scope.currentPage ? "active" : ""
                };
        $scope.pagination.push(p);
      }
      var p =  { value : "Next",
                  page : pages == $scope.currentPage ? 0 : $scope.currentPage + 1,
                  status : pages == $scope.currentPage ? "disabled" : ""
                };
      $scope.pagination.push(p);           
              
    };
    
      
    $scope.remove = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }
      if ( confirm('delete? (' + r.name + ')') ){
          r.id=r.name;
          r.$remove(function(data){
            ruleSetService.query( function(data){
              $scope.list = data;
              $scope.doSearch();
            });
          }, function(data){
            alert('failed' + data)
          });
      }
    };
    
    $scope.edit = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }
      $location.path("/ruleSet/"+r.name);
    };
    $scope.create = function(r){
      $location.path("/ruleSet");
    };
    $scope.copy = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }
      $scope.copyFrom = r.name;
      $scope.copyTo = "";
      $('#copyModal').modal('show');
    };
    $scope.doCopy = function(r){
      if( $scope.copyTo.trim() == "" ){
        return;
      }
      var data = { copyFrom : $scope.copyFrom, copyTo : $scope.copyTo };
      $http.post('management/command/copy',data).success(
                             function(){ ruleSetService.query(
                               function(data) {
                                 $scope.list = data;
                                 $scope.doSearch();
                               }
                             );
                                         $('#copyModal').modal('hide'); 
                             }).error(
                             function(data){ alert(data) } 
                             );
    };
    
    
    $scope.selectRow = function(node){
      var trs = $('tr.listRow');
      if ( $scope.r && $scope.rIndex != undefined ){
        var trNode = $( trs[$scope.rIndex] );
        trNode.removeClass('selected');
        if ( node.$index == $scope.rIndex ){
          //unselect only
          $scope.rIndex = undefined;
          $scope.r = undefined;
          $('button.rowAction').attr('disabled', 'disabled');
          return;
        }  
      }     
      var trNode = $( trs[node.$index] );
      trNode.addClass('selected');
      $scope.r = node.r;
      $scope.rIndex = node.$index;
      $('button.rowAction').removeAttr('disabled');
      
    };
    
    $scope.sortBy = function(col){
      if ($scope.sorting.column == col){
        $scope.sorting.ascending = !($scope.sorting.ascending);
      } else {
        $scope.sorting.ascending = true;
        $scope.sorting.column = col;
      }
      $scope.filtered = $scope.doSorting($scope.filtered);
      $scope.gotoPage($scope.currentPage);
    };
    
    $scope.doSorting = function(list){
      if( $scope.sorting.column == "" ){
        return list;
      }
      var rst = [];
      for ( var i = 0, l = list.length; i < l; i++ ){
        rst[i] = list[i];
      }
      rst.sort( $scope.compareRow );
      return rst;
    };
    $scope.compareRow = function(r1, r2){
      var col = $scope.sorting.column;
      var rst = 0;
      if ( r1[col] > r2[col] ) rst = 1;
      else if (r1[col] == r2[col]) rst = 0;
      else rst = -1;
      return $scope.sorting.ascending ? rst : rst * -1;
    };
    
    $scope.doSearch = function(){ 
      var s = $scope.search.trim();
      if ( s == "" ){
        $scope.filtered = $scope.doSorting($scope.list);
      } else {
        s = s.toLowerCase();
        $scope.filtered = [];
        var src = $scope.list;
        for ( var i = 0, l = src.length; i < l; i++ ){
          var r = src[i];
          if ( r.name.toLowerCase().indexOf(s) > -1 || r.desc.toLowerCase().indexOf(s) > -1 ){
            $scope.filtered.push(r);
          }
        }
        $scope.filtered = $scope.doSorting($scope.filtered);        
      }
      $scope.gotoPage(1);
    };    
  }]);
  
myModel.controller('ruleSetCtrl', ['$scope', 'ruleSetService', '$routeParams', '$location', function($scope, ruleSetService, $routeParams, $location) {
    if ( 'name' in $routeParams ){
      $scope.ruleSet = {}; 
      $scope.pagination = [];
      $scope.stagingList = [];
      $scope.currentPage = 1;
      $scope.pageSize = 5;     
      $scope.sorting = { column: '', ascending : true }; 
      ruleSetService.get({id:$routeParams.name}, 
                     function(data){
                        $scope.ruleSet = data;
                        $scope.ruleSet.rules = $scope.ruleSet.rules || [];
                        $scope.rules = $scope.doSorting($scope.ruleSet.rules);
                        $scope.gotoPage($scope.currentPage);
                     });
      $scope.createNew = false;
    } else {
      $scope.ruleSet = {rules:[]}; 
      $scope.pagination = [];
      $scope.stagingList = [];
      $scope.currentPage = 1;
      $scope.pageSize = 5;     
      $scope.sorting = { column: '', ascending : true }; 
      $scope.createNew = true;
    }
    $scope.search = "";
    

    
    $scope.save = function(){
      if ( $scope.createNew ){
        if ( $scope.ruleSet.name.trim() == "" ){
          return;
        }
        ruleSetService.save({}, $scope.ruleSet, function(data){ $location.path("/list"); }, function(data){ alert('create failed'); });
      } else {
        $scope.ruleSet.id= $scope.ruleSet.name   
        $scope.ruleSet.$save(function(data){
                $location.path("/list");
              },
              function(data){
                alert('failed' + data);
              }  
            );
       } 
    };
    $scope.cancel = function(){
        $location.path("/list"); 
    };
    
    $scope.doGotoPage = function(p, list){
      if ( p <= 0 ) {
        return;
      }
      var len = list.length;
      if ( len == 0 ){
        $scope.stagingList = [];
        $scope.pagination = [];
      }
      var start = $scope.pageSize * (p - 1);
      if ( start >= len ){
        return;
      }
      $scope.currentPage = p;
      $scope.stagingList = [];
      for ( var i = 0; i < $scope.pageSize; i++ ){
        if ( i + start >= len ){
          break;
        } else {
          $scope.stagingList[i] = list[i + start];
        }
      }
      $scope.pagination = [];
      var p =  { value : "Prev",
                  page : $scope.currentPage - 1,
                  status : 1 == $scope.currentPage ? "disabled" : ""
                };
      $scope.pagination.push(p);                
      var pages = Math.ceil(len / $scope.pageSize);
      for ( var i = 1; i <= pages; i++ ){
        var p = { value : i.toString(),
                  page : i == $scope.currentPage ? 0 : i,
                  status : i == $scope.currentPage ? "active" : ""
                };
        $scope.pagination.push(p);
      }
      var p =  { value : "Next",
                  page : pages == $scope.currentPage ? 0 : $scope.currentPage + 1,
                  status : pages == $scope.currentPage ? "disabled" : ""
                };
      $scope.pagination.push(p);           
              
    };
    
    $scope.gotoPage = function(p){
      $scope.doGotoPage(p, $scope.rules);
    };

    $scope.doSearch = function(){ 
      var s = $scope.search.trim();
      if ( s == "" ){
        $scope.rules = $scope.doSorting($scope.ruleSet.rules);
      } else {
        s = s.toLowerCase();
        $scope.rules = [];
        var src = $scope.ruleSet.rules;
        for ( var i = 0, l = src.length; i < l; i++ ){
          var r = src[i];
          if ( r.prim.toLowerCase().indexOf(s) > -1 || r.sec.toLowerCase().indexOf(s) > -1 || r.name.toLowerCase().indexOf(s) > -1 || r.value.toLowerCase().indexOf(s) > -1 ){
            $scope.rules.push(r);
          }
        }
        $scope.rules = $scope.doSorting($scope.rules);        
      }
      $scope.gotoPage(1);
    };
    
    $scope.sortBy = function(col){
      if ($scope.sorting.column == col){
        $scope.sorting.ascending = !($scope.sorting.ascending);
      } else {
        $scope.sorting.ascending = true;
        $scope.sorting.column = col;
      }
      $scope.rules = $scope.doSorting($scope.rules);
      $scope.gotoPage($scope.currentPage);
    };
    
    $scope.doSorting = function(list){
      if( $scope.sorting.column == "" ){
        return list;
      }
      var rst = [];
      for ( var i = 0, l = list.length; i < l; i++ ){
        rst[i] = list[i];
      }
      rst.sort( $scope.compareRow );
      return rst;
    };
    $scope.compareRow = function(r1, r2){
      var col = $scope.sorting.column;
      var rst = 0;
      if ( r1[col] > r2[col] ) rst = 1;
      else if (r1[col] == r2[col]) rst = 0;
      else rst = -1;
      return $scope.sorting.ascending ? rst : rst * -1;
    };
    
    $scope.newRule = function(){
      $scope.existRule = undefined;
      $scope.currRule = {prim: '', sec: '', name: '', value : ''};
      $scope.duplicate_rule = false;
      $scope.form_required_prim = false; 
      $scope.form_required_sec = false;
      $scope.form_required_name = false;
      $scope.form_required_value = false;      
      $('#myModal').modal({show:true});
    }
    
    $scope.editRule = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }
      $scope.existRule = r;
      $scope.currRule = {prim: r.prim, sec: r.sec, name: r.name, value : r.value};
      $scope.duplicate_rule = false;
      $scope.form_required_prim = false; 
      $scope.form_required_sec = false;
      $scope.form_required_name = false;
      $scope.form_required_value = false;      
      $('#myModal').modal({show:true});
    }
    
    $scope.copyRule = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }
      $scope.existRule = undefined;
      $scope.currRule = {prim: r.prim, sec: r.sec, name: r.name, value : r.value};
      $scope.duplicate_rule = false;
      $scope.form_required_prim = false; 
      $scope.form_required_sec = false;
      $scope.form_required_name = false;
      $scope.form_required_value = false;      
      $('#myModal').modal({show:true});
    }

    $scope.ruleFormChange = function(){
      $scope.form_required_prim = false; 
      $scope.form_required_sec = false;
      $scope.form_required_name = false;
      $scope.form_required_value = false;      
      if ( curr.prim.trim() == "" ){
        $scope.form_required_prim = true;
      }
      if ( curr.sec.trim() == "" ){
        $scope.form_required_sec = true;
      }
      if ( curr.name.trim() == "" ){
        $scope.form_required_name = true;
      }
      if ( curr.value.trim() == "" ){
        $scope.form_required_value = true;
      }
    }
    $scope.saveRules = function(){
      var curr = $scope.currRule;
      $scope.duplicate_rule = false;
      $scope.form_required_prim = false; 
      $scope.form_required_sec = false;
      $scope.form_required_name = false;
      $scope.form_required_value = false;      
      if ( curr.prim.trim() == "" ){
        $scope.form_required_prim = true;
      }
      if ( curr.sec.trim() == "" ){
        $scope.form_required_sec = true;
      }
      if ( curr.name.trim() == "" ){
        $scope.form_required_name = true;
      }
      if ( curr.value.trim() == "" ){
        $scope.form_required_value = true;
      }
      if ( $scope.form_required_prim || $scope.form_required_sec || $scope.form_required_name || $scope.form_required_value ){
        return;
      }
      if ($scope.existRule && $scope.existRule.prim == curr.prim && $scope.existRule.sec == curr.sec && $scope.existRule.name == curr.name ){
        $scope.existRule.value = curr.value;
      } else {
        var rules = $scope.ruleSet.rules;
        var idx = -1;
        for (var i = 0, l = rules.length; i < l; i++ ){
          var r1 = rules[i];
          if (r1.prim == curr.prim && r1.sec == curr.sec && r1.name == curr.name){
            var rule = r1;
            var curr = $scope.currRule;
            $scope.duplicate_rule = true;
            return;
          }
          if ($scope.existRule && $scope.existRule.prim == r1.prim && $scope.existRule.sec == r1.sec && $scope.existRule.name == r1.name ){
            idx = i;
          }
        }
        if( idx != -1 ){
          rules.splice(idx, 1 );
        }
        rules.push(curr);
      } 
      $scope.doSearch();
      $('#myModal').modal('hide');
    }
    
    $scope.removeRule = function(r){
      if ( r == undefined && $scope.r ){
        r = $scope.r;
      }      
      if ( confirm('delete rule?') ){
        var rules = $scope.ruleSet.rules;
        for (var i = 0, l = rules.length; i < l; i++ ){
          var r1 = rules[i];
          if (r1.prim == r.prim && r1.sec == r.sec && r1.name == r.name){
            rules.splice(i, 1);
            $scope.doSearch();
            return;
          }
        }
      }        
    }
    
    $scope.selectRow = function(node){
      var trs = $('tr.listRow');
      if ( $scope.r && $scope.rIndex != undefined ){
        var trNode = $( trs[$scope.rIndex] );
        trNode.removeClass('selected');
        if ( node.$index == $scope.rIndex ){
          //unselect only
          $scope.rIndex = undefined;
          $scope.r = undefined;
          $('button.rowAction').attr('disabled', 'disabled');
          return;
        }  
      }     
      var trNode = $( trs[node.$index] );
      trNode.addClass('selected');
      $scope.r = node.rule;
      $scope.rIndex = node.$index;
      $('button.rowAction').removeAttr('disabled');
      
    };    
    
  }]);
  

myModel.controller('configCtrl', ['$scope', 'configService','$location', "$http", function($scope, configService, $location, $http) {
    $scope.config=configService.get('dummy-id');
    
    $scope.submit = function(r){
      $scope.config.id='dummy-id';
      $scope.config.$save( 
          function(data){
            $location.path("/list");
          },
          function(data){
            alert('failed to set database connection' + data);
          }
      );    
    };
    
    $scope.cancel = function(r){
      $location.path("/list");
    };
    
    $scope.destroyCache = function(){
      $http.post('management/command/destroyCache').success(
                             function(){       
                                         $('#copyModal').modal('hide'); 
                                         $location.path("/list");
                             }).error(
                             function(data){ alert(data) } 
                             );
    };
    
  }]);


myModel.controller('aboutCtrl', ['$scope',  function($scope) {

    
  }]);
})();