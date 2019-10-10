module Views.LogUrlSuite exposing (tests)

import Expect as Expect
import Models.Resources.ClientStatus exposing (ClientStatus(..))
import Models.Resources.LogKind exposing (LogKind(..))
import Models.Resources.TaskState exposing (TaskState(..))
import Test exposing (Test, describe, test)
import Views.LogUrl as LogUrl


tests : Test
tests =
    let
        instanceId =
            "id"

        periodicJobId =
            "id/periodic-1234"

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

        kind =
            StdOut
    in
    describe "LogUrl"
        [ test "Should render task log URLs correctly" <|
            \() ->
                LogUrl.taskLog instanceId task kind
                    |> Expect.equal "/downloads/instances/id/allocations/12345/tasks/task/logs/stdout?offset=500KiB"
        , test "Should render periodic task log URLs correctly" <|
            \() ->
                LogUrl.periodicTaskLog instanceId periodicJobId task kind
                    |> Expect.equal "/downloads/instances/id/periodic/id/periodic-1234/allocations/12345/tasks/task/logs/stdout?offset=500KiB"
        ]
