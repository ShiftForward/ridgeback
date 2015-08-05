/**
 * Each section of the site has its own module. It probably also has
 * submodules, though this boilerplate is too simple to demonstrate it. Within
 * `src/app/home`, however, could exist several additional folders representing
 * additional modules that would then be listed as dependencies of this one.
 * For example, a `note` section could have the submodules `note.create`,
 * `note.delete`, `note.edit`, etc.
 *
 * Regardless, so long as dependencies are managed correctly, the build process
 * will automatically take take of the rest.
 *
 * The dependencies block here is also where component dependencies should be
 * specified, as shown below.
 */
angular.module( 'ngBoilerplate.home', [
  'ui.router',
  'restangular',
  'oitozero.ngSweetAlert'
])

/**
 * Each section or module of the site can also have its own routes. AngularJS
 * will handle ensuring they are all available at run-time, but splitting it
 * this way makes each module more "self-contained".
 */
.config(function config( $stateProvider ) {
  $stateProvider.state('home', {
    url: '/home',
    templateUrl: 'home/home.tpl.html',
    // abstract: true,
    data: {pageTitle: 'Home'}
  });
})

/**
 * And of course we define a controller for our route.
 */
.controller( 'HomeCtrl', function HomeController($scope, $rootScope, Restangular, SweetAlert) {
  $scope.loadingProjects = true;
  $rootScope.projects = [];

  Restangular.all('projects').getList().then(function (projects) {
    $scope.loadingProjects = false;
    $rootScope.projects = projects;
  }, function (err) {
    SweetAlert.error('Error', JSON.stringify(err));
  });
});
