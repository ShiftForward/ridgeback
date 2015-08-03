angular.module('ngBoilerplate', [
  'templates-app',
  'templates-common',
  'ngBoilerplate.home',
  'ngBoilerplate.dashboard',
  'ngBoilerplate.createProject',
  'ngBoilerplate.projects',
  'ui.router',
  'restangular'
])

.config(function myAppConfig ($stateProvider, $urlRouterProvider, $httpProvider, RestangularProvider) {
  $urlRouterProvider.otherwise('/dashboard');

  RestangularProvider.setBaseUrl('http://localhost:8080');
})

.run(function run () {
})

.controller('AppCtrl', function AppCtrl ($scope, $location) {
  $scope.$on('$stateChangeSuccess', function(event, toState, toParams, fromState, fromParams){
    if (angular.isDefined(toState.data.pageTitle)) {
      $scope.pageTitle = toState.data.pageTitle + ' | Ridgeback' ;
    }
  });
});
