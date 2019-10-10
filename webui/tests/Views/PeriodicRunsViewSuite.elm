module Views.PeriodicRunsViewSuite exposing (tests)

import Dict
import Expect as Expect
import Models.Resources.ClientStatus exposing (ClientStatus(..))
import Models.Resources.JobStatus exposing (JobStatus(..))
import Models.Resources.TaskState exposing (TaskState(..))
import Test exposing (Test, describe, test)
import Test.Html.Query as Query
import Test.Html.Selector as Selector
import Views.PeriodicRunsView as PeriodicRunsView


tests : Test
tests =
    let
        instanceId =
            "id"

        task =
            { taskName = "task"
            , taskState = TaskRunning
            , allocationId = "12345"
            , clientStatus = ClientComplete
            , resources =
                { cpuRequiredMhz = Nothing
                , cpuUsedMhz = Nothing
                , memoryRequiredBytes = Nothing
                , memoryUsedBytes = Nothing
                }
            }

        allocatedPeriodicTasks =
            [ ( String.concat [ instanceId, "/periodic-1" ], [ task ] )
            , ( String.concat [ instanceId, "/periodic-2" ], [ task ] )
            ]
                |> Dict.fromList

        instanceTasks =
            Just <|
                { instanceId = instanceId
                , allocatedTasks = []
                , allocatedPeriodicTasks = allocatedPeriodicTasks
                }

        periodicRuns =
            [ { status = JobRunning
              , utcSeconds = 1519057596
              , jobName = String.concat <| [ instanceId, "/periodic-1" ]
              }
            , { status = JobStopped
              , utcSeconds = 1519057600
              , jobName = String.concat <| [ instanceId, "/periodic-2" ]
              }
            ]
    in
    describe "PeriodicRunsView"
        [ test "Should render each periodic run" <|
            \() ->
                PeriodicRunsView.view instanceId instanceTasks periodicRuns
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "periodic-run-time" ]
                    |> Query.count (Expect.equal (List.length periodicRuns))
        , test "Should render each allocated periodic task" <|
            \() ->
                PeriodicRunsView.view instanceId instanceTasks periodicRuns
                    |> Query.fromHtml
                    |> Query.findAll [ Selector.class "periodic-run-allocation-id" ]
                    |> Query.count (Expect.equal (Dict.size allocatedPeriodicTasks))
        ]
