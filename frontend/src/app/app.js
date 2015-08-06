angular.module('ngBoilerplate', [
  'templates-app',
  'templates-common',
  'ngBoilerplate.home',
  'ngBoilerplate.dashboard',
  'ngBoilerplate.createProject',
  'ngBoilerplate.projects',
  'ui.bootstrap',
  'ui.router',
  'restangular'
])

.constant('config', {
  'baseUrl': 'http://localhost:8080'
})

.config(function myAppConfig ($stateProvider, $urlRouterProvider, $httpProvider, RestangularProvider, config) {
  $urlRouterProvider.otherwise('/dashboard');

  RestangularProvider.setBaseUrl(config.baseUrl);
})

.run(function run () {
})

.controller('AppCtrl', function AppCtrl ($scope, $rootScope) {
  $rootScope.alerts = [];
  $rootScope.closeAlert = function(index) {
    $rootScope.alerts.splice(index, 1);
  };

  $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState, fromParams){
    if (angular.isDefined(toState.data.pageTitle)) {
      $scope.pageTitle = toState.data.pageTitle + ' | Ridgeback' ;
    }
  });
});
