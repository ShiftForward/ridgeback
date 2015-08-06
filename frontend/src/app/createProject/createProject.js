angular.module('ngBoilerplate.createProject', [
  'ui.router',
  'restangular',
  'oitozero.ngSweetAlert'
]).config(function config($stateProvider) {
  $stateProvider.state('home.createProject', {
    url: '/createProject',
    controller: 'CreateProjectCtrl',
    templateUrl: 'createProject/createProject.tpl.html',
    data: {pageTitle: 'Create Project'}
  });
}).controller('CreateProjectCtrl', function CreateProjectController($scope, $rootScope, $state, SweetAlert, Restangular) {
  var bitbucketRegex = /.*bitbucket\.org\/([A-Za-z0-9\-_\.]+)\/([A-Za-z0-9\-_\.]+).*/g;
  var githubRegex = /.*github\.com\/([A-Za-z0-9\-_\.]+)\/([A-Za-z0-9\-_\.]+).*/g;

  $scope.linkChange = function () {
    var match, org, name = null;
    if (match = bitbucketRegex.exec($scope.link)) {
      org = match[1];
      name = match[2];
      $scope.input.name = name;
      $scope.input.url = "git@bitbucket.org:" + org + "/" + name + ".git";
    } else if (match = githubRegex.exec($scope.link)) {
      org = match[1];
      name = match[2];
      $scope.input.name = name;
      $scope.input.url = "git@github.com:" + org + "/" + name + ".git";
    }
  };

  $scope.input = {
    name: '',
    url: ''
  };

  $scope.creatingProject = false;

  $scope.submitForm = function (isValid) {
    if (isValid) {
      $scope.creatingProject = true;

      Restangular.all('projects').post({name: $scope.input.name, gitRepo: $scope.input.url}).then(function (id) {
        $scope.input.id = id;
        $rootScope.projects.push($scope.input);
        $state.go('home.projects', {id: id});
      }, function (err) {
        SweetAlert.error('Error', JSON.stringify(err));
      }).then(function () {
        $scope.creatingProject = false;
      });

    } else {
      SweetAlert.error("Error", "Form is invalid.");
    }
  };
});
