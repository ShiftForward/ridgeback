angular.module('ngBoilerplate.dashboard', [
  'ui.router',
  'oitozero.ngSweetAlert',
  'restangular'
]).config(function ($stateProvider) {
  $stateProvider.state('home.dashboard', {
    url: '/dashboard',
    controller: 'DashboardCtrl',
    templateUrl: 'dashboard/dashboard.tpl.html',
    data: {pageTitle: 'Dashboard'}
  });
}).controller('DashboardCtrl', function DashboardController($scope, $rootScope, Restangular, SweetAlert) {

  var regex = /.*(bitbucket\.org|github\.com):([a-z0-9\-_\.]+)\/([a-z0-9\-_\.]+).git.*/i;

  Restangular.all('projects').getList().then(function (projects) {
    $scope.projects = projects;
    projects.forEach(function (project) {

      var match = regex.exec(project.gitRepo);
      console.log(project.gitRepo);
      if (match) {
        console.log(match);
        var provider = match[1];
        var org = match[2];
        var name = match[3];

        project.org = match[2];
        project.repoName = match[3];
        project.url = provider + '/' + org + '/' + name;
      }

      project.buildRunning = false;

      Restangular.all('tests').getList({'projId': project.id}).then(function (tests) {
        project.testsCount = tests.length;
        if (tests.length === 0) {
          return null;
        } else {
          project.lastTest = tests[tests.length - 1];
          return Restangular.one('tests', project.lastTest.id).all('events').getList();
        }
      }).then(function (events) {
        if (events && events.length !== 0) {
          project.buildRunning = true;
          events.forEach(function (event) {
            if (event[0] == "Finished") {
              $scope.buildRunning = false;
            }
          });
        }
      }, function (err) {
        SweetAlert.error('Error', JSON.stringify(err));
        $scope.testsTabDisabled = false;
        $scope.buildTabDisabled = false;
      });

      Restangular.all('jobs').getList({'projId': project.id}).then(function (jobs) {
        project.jobsCount = jobs.length;
        // TODO: do something else with jobs
      }, function (err) {
        SweetAlert.error('Error', JSON.stringify(err));
      });
    });
  }, function (err) {
    SweetAlert.error('Error', JSON.stringify(err));
  });

  $scope.durationDiff = function (start, end) {
    var duration = moment.duration(moment(end).diff(moment(start)));
    return duration.asMilliseconds();
  };
});
