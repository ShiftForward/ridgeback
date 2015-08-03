angular.module( 'ngBoilerplate.projects', [
    'ui.router'
]).config(function config( $stateProvider ) {
    $stateProvider.state( 'home.projects', {
        url: '/projects/:id',
        controller: 'ProjectsCtrl',
        templateUrl: 'projects/projects.tpl.html',
        data:{ pageTitle: 'Projects' }
    });
}).controller( 'ProjectsCtrl', function ProjectsController( $scope ) {
});
