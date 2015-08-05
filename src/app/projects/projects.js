angular.module('ngBoilerplate.projects', [
  'ui.router',
  'restangular',
  'oitozero.ngSweetAlert',
  'datatables',
  'angularMoment',
  'pusher-angular',
  'luegg.directives'
]).config(function config($stateProvider) {
  $stateProvider.state('home.projects', {
    url: '/projects/:id',
    controller: 'ProjectsCtrl',
    templateUrl: 'projects/projects.tpl.html',
    data: {pageTitle: 'Projects'}
  });
}).controller('ProjectsCtrl', function ProjectsController($scope, $stateParams, Restangular, SweetAlert,
                                                          $pusher) {

  var regex = /.*(bitbucket\.org|github\.com):([A-Za-z0-9\-_\.]+)\/([A-Za-z0-9\-_\.]+).git.*/g;

  $scope.currentBuild = '';

  Restangular.one('projects', $stateParams.id).get()
    .then(function (project) {
      $scope.project = project;

      var match = regex.exec($scope.project.gitRepo);
      if (match) {
        var provider = match[1];
        var org = match[2];
        var name = match[3];

        $scope.providerIconClass = "fa fa-" + provider.slice(0, -4);
        $scope.project.providerName = provider.slice(0, -4);
        $scope.project.url = provider + '/' + org + '/' + name;
      }

      return Restangular.all('tests').getList({'projId': $stateParams.id});
    })
    .then(function (tests) {
      $scope.tests = tests;

      if ($scope.tests.length !== 0) {
        return Restangular.one('tests', $scope.tests[$scope.tests.length - 1].id).all('pastEvents').getList();
      } else {
        return null;
      }
    })
    .then(function (pastEvents) {
      if (pastEvents) {
        pastEvents.forEach(function (pastEvent) {
          $scope.currentBuild += pastEvent[0] + ": " + pastEvent[1] + "\n";
        });
      }

      if ($scope.tests.length !== 0) {
        var API_KEY = 'cd120e0fb030ba0e217b';
        var client = new Pusher(API_KEY);
        var pusher = $pusher(client);

        var channel = pusher.subscribe($scope.project.name + "-" + $scope.tests[$scope.tests.length - 1].id);

        $scope.currentBuild += "Bound to channel " + channel.name + ".\n";

        channel.bind_all(function (event, data) {
          $scope.currentBuild += event + ": " + JSON.stringify(data) + "\n";
        });
      }
    }, function (err) {
      SweetAlert.error('Error', JSON.stringify(err));
    });

  $scope.durationDiff = function (start, end) {
    var duration = moment.duration(moment(end).diff(moment(start)));
    return duration.asMilliseconds();
  };
});
