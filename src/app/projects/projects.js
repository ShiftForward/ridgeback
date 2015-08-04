angular.module('ngBoilerplate.projects', [
  'ui.router',
  'restangular',
  'oitozero.ngSweetAlert',
  'datatables',
  'angularMoment'
]).config(function config($stateProvider) {
  $stateProvider.state('home.projects', {
    url: '/projects/:id',
    controller: 'ProjectsCtrl',
    templateUrl: 'projects/projects.tpl.html',
    data: {pageTitle: 'Projects'}
  });
}).controller('ProjectsCtrl', function ProjectsController($scope, $stateParams, Restangular, SweetAlert,
                                                          DTOptionsBuilder, DTColumnBuilder) {

  var regex = /.*(bitbucket\.org|github\.com):([A-Za-z0-9\-_\.]+)\/([A-Za-z0-9\-_\.]+).git.*/g;

  Restangular.one('projects', $stateParams.id).get().then(function (project) {
    $scope.project = project;

    var match = regex.exec($scope.project.gitRepo);
    if (match) {
      var provider = match[1];
      var org = match[2];
      var name = match[3];

      $scope.providerIconClass = "fa fa-" + provider.slice(0, -4);
      $scope.project.providerName =  provider.slice(0, -4);
      $scope.project.url = provider + '/' + org + '/' + name;
    }

  }, function (err) {
    SweetAlert.error('Error', JSON.stringify(err));
  });

  $scope.tests = Restangular.all('tests').getList({ 'projId': $stateParams.id}).$object;

  $scope.durationDiff = function (start, end) {
    var duration = moment.duration(moment(end).diff(moment(start)));
    return duration.asMilliseconds();
  };
});
