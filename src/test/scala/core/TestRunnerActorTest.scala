package core

import org.specs2.mutable._
import org.specs2.time.NoTimeConversions
import akka.actor._
import akka.testkit._
import scala.concurrent.duration.Duration

/* A tiny class that can be used as a Specs2 'context'. */
abstract class AkkaTestkitSpecs2Support extends TestKit(ActorSystem()) with After with ImplicitSender {
  // make sure we shut down the actor system after all tests have run
  def after = system.shutdown()
}

class TestRunnerActorTest extends Specification with NoTimeConversions {
  // sequential

  "A TestRunnerActor" should {
    "execute commands in order" in new AkkaTestkitSpecs2Support {
      val yamlStr =
        """
        before_jobs:
          - true 1
          - true 2

        jobs:
          - name: job1
            source: ignore
            script:
              - true 3
          - name: job2
            source: ignore
            before_script:
              - true 4
            script:
              - true 5
            after_script:
              - true 6

        after_jobs:
          - true 7
        """.stripMargin

      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(yamlStr)

      expectMsg(CommandExecuted("true 1"))
      expectMsg(CommandExecuted("true 2"))
      expectMsg(CommandExecuted("true 3"))
      expectMsg(CommandExecuted("true 4"))
      expectMsg(CommandExecuted("true 5"))
      expectMsg(CommandExecuted("true 6"))
      expectMsg(CommandExecuted("true 7"))
    }

    "fail on empty yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("")

      expectMsg(TestError(BadConfiguration(Seq("Could not parse"))))
    }

    "fail on invalid source" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: bad
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("Unknown source bad in job1"))))
    }

    "fail on invalid format" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: bad
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("job1 format bad doesn't match source output"))))
    }

    "fail on 2 jobs missing required job name" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - script:
              - true
            - script:
               - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq(
        "A job is missing its name",
        "A job is missing its name"))))
    }

    "fail on missing required job source" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              script:
                - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("job1 is missing its source"))))
    }

    "fail on missing jobs" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - true
        """.stripMargin)

      expectMsg(TestError(BadConfiguration(Seq("Test has no jobs"))))
    }

    "fail on garbish yaml" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run("garbishblala")

      expectMsgClass(classOf[TestError])
    }

    "tears down correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          before_jobs:
            - mkdir ttt
          jobs:
            - name: job1
              source: ignore
              script:
                - true
          after_jobs:
            - rmdir ttt
        """.stripMargin)

      expectMsg(CommandExecuted("mkdir ttt"))
      expectMsg(CommandExecuted("true"))
      expectMsg(CommandExecuted("rmdir ttt"))
      expectMsg(Finished)
    }

    "time source works correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: time
              script:
                - sleep 1
                - sleep 1
        """.stripMargin)

      expectMsgClass(classOf[CommandExecuted])
      val msg = expectMsgClass(classOf[MetricOutput])
      msg.m must haveSuperclass[Duration]
    }

    "output source works correctly" in new AkkaTestkitSpecs2Support {
      val actor = system.actorOf(Props(new TestRunnerActor))
      actor ! Run(
        """
          jobs:
            - name: job1
              source: output
              format: seconds
              script:
                - curl -w %{time_total} -o NUL -s http://google.com/
        """.stripMargin)

      expectMsgClass(classOf[CommandExecuted])
      val msg = expectMsgClass(classOf[MetricOutput])
      msg.m must haveSuperclass[Duration]
    }
  }
}
