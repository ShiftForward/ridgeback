angular.module('ngBoilerplate.dashboard', [
  'ui.router'
]).config(function ($stateProvider) {
  $stateProvider.state('home.dashboard', {
    url: '/dashboard',
    controller: 'DashboardCtrl',
    templateUrl: 'dashboard/dashboard.tpl.html',
    data: {pageTitle: 'Dashboard'}
  });
}).controller('DashboardCtrl', function DashboardController($scope) {
});
